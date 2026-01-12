package org.mirage.gfbs.fams.central;

import dan200.computercraft.api.lua.LuaException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

public final class FamsTypes {
    private FamsTypes(){}

    // 系统运行模式
    public enum SystemMode {
        /** 休眠模式：不主动决策，仅监测与必要的自动升档 */
        SLEEP,
        /** 部分节点响应自动处理模式：仅对标记为自动的节点下发动作 */
        PARTIAL_AUTO,
        /** 正式处理模式：全量闭环控制/学习 */
        FORMAL,
        /** 紧急模式：以安全为最高优先级，强制降级/联锁，学习默认关闭 */
        EMERGENCY
    }

    // 模式参数
    public static final class ModeConfig {
        public boolean learningEnabled = true;
        public double learningRate = 0.01;

        /** 风险硬联锁阈值：risk >= riskHardLimit 时强制清零动作并夹紧 */
        public double riskHardLimit = 0.85;

        /** 自动升级到紧急模式的触发阈值（建议 >= riskHardLimit） */
        public double emergencyTriggerRisk = 0.95;

        /** 外部循环监测间隔（仅在休眠模式启用外部监测时使用） */
        public long monitorIntervalMs = 250;

        /** 休眠模式是否启用外部自动循环监测系统 */
        public boolean enableExternalMonitor = true;

        /** 是否允许下发动作（休眠模式默认 false） */
        public boolean allowActionDispatch = true;

        public ModeConfig copy(){
            ModeConfig c=new ModeConfig();
            c.learningEnabled=this.learningEnabled;
            c.learningRate=this.learningRate;
            c.riskHardLimit=this.riskHardLimit;
            c.emergencyTriggerRisk=this.emergencyTriggerRisk;
            c.monitorIntervalMs=this.monitorIntervalMs;
            c.enableExternalMonitor=this.enableExternalMonitor;
            c.allowActionDispatch=this.allowActionDispatch;
            return c;
        }
    }

    // 设施状态向量(N维),与传感器/子系统摘要对齐
    public static final class State {
        public final double[] x;          // 状态向量
        public final double[] obsWeight;  // 观测置信权重(同维),用于抑制坏数据
        public long t;                    // 时间戳/步号
        public State(int n){ x=new double[n]; obsWeight=new double[n]; for(int i=0;i<n;i++) obsWeight[i]=1.0; }
    }

    // 控制动作向量(M维)，与设施执行器/命令通道对齐
    public static final class Action {
        public final double[] u;
        public Action(int m){ u=new double[m]; }
    }

    // 风险与约束："安全联锁/策略"
    public static final class Safety {
        public double risk;          // 0..1
        public double riskHardLimit; // 硬限制
        public double[] uMin, uMax;  // 动作上下限
        public double[] xSoft;       // 状态软阈值（超过会提高风险）
        public double[] xHard;       // 状态硬阈值（超过直接联锁/强制降级）
        public Safety(int n,int m){
            uMin=new double[m]; uMax=new double[m];
            xSoft=new double[n]; xHard=new double[n];
            riskHardLimit=0.85;
            for(int i=0;i<m;i++){ uMin[i]=-1; uMax[i]=1; }
            for(int i=0;i<n;i++){ xSoft[i]=1e9; xHard[i]=1e9; }
        }

        public void setFromMap(Map<String, Object> config) throws LuaException {
            if (config == null) return;

            // 设置风险硬限制
            if (config.containsKey("riskHardLimit")) {
                Object value = config.get("riskHardLimit");
                if (value instanceof Number) {
                    double limit = ((Number) value).doubleValue();
                    if (limit >= 0 && limit <= 1.0) {
                        this.riskHardLimit = limit;
                    } else {
                        throw new LuaException("riskHardLimit must be between 0 and 1.0");
                    }
                } else {
                    throw new LuaException("riskHardLimit must be a number");
                }
            }

            // 设置紧急触发风险阈值
            if (config.containsKey("emergencyTriggerRisk")) {
                Object value = config.get("emergencyTriggerRisk");
                if (value instanceof Number) {
                    double trigger = ((Number) value).doubleValue();
                    if (trigger >= 0 && trigger <= 1.0) {
                        if (trigger < riskHardLimit) {
                            throw new LuaException("emergencyTriggerRisk should be >= riskHardLimit for safety");
                        }
                    } else {
                        throw new LuaException("emergencyTriggerRisk must be between 0 and 1.0");
                    }
                } else {
                    throw new LuaException("emergencyTriggerRisk must be a number");
                }
            }

            // 设置动作下限 uMin
            if (config.containsKey("uMin")) {
                Object uMinObj = config.get("uMin");
                double[] uMinArray = parseDoubleArray(uMinObj, uMin.length, "uMin");
                System.arraycopy(uMinArray, 0, uMin, 0, Math.min(uMinArray.length, uMin.length));

                // 验证 uMin <= uMax
                validateActionLimits();
            }

            // 设置动作上限 uMax
            if (config.containsKey("uMax")) {
                Object uMaxObj = config.get("uMax");
                double[] uMaxArray = parseDoubleArray(uMaxObj, uMax.length, "uMax");
                System.arraycopy(uMaxArray, 0, uMax, 0, Math.min(uMaxArray.length, uMax.length));

                // 验证 uMin <= uMax
                validateActionLimits();
            }

            // 设置状态软阈值 xSoft
            if (config.containsKey("xSoft")) {
                Object xSoftObj = config.get("xSoft");
                double[] xSoftArray = parseDoubleArray(xSoftObj, xSoft.length, "xSoft");
                System.arraycopy(xSoftArray, 0, xSoft, 0, Math.min(xSoftArray.length, xSoft.length));

                // 验证 xSoft <= xHard
                validateStateThresholds();
            }

            // 设置状态硬阈值 xHard
            if (config.containsKey("xHard")) {
                Object xHardObj = config.get("xHard");
                double[] xHardArray = parseDoubleArray(xHardObj, xHard.length, "xHard");
                System.arraycopy(xHardArray, 0, xHard, 0, Math.min(xHardArray.length, xHard.length));

                // 验证 xSoft <= xHard
                validateStateThresholds();
            }

            if (config.containsKey("uMinAt")) {
                setArrayValuesByIndex(config.get("uMinAt"), uMin, "uMinAt");
                validateActionLimits();
            }

            if (config.containsKey("uMaxAt")) {
                setArrayValuesByIndex(config.get("uMaxAt"), uMax, "uMaxAt");
                validateActionLimits();
            }

            if (config.containsKey("xSoftAt")) {
                setArrayValuesByIndex(config.get("xSoftAt"), xSoft, "xSoftAt");
                validateStateThresholds();
            }

            if (config.containsKey("xHardAt")) {
                setArrayValuesByIndex(config.get("xHardAt"), xHard, "xHardAt");
                validateStateThresholds();
            }

            if (config.containsKey("actionLimits")) {
                Object limitsObj = config.get("actionLimits");
                if (limitsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> limitsMap = (Map<String, Object>) limitsObj;
                    setActionLimitsFromMap(limitsMap);
                }
            }

            if (config.containsKey("stateThresholds")) {
                Object thresholdsObj = config.get("stateThresholds");
                if (thresholdsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> thresholdsMap = (Map<String, Object>) thresholdsObj;
                    setStateThresholdsFromMap(thresholdsMap);
                }
            }
        }

        private double[] parseDoubleArray(Object obj, int expectedLength, String fieldName) throws LuaException {
            if (obj instanceof Number) {
                double[] result = new double[expectedLength];
                double value = ((Number) obj).doubleValue();
                Arrays.fill(result, value);
                return result;
            } else if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                double[] result = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Number) {
                        result[i] = ((Number) list.get(i)).doubleValue();
                    } else {
                        throw new LuaException(fieldName + " array must contain numbers at index " + i);
                    }
                }
                return result;
            } else if (obj instanceof Object[]) {
                Object[] array = (Object[]) obj;
                double[] result = new double[array.length];
                for (int i = 0; i < array.length; i++) {
                    if (array[i] instanceof Number) {
                        result[i] = ((Number) array[i]).doubleValue();
                    } else {
                        throw new LuaException(fieldName + " array must contain numbers at index " + i);
                    }
                }
                return result;
            } else {
                throw new LuaException(fieldName + " must be a number or array of numbers");
            }
        }

        private void setArrayValuesByIndex(Object indexMapObj, double[] array, String fieldName) throws LuaException {
            if (indexMapObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> indexMap = (Map<String, Object>) indexMapObj;
                for (Map.Entry<String, Object> entry : indexMap.entrySet()) {
                    try {
                        int index = Integer.parseInt(entry.getKey());
                        if (index >= 0 && index < array.length) {
                            if (entry.getValue() instanceof Number) {
                                array[index] = ((Number) entry.getValue()).doubleValue();
                            } else {
                                throw new LuaException(fieldName + " value must be a number for index " + index);
                            }
                        } else {
                            throw new LuaException(fieldName + " index " + index + " out of range [0, " + (array.length - 1) + "]");
                        }
                    } catch (NumberFormatException e) {
                        throw new LuaException(fieldName + " key must be an integer index");
                    }
                }
            } else {
                throw new LuaException(fieldName + " must be a map of index-value pairs");
            }
        }

        private void validateActionLimits() throws LuaException {
            for (int i = 0; i < uMin.length && i < uMax.length; i++) {
                if (uMin[i] > uMax[i]) {
                    throw new LuaException("uMin[" + i + "] = " + uMin[i] + " cannot be greater than uMax[" + i + "] = " + uMax[i]);
                }
            }
        }

        private void validateStateThresholds() throws LuaException {
            for (int i = 0; i < xSoft.length && i < xHard.length; i++) {
                if (xSoft[i] > xHard[i]) {
                    throw new LuaException("xSoft[" + i + "] = " + xSoft[i] + " cannot be greater than xHard[" + i + "] = " + xHard[i]);
                }
                if (xHard[i] <= 0) {
                    throw new LuaException("xHard[" + i + "] must be positive");
                }
            }
        }

        private void setActionLimitsFromMap(Map<String, Object> limitsMap) throws LuaException {
            if (limitsMap.containsKey("min")) {
                Object minObj = limitsMap.get("min");
                double[] minArray = parseDoubleArray(minObj, uMin.length, "actionLimits.min");
                System.arraycopy(minArray, 0, uMin, 0, Math.min(minArray.length, uMin.length));
            }

            if (limitsMap.containsKey("max")) {
                Object maxObj = limitsMap.get("max");
                double[] maxArray = parseDoubleArray(maxObj, uMax.length, "actionLimits.max");
                System.arraycopy(maxArray, 0, uMax, 0, Math.min(maxArray.length, uMax.length));
            }

            if (limitsMap.containsKey("byDimension")) {
                Object byDimObj = limitsMap.get("byDimension");
                if (byDimObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> byDimMap = (Map<String, Object>) byDimObj;
                    for (Map.Entry<String, Object> entry : byDimMap.entrySet()) {
                        try {
                            int dim = Integer.parseInt(entry.getKey());
                            if (dim >= 0 && dim < uMin.length && dim < uMax.length) {
                                if (entry.getValue() instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> dimLimits = (Map<String, Object>) entry.getValue();
                                    if (dimLimits.containsKey("min") && dimLimits.get("min") instanceof Number) {
                                        uMin[dim] = ((Number) dimLimits.get("min")).doubleValue();
                                    }
                                    if (dimLimits.containsKey("max") && dimLimits.get("max") instanceof Number) {
                                        uMax[dim] = ((Number) dimLimits.get("max")).doubleValue();
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }

            validateActionLimits();
        }

        private void setStateThresholdsFromMap(Map<String, Object> thresholdsMap) throws LuaException {
            if (thresholdsMap.containsKey("soft")) {
                Object softObj = thresholdsMap.get("soft");
                double[] softArray = parseDoubleArray(softObj, xSoft.length, "stateThresholds.soft");
                System.arraycopy(softArray, 0, xSoft, 0, Math.min(softArray.length, xSoft.length));
            }

            if (thresholdsMap.containsKey("hard")) {
                Object hardObj = thresholdsMap.get("hard");
                double[] hardArray = parseDoubleArray(hardObj, xHard.length, "stateThresholds.hard");
                System.arraycopy(hardArray, 0, xHard, 0, Math.min(hardArray.length, xHard.length));
            }

            if (thresholdsMap.containsKey("byDimension")) {
                Object byDimObj = thresholdsMap.get("byDimension");
                if (byDimObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> byDimMap = (Map<String, Object>) byDimObj;
                    for (Map.Entry<String, Object> entry : byDimMap.entrySet()) {
                        try {
                            int dim = Integer.parseInt(entry.getKey());
                            if (dim >= 0 && dim < xSoft.length && dim < xHard.length) {
                                if (entry.getValue() instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> dimThresholds = (Map<String, Object>) entry.getValue();
                                    if (dimThresholds.containsKey("soft") && dimThresholds.get("soft") instanceof Number) {
                                        xSoft[dim] = ((Number) dimThresholds.get("soft")).doubleValue();
                                    }
                                    if (dimThresholds.containsKey("hard") && dimThresholds.get("hard") instanceof Number) {
                                        xHard[dim] = ((Number) dimThresholds.get("hard")).doubleValue();
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }

            validateStateThresholds();
        }
    }

    // 控制目标 : 期望状态/输出
    public static final class Goal {
        public final double[] targetX;     // 期望状态
        public final double[] weight;      // 每维权重
        public Goal(int n){ targetX=new double[n]; weight=new double[n]; for(int i=0;i<n;i++) weight[i]=1.0; }

        public void setFromMap(Map<String, Object> config) throws LuaException {
            if (config == null) return;
            if (config.containsKey("target")) {
                Object targetObj = config.get("target");
                double[] targetArray = parseDoubleArray(targetObj, targetX.length, "target");
                System.arraycopy(targetArray, 0, targetX, 0, Math.min(targetArray.length, targetX.length));
            }
            if (config.containsKey("weights")) {
                Object weightsObj = config.get("weights");
                double[] weightsArray = parseDoubleArray(weightsObj, weight.length, "weights");
                System.arraycopy(weightsArray, 0, weight, 0, Math.min(weightsArray.length, weight.length));
            }
            if (config.containsKey("targetAt")) {
                Object targetAtObj = config.get("targetAt");
                if (targetAtObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> targetAtMap = (Map<String, Object>) targetAtObj;
                    for (Map.Entry<String, Object> entry : targetAtMap.entrySet()) {
                        try {
                            int index = Integer.parseInt(entry.getKey());
                            if (index >= 0 && index < targetX.length && entry.getValue() instanceof Number) {
                                targetX[index] = ((Number) entry.getValue()).doubleValue();
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
            if (config.containsKey("weightAt")) {
                Object weightAtObj = config.get("weightAt");
                if (weightAtObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> weightAtMap = (Map<String, Object>) weightAtObj;
                    for (Map.Entry<String, Object> entry : weightAtMap.entrySet()) {
                        try {
                            int index = Integer.parseInt(entry.getKey());
                            if (index >= 0 && index < weight.length && entry.getValue() instanceof Number) {
                                double w = ((Number) entry.getValue()).doubleValue();
                                weight[index] = Math.max(0, w);
                            }
                        } catch (NumberFormatException e) {
                        }
                    }
                }
            }
        }

        private double[] parseDoubleArray(Object obj, int expectedLength, String fieldName) throws LuaException {
            if (obj instanceof Number) {
                double[] result = new double[expectedLength];
                double value = ((Number) obj).doubleValue();
                Arrays.fill(result, value);
                return result;
            } else if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                double[] result = new double[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) instanceof Number) {
                        result[i] = ((Number) list.get(i)).doubleValue();
                    } else {
                        throw new LuaException(fieldName + " array must contain numbers at index " + i);
                    }
                }
                return result;
            } else if (obj instanceof Object[]) {
                Object[] array = (Object[]) obj;
                double[] result = new double[array.length];
                for (int i = 0; i < array.length; i++) {
                    if (array[i] instanceof Number) {
                        result[i] = ((Number) array[i]).doubleValue();
                    } else {
                        throw new LuaException(fieldName + " array must contain numbers at index " + i);
                    }
                }
                return result;
            } else {
                throw new LuaException(fieldName + " must be a number or array of numbers");
            }
        }
    }

    // 子系统节点
    public static final class Node {
        public final String id;
        public final double[] in;     // 子系统上报摘要
        public final double[] out;    // 分配给子系统的动作片段
        public Node(String id,int inDim,int outDim){
            this.id=id; in=new double[inDim]; out=new double[outDim];
        }
    }
}
