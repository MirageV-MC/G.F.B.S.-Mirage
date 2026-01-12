package org.mirage.gfbs.fams.central;

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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.mirage.gfbs.fams.central.FamsTypes.*;

public final class FamsCentral {
    private final int n,m;
    private final State state;
    private final Action action;
    private final Goal goal;
    private final Safety safety;

    private final MetaSpace meta;
    private final OnlineLearner learner;

    private final FamsMemory memory;

    private Node[] nodes;
    private int nodeCount;

    // 运行参数
    private double lossLast;
    private boolean learningEnabled;
    private long step;

    // 系统状态/模式
    private volatile SystemMode mode;
    private final ModeConfig cfgSleep;
    private final ModeConfig cfgPartial;
    private final ModeConfig cfgFormal;
    private final ModeConfig cfgEmergency;

    // PARTIAL_AUTO：节点自动响应开关（按 addNode 顺序对齐）
    private boolean[] nodeAutoEnabled;

    // 休眠模式外部循环监测线程
    private volatile boolean monitorRun;
    private Thread monitorThread;

    // 记忆系统存储目录（长期历史 + 短期窗口）
    private static File defaultMemoryDir(){
        String ud=System.getProperty("user.dir");
        File base = ud==null? new File(".") : new File(ud);
        // 优先使用 Minecraft/Forge 常见的 config 目录
        File cfg=new File(base, "config");
        File dir = cfg.exists() && cfg.isDirectory() ? new File(cfg, "fams_memory") : new File(base, "fams_memory");
        if(dir.exists()){
            if(!dir.isDirectory()){ try{ dir.delete(); }catch(Throwable ignore){} }
        }
        if(!dir.exists()){ try{ dir.mkdirs(); }catch(Throwable ignore){} }
        return dir;
    }

    public FamsCentral(int stateDimN, int actionDimM, int maxShells, int maxNodes){
        this.n=stateDimN; this.m=actionDimM;
        state=new State(n);
        action=new Action(m);
        goal=new Goal(n);
        safety=new Safety(n,m);

        meta=new MetaSpace(n,m,maxShells);
        learner=new OnlineLearner(n,m);

        memory=new FamsMemory(n,m,defaultMemoryDir());
        // 默认：长期记忆启用 + 短期 TTL 窗口（可通过 memory().reconfigure 动态调整）

        nodes=new Node[maxNodes];
        nodeCount=0;

        nodeAutoEnabled=new boolean[maxNodes];
        Arrays.fill(nodeAutoEnabled, true);

        // 预置模式参数（可在运行时动态修改）
        cfgSleep=new ModeConfig();
        cfgSleep.learningEnabled=false;
        cfgSleep.learningRate=0.0;
        cfgSleep.allowActionDispatch=false;
        cfgSleep.riskHardLimit=0.80;
        cfgSleep.emergencyTriggerRisk=0.88;
        cfgSleep.monitorIntervalMs=200;
        cfgSleep.enableExternalMonitor=true;

        cfgPartial=new ModeConfig();
        cfgPartial.learningEnabled=true;
        cfgPartial.learningRate=0.008;
        cfgPartial.allowActionDispatch=true;
        cfgPartial.riskHardLimit=0.85;
        cfgPartial.emergencyTriggerRisk=0.92;
        cfgPartial.monitorIntervalMs=250;
        cfgPartial.enableExternalMonitor=false;

        cfgFormal=new ModeConfig();
        cfgFormal.learningEnabled=true;
        cfgFormal.learningRate=0.01;
        cfgFormal.allowActionDispatch=true;
        cfgFormal.riskHardLimit=0.85;
        cfgFormal.emergencyTriggerRisk=0.93;
        cfgFormal.monitorIntervalMs=250;
        cfgFormal.enableExternalMonitor=false;

        cfgEmergency=new ModeConfig();
        cfgEmergency.learningEnabled=false;
        cfgEmergency.learningRate=0.0;
        cfgEmergency.allowActionDispatch=true;
        cfgEmergency.riskHardLimit=0.75;
        cfgEmergency.emergencyTriggerRisk=0.75;
        cfgEmergency.monitorIntervalMs=150;
        cfgEmergency.enableExternalMonitor=false;

        meta.addShell();
        meta.addShell();

        for(int i=0;i<n;i++){ goal.targetX[i]=0; goal.weight[i]=1.0; }
        learningEnabled=true;
        lossLast=0;
        step=0;

        // 默认进入正式处理模式
        mode=SystemMode.FORMAL;
        applyModeConfig(cfgFormal);
    }

    // 注册子系统节点
    public void addNode(Node nd){
        if(nodeCount>=nodes.length) return;
        nodes[nodeCount]=nd;
        if(nodeAutoEnabled!=null && nodeCount<nodeAutoEnabled.length) nodeAutoEnabled[nodeCount]=true;
        nodeCount++;
    }

    // 目标设定
    public Goal goal(){ return goal; }
    public Safety safety(){ return safety; }
    public State state(){ return state; }
    public Action action(){ return action; }
    public FamsMemory memory(){ return memory; }

    public void setLearningEnabled(boolean b){ learningEnabled=b; }
    public void setLearningRate(double lr){ learner.setLR(lr); }

    public SystemMode mode(){ return mode; }

    public synchronized void setMode(SystemMode newMode){
        if(newMode==null) return;
        if(this.mode==newMode) return;

        SystemMode old=this.mode;
        this.mode=newMode;
        applyModeConfig(getModeConfigInternal(newMode));

        // 休眠模式启用外部自动循环监测系统
        if(newMode==SystemMode.SLEEP){
            startMonitorIfNeeded();
        } else {
            stopMonitor();
        }

        // 记录模式切换到记忆系统（长期+短期）
        long now=System.currentTimeMillis();
        int flags=FamsMemory.Flags.MODE_SWITCH;
        String note="MODE_SWITCH:"+String.valueOf(old)+"->"+String.valueOf(newMode);
        memory.record(now, step, newMode, flags, safety.risk, lossLast, state.x, action.u, note);
    }

    /** 动态修改某个模式的参数（会拷贝保存，避免外部引用被修改） */
    public synchronized void setModeConfig(SystemMode m, ModeConfig cfg){
        if(m==null || cfg==null) return;
        ModeConfig dst=getModeConfigRef(m);
        ModeConfig c=cfg.copy();
        dst.learningEnabled=c.learningEnabled;
        dst.learningRate=c.learningRate;
        dst.riskHardLimit=c.riskHardLimit;
        dst.emergencyTriggerRisk=c.emergencyTriggerRisk;
        dst.monitorIntervalMs=c.monitorIntervalMs;
        dst.enableExternalMonitor=c.enableExternalMonitor;
        dst.allowActionDispatch=c.allowActionDispatch;

        if(this.mode==m){
            applyModeConfig(dst);
            if(m==SystemMode.SLEEP) startMonitorIfNeeded();
        }
        // 记录模式参数变更（用于长期回溯）
        long now=System.currentTimeMillis();
        String note="MODE_CONFIG_UPDATE:"+String.valueOf(m);
        memory.record(now, step, mode, FamsMemory.Flags.NONE, safety.risk, lossLast, state.x, action.u, note);
    }

    /** 获取某个模式当前参数快照 */
    public synchronized ModeConfig getModeConfig(SystemMode m){
        if(m==null) return null;
        return getModeConfigInternal(m).copy();
    }

    /** PARTIAL_AUTO：按节点 id 设置是否自动响应（非自动节点的 out 会被置 0） */
    public synchronized void setNodeAutoEnabled(String nodeId, boolean enabled){
        if(nodeId==null) return;
        for(int i=0;i<nodeCount;i++){
            if(nodeId.equals(nodes[i].id)){
                nodeAutoEnabled[i]=enabled;
                return;
            }
        }
    }

    /** PARTIAL_AUTO：按节点序号设置是否自动响应 */
    public synchronized void setNodeAutoEnabled(int nodeIndex, boolean enabled){
        if(nodeIndex<0 || nodeIndex>=nodeCount) return;
        nodeAutoEnabled[nodeIndex]=enabled;
    }

    private ModeConfig getModeConfigInternal(SystemMode m){
        switch(m){
            case SLEEP: return cfgSleep;
            case PARTIAL_AUTO: return cfgPartial;
            case EMERGENCY: return cfgEmergency;
            case FORMAL:
            default: return cfgFormal;
        }
    }

    private ModeConfig getModeConfigRef(SystemMode m){
        return getModeConfigInternal(m);
    }

    private void applyModeConfig(ModeConfig cfg){
        // 学习参数
        learningEnabled=cfg.learningEnabled;
        if(cfg.learningRate>0) learner.setLR(cfg.learningRate);
        // 安全阈值
        safety.riskHardLimit=cfg.riskHardLimit;
    }

    private void startMonitorIfNeeded(){
        ModeConfig cfg=cfgSleep;
        if(!cfg.enableExternalMonitor) return;
        if(monitorRun && monitorThread!=null && monitorThread.isAlive()) return;
        monitorRun=true;
        monitorThread=new Thread(new Runnable(){
            @Override public void run(){
                while(monitorRun){
                    try{
                        if(mode!=SystemMode.SLEEP) break;
                        sleepMonitorTick();
                        long ms=cfgSleep.monitorIntervalMs;
                        if(ms<10) ms=10;
                        Thread.sleep(ms);
                    }catch(InterruptedException e){
                        break;
                    }catch(Throwable t){
                        // 忽略监测线程异常，继续循环
                    }
                }
            }
        }, "FAMS-SleepMonitor");
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void stopMonitor(){
        monitorRun=false;
        if(monitorThread!=null){
            try{ monitorThread.interrupt(); }catch(Throwable ignore){}
        }
    }

    // 休眠模式下的外部循环监测：仅汇聚与风险评估，必要时自动切换模式
    private void sleepMonitorTick(){
        step++;
        ingest();
        evalRisk();

        // 休眠监测：动作默认全 0，但仍记录状态/风险/损失到记忆系统，以形成完整历史
        for(int i=0;i<m;i++) action.u[i]=0;
        boolean interlock = safety.risk >= safety.riskHardLimit;
        safetyInterlock();
        double L=loss();
        lossLast=L;

        int flags = interlock ? FamsMemory.Flags.SAFETY_INTERLOCK : FamsMemory.Flags.NONE;
        long now=System.currentTimeMillis();
        memory.record(now, step, mode, flags, safety.risk, L, state.x, action.u, "SLEEP_MONITOR_TICK");

        autoSwitchIfIncident(cfgSleep);

        if(mode==SystemMode.SLEEP){
            dispatchWithPolicy(cfgSleep);
        }
    }

    private void autoSwitchIfIncident(ModeConfig cfg){
        double r=safety.risk;

        // 基于短期记忆的风险趋势判定：用于更早的事故预警（避免只看单点）
        double slope = memory.shortTerm().riskSlope(6000);          // risk / second
        double avgR  = memory.shortTerm().avgRisk(6000);
        boolean trendIncident =
                slope > 0.20 &&
                avgR  > (cfg.emergencyTriggerRisk * 0.70) &&
                r     > (cfg.emergencyTriggerRisk * 0.55);

        if(r>=1.0 || r>=cfg.emergencyTriggerRisk || r>=safety.riskHardLimit || trendIncident){
            long now=System.currentTimeMillis();
            String note="INCIDENT_TRIGGER:r="+r+",avgR="+avgR+",slope="+slope+",cfgTrig="+cfg.emergencyTriggerRisk;
            memory.record(now, step, mode, FamsMemory.Flags.INCIDENT, r, lossLast, state.x, action.u, note);

            setMode(SystemMode.EMERGENCY);
            emergencyTick();
        }
    }

    private void emergencyTick(){
        ingest();
        evalRisk();
        for(int i=0;i<m;i++) action.u[i]=0;

        boolean interlock = safety.risk >= safety.riskHardLimit;
        safetyInterlock();

        double L=loss();
        lossLast=L;

        int flags = FamsMemory.Flags.INCIDENT | (interlock ? FamsMemory.Flags.SAFETY_INTERLOCK : 0);
        long now=System.currentTimeMillis();
        memory.record(now, step, mode, flags, safety.risk, L, state.x, action.u, "EMERGENCY_TICK");

        dispatchWithPolicy(cfgEmergency);
    }

    private void dispatchWithPolicy(ModeConfig cfg){
        if(cfg!=null && !cfg.allowActionDispatch){
            for(int ni=0;ni<nodeCount;ni++){
                Node nd=nodes[ni];
                for(int k=0;k<nd.out.length;k++) nd.out[k]=0;
            }
            return;
        }

        if(mode==SystemMode.PARTIAL_AUTO){
            int idx=0;
            for(int ni=0;ni<nodeCount;ni++){
                Node nd=nodes[ni];
                boolean en=(nodeAutoEnabled!=null && ni<nodeAutoEnabled.length && nodeAutoEnabled[ni]);
                if(!en){
                    for(int k=0;k<nd.out.length;k++) nd.out[k]=0;
                    // 即使不下发，也要消耗对应动作片段，保证全局动作布局不变
                    idx += nd.out.length;
                    if(idx>m) idx=m;
                    continue;
                }
                for(int k=0;k<nd.out.length && idx<m;k++,idx++){
                    nd.out[k]=action.u[idx];
                }
                if(idx>=m){
                    // 后续节点 out 清零
                    for(int nj=ni+1;nj<nodeCount;nj++){
                        Node nnd=nodes[nj];
                        for(int k=0;k<nnd.out.length;k++) nnd.out[k]=0;
                    }
                    break;
                }
            }
            return;
        }

        // 默认全量分发
        dispatchRaw();
    }

    // 汇聚：将各节点摘要 in 累加/拼接到 state.x
    public void ingest(){
        for(int i=0;i<n;i++) state.x[i]=0;

        int idx=0;
        for(int ni=0;ni<nodeCount;ni++){
            Node nd=nodes[ni];
            for(int k=0;k<nd.in.length && idx<n;k++,idx++){
                state.x[idx]+=nd.in[k];
            }
            if(idx>=n) break;
        }
        state.t=step;
    }

    // 风险估计 (软阈值->风险上升, 硬阈值->强制联锁）
    private void evalRisk(){
        double r=0;
        for(int i=0;i<n;i++){
            double ax=Math.abs(state.x[i]);
            double soft=safety.xSoft[i], hard=safety.xHard[i];
            if(ax>hard){
                r=1.0;
                break;
            }
            if(ax>soft){
                double d=(ax-soft)/(hard-soft+1e-9);
                if(d<0) d=0;
                if(d>1) d=1;
                r += d*0.08;
            }
        }
        if(r>1) r=1;
        safety.risk=r;
    }

    // 安全联锁
    private void safetyInterlock(){
        if(safety.risk >= safety.riskHardLimit){
            for(int i=0;i<m;i++) action.u[i]=0;
            SpaceMath.clamp(action.u, safety.uMin, safety.uMax);
        }
    }

    // 损失: 目标误差能量|风险惩罚
    private double loss(){
        double s=0;
        for(int i=0;i<n;i++){
            double e=(goal.targetX[i]-state.x[i])*goal.weight[i];
            s += e*e;
        }
        s = s/(n+1e-9);
        s += safety.risk*2.0;
        return s;
    }

    // 把全局动作分发到各节点 out
    private void dispatchRaw(){
        int idx=0;
        for(int ni=0;ni<nodeCount;ni++){
            Node nd=nodes[ni];
            for(int k=0;k<nd.out.length && idx<m;k++,idx++){
                nd.out[k]=action.u[idx];
            }
            if(idx>=m) break;
        }
    }

    // 按模式策略分发
    public void dispatch(){
        dispatchWithPolicy(getModeConfigInternal(mode));
    }

    // 单步: 汇聚->风险->元空间提案->联锁->学习->分发
// 单步: 汇聚->风险->元空间提案->联锁->学习->分发
    public void step(){
        step++;
        ingest();
        evalRisk();

        ModeConfig cfg=getModeConfigInternal(mode);
        autoSwitchIfIncident(cfg);

        // 紧急模式下，强制走 emergencyTick（内部会记录记忆）
        if(mode==SystemMode.EMERGENCY){
            emergencyTick();
            return;
        }

        // 休眠模式下默认不主动决策，但仍记录（用于历史分析/趋势检测）
        if(mode==SystemMode.SLEEP){
            for(int i=0;i<m;i++) action.u[i]=0;

            boolean interlock = safety.risk >= safety.riskHardLimit;
            safetyInterlock();

            double L=loss();
            lossLast=L;

            int flags = interlock ? FamsMemory.Flags.SAFETY_INTERLOCK : FamsMemory.Flags.NONE;
            long now=System.currentTimeMillis();
            memory.record(now, step, mode, flags, safety.risk, L, state.x, action.u, "STEP_SLEEP");

            meta.feedback(L, safety);
            dispatchWithPolicy(cfgSleep);
            return;
        }

        // 正式/部分自动：提案动作
        meta.propose(state, goal, safety, action);

        boolean interlock = safety.risk >= safety.riskHardLimit;
        safetyInterlock();

        double L=loss();
        lossLast=L;

        int flags = interlock ? FamsMemory.Flags.SAFETY_INTERLOCK : FamsMemory.Flags.NONE;
        long now=System.currentTimeMillis();
        String note="STEP:"+String.valueOf(mode)+(learningEnabled?":LEARN_ON":":LEARN_OFF");
        memory.record(now, step, mode, flags, safety.risk, L, state.x, action.u, note);

        if(learningEnabled){
            int best=0;
            double bs=meta.get(0).score;
            for(int i=1;i<meta.count();i++){
                double sc=meta.get(i).score;
                if(sc>bs){ bs=sc; best=i; }
            }
            learner.trainShell(meta.get(best), state, goal, safety, L);
        }
        meta.feedback(L, safety);

        dispatchWithPolicy(cfg);
    }

    /** 停止外部监测与记忆写盘线程（建议在模组卸载/服务器停机时调用） */
    public void shutdown(){
        stopMonitor();
        try{ memory.close(); }catch(Throwable ignore){}
    }

    public double lastLoss(){ return lossLast; }

    // 持久化
    public void save(File f) throws IOException { Persistence.save(f, meta); }
    public void load(File f) throws IOException { Persistence.load(f, meta); }


    public int getMaxNodesCapacity() {
        return nodes.length;
    }

    public int getActiveNodeCount() {
        return nodeCount;
    }

    public synchronized Map<String, Object> getNodeInfo(String nodeId) {
        for (int i = 0; i < nodeCount; i++) {
            if (nodes[i].id.equals(nodeId)) {
                Map<String, Object> info = new HashMap<>();
                info.put("id", nodes[i].id);
                info.put("inDim", nodes[i].in.length);
                info.put("outDim", nodes[i].out.length);
                info.put("index", i);
                info.put("autoEnabled", i < nodeAutoEnabled.length ? nodeAutoEnabled[i] : true);
                info.put("currentInput", Arrays.copyOf(nodes[i].in, nodes[i].in.length));
                info.put("currentOutput", Arrays.copyOf(nodes[i].out, nodes[i].out.length));
                return info;
            }
        }
        return null;
    }
}
