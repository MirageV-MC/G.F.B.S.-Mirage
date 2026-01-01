package org.mirage.ccio.app.famsApi;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.mirage.ccio.api.CCIoApiRegistry;
import org.mirage.fams.central.FamsCentral;
import org.mirage.fams.central.FamsMemory;
import org.mirage.fams.central.FamsTypes;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Registers FAMS APIs into {@link CCIoApiRegistry}.
 *
 * <p>Lua usage example (central computer):
 * <pre>
 *   local ccio = peripheral.wrap("right")
 *   ccio.invokeApi("fams.setCentral", { stateDim=16, actionDim=16, maxShells=64, maxNodes=64, tickIntervalMs=50 })
 * </pre>
 *
 * <p>Lua usage example (node computer):
 * <pre>
 *   local ccio = peripheral.wrap("right")
 *   ccio.invokeApi("fams.connectCentral") -- or pass centralId
 *   ccio.invokeApi("fams.registerNode", { id="DMR_CONTROL", inDim=8, outDim=8 })
 *   ccio.invokeApi("fams.pushIn", {0.2, 0.7, 0.0})
 *   local out = ccio.invokeApi("fams.pullOut")
 * </pre>
 */
public final class FamsApis {

    private FamsApis(){}

    public static void register(){
        // 中央管理
        CCIoApiRegistry.register("fams.setCentral", FamsApis::setCentral);
        CCIoApiRegistry.register("fams.clearCentral", FamsApis::clearCentral);

        // 连接
        CCIoApiRegistry.register("fams.connectCentral", FamsApis::connectCentral);
        CCIoApiRegistry.register("fams.disconnect", FamsApis::disconnect);

        // 节点管理
        CCIoApiRegistry.register("fams.registerNode", FamsApis::registerNode);
        CCIoApiRegistry.register("fams.setNodeAutoEnabled", FamsApis::setNodeAutoEnabled);

        // 数据 I/O
        CCIoApiRegistry.register("fams.pushIn", FamsApis::pushIn);
        CCIoApiRegistry.register("fams.pullOut", FamsApis::pullOut);

        // 步骤/统计数据
        CCIoApiRegistry.register("fams.step", FamsApis::step);
        CCIoApiRegistry.register("fams.setMode", FamsApis::setMode);
        CCIoApiRegistry.register("fams.getMode", FamsApis::getMode);
        CCIoApiRegistry.register("fams.stats", FamsApis::stats);

        // 目标管理
        CCIoApiRegistry.register("fams.setGoal", FamsApis::setGoal);
        CCIoApiRegistry.register("fams.getGoal", FamsApis::getGoal);

        // 安全参数配置
        CCIoApiRegistry.register("fams.setSafetyLimits", FamsApis::setSafetyLimits);
        CCIoApiRegistry.register("fams.getSafetyLimits", FamsApis::getSafetyLimits);

        // 节点信息查询
        CCIoApiRegistry.register("fams.getNodeInfo", FamsApis::getNodeInfo);
        CCIoApiRegistry.register("fams.listNodes", FamsApis::listNodes);

        // 模式参数配置
        CCIoApiRegistry.register("fams.setModeConfig", FamsApis::setModeConfig);
        CCIoApiRegistry.register("fams.getModeConfig", FamsApis::getModeConfig);

        // 记忆系统管理
        CCIoApiRegistry.register("fams.memoryStats", FamsApis::memoryStats);
        CCIoApiRegistry.register("fams.memoryConfig", FamsApis::memoryConfig);
        CCIoApiRegistry.register("fams.setMemoryConfig", FamsApis::setMemoryConfig);

        // 学习参数控制
        CCIoApiRegistry.register("fams.setLearningRate", FamsApis::setLearningRate);
        CCIoApiRegistry.register("fams.setLearningEnabled", FamsApis::setLearningEnabled);

        // 实时状态监控
        CCIoApiRegistry.register("fams.getState", FamsApis::getState);
        CCIoApiRegistry.register("fams.getRisk", FamsApis::getRisk);
        CCIoApiRegistry.register("fams.getLastLoss", FamsApis::getLastLoss);

        // 系统控制
        CCIoApiRegistry.register("fams.shutdown", FamsApis::shutdown);
        CCIoApiRegistry.register("fams.save", FamsApis::save);
        CCIoApiRegistry.register("fams.load", FamsApis::load);
    }

    private static Object setCentral(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        Map<String,Object> cfg = argsToMap(args, 0, "setCentral expects: (configTable)");
        FamsRuntimeManager.get(level).setCentral(cid, cfg);
        return true;
    }

    private static Object clearCentral(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        FamsRuntimeManager.get(level).clearCentral(cid);
        return true;
    }

    private static Object connectCentral(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        Integer centralId = null;
        if(args != null && args.length >= 1 && args[0] != null){
            if(args[0] instanceof Number n) centralId = n.intValue();
            else throw new LuaException("connectCentral expects optional centralId number");
        }
        FamsRuntimeManager.get(level).connect(cid, centralId);
        return true;
    }

    private static Object disconnect(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        FamsRuntimeManager.get(level).disconnect(cid);
        return true;
    }

    private static Object registerNode(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        Map<String,Object> spec = argsToMap(args, 0, "registerNode expects: (specTable)");
        FamsRuntime.NodeBinding b = FamsRuntimeManager.get(level).registerNode(cid, spec);
        return Map.of(
                "computerId", b.computerId,
                "id", b.nodeId,
                "inDim", b.node.in.length,
                "outDim", b.node.out.length
        );
    }

    private static Object setNodeAutoEnabled(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        if(args==null || args.length<2) throw new LuaException("setNodeAutoEnabled expects: (nodeId, enabled)");
        if(!(args[0] instanceof String nodeId)) throw new LuaException("setNodeAutoEnabled expects nodeId string");
        boolean enabled;
        Object v = args[1];
        if(v instanceof Boolean b) enabled = b;
        else if(v instanceof Number n) enabled = n.intValue()!=0;
        else throw new LuaException("setNodeAutoEnabled expects enabled boolean/number");
        FamsRuntimeManager.get(level).setNodeAutoEnabled(nodeId, enabled);
        return true;
    }

    private static Object pushIn(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        if(args==null || args.length<1) throw new LuaException("pushIn expects: (arrayOfNumbers)");
        FamsRuntimeManager.get(level).pushIn(cid, args[0]);
        return true;
    }

    private static Object pullOut(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        double[] out = FamsRuntimeManager.get(level).pullOut(cid);
        // Convert to Lua list
        Object[] arr = new Object[out.length];
        for(int i=0;i<out.length;i++) arr[i] = out[i];
        return arr;
    }

    private static Object step(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        return FamsRuntimeManager.get(level).step(cid);
    }

    private static Object setMode(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        int cid = FamsRuntimeManager.computerId(computer);
        if(args==null || args.length<1) throw new LuaException("setMode expects: (modeName)");
        if(!(args[0] instanceof String s)) throw new LuaException("setMode expects modeName string");
        return FamsRuntimeManager.get(level).setMode(cid, s);
    }

    private static Object getMode(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        return FamsRuntimeManager.get(level).getMode();
    }

    private static Object stats(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException{
        return FamsRuntimeManager.get(level).stats();
    }

    @SuppressWarnings("unchecked")
    private static Map<String,Object> argsToMap(Object[] args, int index, String err) throws LuaException{
        if(args==null || args.length<=index) throw new LuaException(err);
        Object o = args[index];
        if(o instanceof Map<?,?> map){
            Map<String,Object> out = new HashMap<>();
            for(Map.Entry<?,?> e : map.entrySet()){
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
            return out;
        }
        throw new LuaException(err);
    }

    private static Object setGoal(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can set goal");
        }
        Map<String,Object> goalConfig = argsToMap(args, 0, "setGoal expects: (goalTable)");
        FamsRuntimeManager.get(level).requireCentral().goal().setFromMap(goalConfig);
        return true;
    }

    private static Object getGoal(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can get goal");
        }
        FamsTypes.Goal goal = FamsRuntimeManager.get(level).requireCentral().goal();
        return Map.of(
                "target", goal.targetX,
                "weights", goal.weight
        );
    }

    private static Object setSafetyLimits(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can set safety limits");
        }
        Map<String,Object> safetyConfig = argsToMap(args, 0, "setSafetyLimits expects: (safetyTable)");
        FamsRuntimeManager.get(level).requireCentral().safety().setFromMap(safetyConfig);
        return true;
    }

    private static Object getSafetyLimits(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can get safety limits");
        }
        FamsTypes.Safety safety = FamsRuntimeManager.get(level).requireCentral().safety();
        return Map.of(
                "risk", safety.risk,
                "riskHardLimit", safety.riskHardLimit,
                "uMin", safety.uMin,
                "uMax", safety.uMax
        );
    }

    private static Object getNodeInfo(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        if(args == null || args.length < 1 || !(args[0] instanceof String)) {
            throw new LuaException("getNodeInfo expects: (nodeId)");
        }
        String nodeId = (String) args[0];
        FamsRuntime.NodeBinding binding = FamsRuntimeManager.get(level).requireBindingByNodeId(nodeId);
        return Map.of(
                "id", binding.nodeId,
                "computerId", binding.computerId,
                "inDim", binding.node.in.length,
                "outDim", binding.node.out.length,
                "autoEnabled", binding.autoEnabled,
                "connected", FamsRuntimeManager.get(level).isConnected(binding.computerId)
        );
    }

    private static Object listNodes(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        FamsRuntime runtime = FamsRuntimeManager.get(level);

        if (!runtime.isCentral(cid) && !runtime.isConnected(cid)) {
            throw new LuaException("Only central computer or connected nodes can list nodes");
        }

        boolean includeDetails = false;
        boolean includeValues = false;

        if (args != null && args.length >= 1) {
            if (args[0] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> options = (Map<String, Object>) args[0];
                includeDetails = Boolean.TRUE.equals(options.get("includeDetails"));
                includeValues = Boolean.TRUE.equals(options.get("includeValues"));
            } else if (args[0] instanceof Boolean) {
                includeDetails = (Boolean) args[0];
            }
        }

        List<Map<String, Object>> nodesList = new ArrayList<>();
        FamsRuntime runtimeInstance = FamsRuntimeManager.get(level);

        synchronized (runtimeInstance.lock) {
            Collection<FamsRuntime.NodeBinding> bindings;
            try {
                java.lang.reflect.Method getAllNodeBindings = runtimeInstance.getClass().getMethod("getAllNodeBindings");
                bindings = (Collection<FamsRuntime.NodeBinding>) getAllNodeBindings.invoke(runtimeInstance);
            } catch (Exception e) {
                try {
                    java.lang.reflect.Field bindingsByNodeId = runtimeInstance.getClass().getDeclaredField("bindingsByNodeId");
                    bindingsByNodeId.setAccessible(true);
                    java.util.concurrent.ConcurrentHashMap<String, FamsRuntime.NodeBinding> map =
                            (java.util.concurrent.ConcurrentHashMap<String, FamsRuntime.NodeBinding>) bindingsByNodeId.get(runtimeInstance);
                    bindings = map.values();
                } catch (Exception ex) {
                    throw new LuaException("Failed to retrieve node list: " + ex.getMessage());
                }
            }

            for (FamsRuntime.NodeBinding binding : bindings) {
                Map<String, Object> nodeInfo = new HashMap<>();
                nodeInfo.put("id", binding.nodeId);
                nodeInfo.put("computerId", binding.computerId);
                nodeInfo.put("inDim", binding.node.in.length);
                nodeInfo.put("outDim", binding.node.out.length);
                nodeInfo.put("autoEnabled", binding.autoEnabled);
                nodeInfo.put("connected", runtimeInstance.isConnected(binding.computerId));

                if (includeDetails) {
                    nodeInfo.put("registeredAt", System.currentTimeMillis()); // 可以添加注册时间戳
                    nodeInfo.put("isCentral", runtimeInstance.isCentral(binding.computerId));
                }

                if (includeValues) {
                    synchronized (runtimeInstance.lock) {
                        double[] inputCopy = Arrays.copyOf(binding.node.in, binding.node.in.length);
                        double[] outputCopy = Arrays.copyOf(binding.node.out, binding.node.out.length);

                        List<Double> inputList = new ArrayList<>();
                        List<Double> outputList = new ArrayList<>();
                        for (double d : inputCopy) inputList.add(d);
                        for (double d : outputCopy) outputList.add(d);

                        nodeInfo.put("currentInput", inputList);
                        nodeInfo.put("currentOutput", outputList);
                    }
                }

                nodesList.add(nodeInfo);
            }
        }

        nodesList.sort((a, b) -> {
            String idA = (String) a.get("id");
            String idB = (String) b.get("id");
            return idA.compareTo(idB);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("totalNodes", nodesList.size());
        result.put("nodes", nodesList);

        if (runtime.isCentral(cid)) {
            try {
                FamsCentral central = runtime.requireCentral();
                result.put("maxNodesCapacity", central.getMaxNodesCapacity());
                result.put("activeNodeCount", central.getActiveNodeCount());
            } catch (LuaException e) {
            }
        }

        return result;
    }

    private static Object setModeConfig(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can set mode config");
        }
        if(args == null || args.length < 2) {
            throw new LuaException("setModeConfig expects: (modeName, configTable)");
        }
        String modeName = (String) args[0];
        Map<String,Object> config = argsToMap(args, 1, "setModeConfig expects config table");

        FamsTypes.SystemMode mode = LuaParsing.parseSystemMode(modeName);
        FamsTypes.ModeConfig modeConfig = new FamsTypes.ModeConfig();

        FamsRuntimeManager.get(level).requireCentral().setModeConfig(mode, modeConfig);
        return true;
    }

    private static Object getModeConfig(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can get mode config");
        }
        if(args == null || args.length < 1) {
            throw new LuaException("getModeConfig expects: (modeName)");
        }
        String modeName = (String) args[0];
        FamsTypes.SystemMode mode = LuaParsing.parseSystemMode(modeName);
        FamsTypes.ModeConfig config = FamsRuntimeManager.get(level).requireCentral().getModeConfig(mode);

        return Map.of(
                "learningEnabled", config.learningEnabled,
                "learningRate", config.learningRate,
                "riskHardLimit", config.riskHardLimit,
                "emergencyTriggerRisk", config.emergencyTriggerRisk,
                "monitorIntervalMs", config.monitorIntervalMs,
                "enableExternalMonitor", config.enableExternalMonitor,
                "allowActionDispatch", config.allowActionDispatch
        );
    }

    private static Object memoryStats(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can get memory stats");
        }
        FamsMemory memory = FamsRuntimeManager.get(level).requireCentral().memory();
        FamsMemory.ShortTermStats shortStats = memory.shortTerm().stats();
        FamsMemory.LongTermStats longStats = memory.longTerm().stats();

        return Map.of(
                "shortTerm", Map.of(
                        "totalAdded", shortStats.totalAdded,
                        "totalExpired", shortStats.totalExpired,
                        "totalOverwritten", shortStats.totalOverwritten,
                        "currentSize", memory.shortTerm().size()
                ),
                "longTerm", Map.of(
                        "totalEnqueued", longStats.totalEnqueued,
                        "totalWritten", longStats.totalWritten,
                        "totalSyncFallback", longStats.totalSyncFallback,
                        "totalDropped", longStats.totalDropped
                )
        );
    }

    private static Object setLearningRate(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can set learning rate");
        }
        if(args == null || args.length < 1 || !(args[0] instanceof Number)) {
            throw new LuaException("setLearningRate expects: (learningRate)");
        }
        double lr = ((Number) args[0]).doubleValue();
        FamsRuntimeManager.get(level).requireCentral().setLearningRate(lr);
        return true;
    }

    private static Object getState(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if(!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can get state");
        }
        FamsTypes.State state = FamsRuntimeManager.get(level).requireCentral().state();
        return state.x;
    }

    private static Object getRisk(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        return FamsRuntimeManager.get(level).getRisk();
    }

    private static Object getLastLoss(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        return FamsRuntimeManager.get(level).getLastLoss();
    }

    private static Object memoryConfig(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if (!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can get memory config");
        }

        FamsMemory memory = FamsRuntimeManager.get(level).requireCentral().memory();
        FamsMemory.MemoryConfig config = memory.config();

        return Map.of(
                "shortTermCapacity", config.shortTermCapacity,
                "shortTermTtlMs", config.shortTermTtlMs,
                "longTermEnabled", config.longTermEnabled,
                "longTermQueueCapacity", config.longTermQueueCapacity,
                "longTermFlushIntervalMs", config.longTermFlushIntervalMs,
                "longTermSegmentMaxBytes", config.longTermSegmentMaxBytes,
                "longTermIndexStride", config.longTermIndexStride,
                "longTermRetentionDays", config.longTermRetentionDays,
                "allowSyncFallbackWhenQueueFull", config.allowSyncFallbackWhenQueueFull
        );
    }

    private static Object setMemoryConfig(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if (!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can set memory config");
        }

        if (args == null || args.length < 1) {
            throw new LuaException("setMemoryConfig expects: (configTable)");
        }

        Map<String, Object> configMap = argsToMap(args, 0, "setMemoryConfig expects config table");
        FamsMemory.MemoryConfig newConfig = new FamsMemory.MemoryConfig();

        if (configMap.containsKey("shortTermCapacity")) {
            newConfig.shortTermCapacity = LuaParsing.getInt(configMap, "shortTermCapacity", 4096, 16, 100000);
        }
        if (configMap.containsKey("shortTermTtlMs")) {
            newConfig.shortTermTtlMs = LuaParsing.getInt(configMap, "shortTermTtlMs", 600000, 0, (int) Long.MAX_VALUE);
        }
        if (configMap.containsKey("longTermEnabled")) {
            Object enabled = configMap.get("longTermEnabled");
            if (enabled instanceof Boolean) {
                newConfig.longTermEnabled = (Boolean) enabled;
            } else if (enabled instanceof Number) {
                newConfig.longTermEnabled = ((Number) enabled).intValue() != 0;
            }
        }
        if (configMap.containsKey("longTermQueueCapacity")) {
            newConfig.longTermQueueCapacity = LuaParsing.getInt(configMap, "longTermQueueCapacity", 2048, 64, 100000);
        }
        if (configMap.containsKey("longTermFlushIntervalMs")) {
            newConfig.longTermFlushIntervalMs = LuaParsing.getInt(configMap, "longTermFlushIntervalMs", 1500, 100, 60000);
        }
        if (configMap.containsKey("longTermSegmentMaxBytes")) {
            newConfig.longTermSegmentMaxBytes = LuaParsing.getInt(configMap, "longTermSegmentMaxBytes", 64 * 1024 * 1024, 1024 * 1024, 1024 * 1024 * 1024);
        }
        if (configMap.containsKey("longTermIndexStride")) {
            newConfig.longTermIndexStride = LuaParsing.getInt(configMap, "longTermIndexStride", 128, 1, 10000);
        }
        if (configMap.containsKey("longTermRetentionDays")) {
            newConfig.longTermRetentionDays = LuaParsing.getInt(configMap, "longTermRetentionDays", 30, 0, 3650);
        }
        if (configMap.containsKey("allowSyncFallbackWhenQueueFull")) {
            Object fallback = configMap.get("allowSyncFallbackWhenQueueFull");
            if (fallback instanceof Boolean) {
                newConfig.allowSyncFallbackWhenQueueFull = (Boolean) fallback;
            } else if (fallback instanceof Number) {
                newConfig.allowSyncFallbackWhenQueueFull = ((Number) fallback).intValue() != 0;
            }
        }

        FamsRuntimeManager.get(level).requireCentral().memory().reconfigure(newConfig);
        return true;
    }

    private static Object setLearningEnabled(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if (!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can set learning enabled");
        }

        if (args == null || args.length < 1) {
            throw new LuaException("setLearningEnabled expects: (enabled)");
        }

        boolean enabled;
        Object enabledObj = args[0];
        if (enabledObj instanceof Boolean) {
            enabled = (Boolean) enabledObj;
        } else if (enabledObj instanceof Number) {
            enabled = ((Number) enabledObj).intValue() != 0;
        } else {
            throw new LuaException("setLearningEnabled expects enabled boolean/number");
        }

        FamsRuntimeManager.get(level).requireCentral().setLearningEnabled(enabled);
        return true;
    }

    private static Object shutdown(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        FamsRuntime runtime = FamsRuntimeManager.get(level);

        if (!runtime.isCentral(cid)) {
            throw new LuaException("Only central computer can shutdown FAMS system");
        }

        boolean clearCentral = false;
        if (args != null && args.length >= 1) {
            Object clearObj = args[0];
            if (clearObj instanceof Boolean) {
                clearCentral = (Boolean) clearObj;
            } else if (clearObj instanceof Number) {
                clearCentral = ((Number) clearObj).intValue() != 0;
            }
        }

        try {
            // 关闭记忆系统
            FamsCentral central = runtime.requireCentral();
            central.shutdown();

            // 如果需要，清除中央
            if (clearCentral) {
                runtime.clearCentral(cid);
            }

            return Map.of(
                    "success", true,
                    "message", "FAMS system shutdown completed",
                    "centralCleared", clearCentral
            );
        } catch (Exception e) {
            throw new LuaException("Failed to shutdown FAMS system: " + e.getMessage());
        }
    }

    private static Object save(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if (!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can save FAMS state");
        }

        String filename = "fams_backup.dat";
        if (args != null && args.length >= 1 && args[0] instanceof String) {
            filename = (String) args[0];
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                throw new LuaException("Invalid filename: " + filename);
            }
        }

        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }

        try {
            FamsCentral central = FamsRuntimeManager.get(level).requireCentral();
            File saveDir = new File(System.getProperty("user.dir"), "fams_saves");
            if (!saveDir.exists()) {
                saveDir.mkdirs();
            }

            File saveFile = new File(saveDir, filename);
            central.save(saveFile);

            return Map.of(
                    "success", true,
                    "filename", filename,
                    "path", saveFile.getAbsolutePath(),
                    "size", saveFile.length()
            );
        } catch (IOException e) {
            throw new LuaException("Failed to save FAMS state: " + e.getMessage());
        }
    }

    private static Object load(ServerLevel level, BlockPos bridgePos, IComputerAccess computer, Object[] args) throws LuaException {
        int cid = FamsRuntimeManager.computerId(computer);
        if (!FamsRuntimeManager.get(level).isCentral(cid)) {
            throw new LuaException("Only central computer can load FAMS state");
        }

        String filename = "fams_backup.dat";
        if (args != null && args.length >= 1 && args[0] instanceof String) {
            filename = (String) args[0];
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                throw new LuaException("Invalid filename: " + filename);
            }
        }

        if (!filename.endsWith(".dat")) {
            filename += ".dat";
        }

        try {
            FamsCentral central = FamsRuntimeManager.get(level).requireCentral();
            File saveDir = new File(System.getProperty("user.dir"), "fams_saves");
            File saveFile = new File(saveDir, filename);

            if (!saveFile.exists()) {
                File memoryDir = central.memory().dir();
                File alternativeFile = new File(memoryDir.getParentFile(), "fams_saves/" + filename);
                if (alternativeFile.exists()) {
                    saveFile = alternativeFile;
                } else {
                    throw new LuaException("Save file not found: " + filename);
                }
            }

            central.load(saveFile);

            return Map.of(
                    "success", true,
                    "filename", filename,
                    "path", saveFile.getAbsolutePath(),
                    "size", saveFile.length()
            );
        } catch (IOException e) {
            throw new LuaException("Failed to load FAMS state: " + e.getMessage());
        }
    }
}
