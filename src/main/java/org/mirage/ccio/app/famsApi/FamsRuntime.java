package org.mirage.ccio.app.famsApi;

import dan200.computercraft.api.lua.LuaException;
import org.mirage.fams.central.FamsCentral;
import org.mirage.fams.central.FamsTypes;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class FamsRuntime {

    public static class NodeBinding {
        public final int computerId;
        public final String nodeId;
        public final FamsTypes.Node node;
        public volatile boolean autoEnabled = true;

        private NodeBinding(int computerId, String nodeId, FamsTypes.Node node){
            this.computerId = computerId;
            this.nodeId = nodeId;
            this.node = node;
        }
    }

    final Object lock = new Object();

    private volatile Integer centralComputerId = null;
    private volatile FamsCentral central = null;

    private final ConcurrentHashMap<Integer, Boolean> connected = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, NodeBinding> bindingsByComputer = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, NodeBinding> bindingsByNodeId = new ConcurrentHashMap<>();

    private final AtomicBoolean schedulerRunning = new AtomicBoolean(false);
    private ScheduledExecutorService scheduler;

    public boolean hasCentral(){
        return centralComputerId != null && central != null;
    }

    public int requireCentralId() throws LuaException{
        Integer id = centralComputerId;
        if(id==null) throw new LuaException("FAMS central is not set. Call fams.setCentral() on the central computer first.");
        return id;
    }

    public boolean isCentral(int computerId){
        Integer id = centralComputerId;
        return id != null && id == computerId;
    }

    public FamsCentral requireCentral() throws LuaException{
        FamsCentral c = central;
        if(c==null) throw new LuaException("FAMS central is not set. Call fams.setCentral() first.");
        return c;
    }

    public void setCentral(int computerId, Map<String,Object> config) throws LuaException{
        Objects.requireNonNull(config, "config");
        synchronized (lock){
            if(centralComputerId != null && centralComputerId != computerId){
                throw new LuaException("FAMS central already set to computerId=" + centralComputerId);
            }

            int stateDim = LuaParsing.getInt(config, "stateDim", 16, 1, 4096);
            int actionDim = LuaParsing.getInt(config, "actionDim", 16, 1, 4096);
            int maxShells = LuaParsing.getInt(config, "maxShells", 64, 1, 4096);
            int maxNodes  = LuaParsing.getInt(config, "maxNodes", 64, 1, 4096);

            centralComputerId = computerId;
            central = new FamsCentral(stateDim, actionDim, maxShells, maxNodes);

            // The central computer is always considered connected.
            connected.put(computerId, Boolean.TRUE);
        }

        // optional scheduler
        Integer intervalMs = LuaParsing.getNullableInt(config, "tickIntervalMs", 0, 0, 600_000);
        if(intervalMs != null && intervalMs > 0){
            startScheduler(intervalMs);
        } else {
            stopScheduler();
        }
    }

    public void clearCentral(int computerId) throws LuaException{
        synchronized (lock){
            if(centralComputerId == null) return;
            if(centralComputerId != computerId){
                throw new LuaException("Only the central computer can clear central.");
            }
            stopScheduler();
            centralComputerId = null;
            central = null;
            connected.clear();
            bindingsByComputer.clear();
            bindingsByNodeId.clear();
        }
    }

    public void connect(int computerId, Integer centralId) throws LuaException{
        int cid = requireCentralId();
        if(centralId != null && centralId != cid){
            throw new LuaException("Central computerId mismatch. Expected " + cid + ", got " + centralId);
        }
        connected.put(computerId, Boolean.TRUE);
    }

    public void disconnect(int computerId){
        connected.remove(computerId);
        NodeBinding b = bindingsByComputer.remove(computerId);
        if(b != null){
            bindingsByNodeId.remove(b.nodeId);
            synchronized (lock){
                // Keep central.nodes array consistent by simply disabling auto and zeroing in/out.
                Arrays.fill(b.node.in, 0);
                Arrays.fill(b.node.out, 0);
                b.autoEnabled = false;
            }
        }
    }

    public boolean isConnected(int computerId){
        return Boolean.TRUE.equals(connected.get(computerId));
    }

    public NodeBinding registerNode(int computerId, Map<String,Object> spec) throws LuaException{
        if(!isConnected(computerId)){
            throw new LuaException("Computer not connected to central. Call fams.connectCentral() first.");
        }
        if(isCentral(computerId)){
            throw new LuaException("Central computer cannot register itself as a node. Use separate computers as nodes.");
        }
        String nodeId = LuaParsing.getString(spec, "id", null);
        if(nodeId == null || nodeId.isBlank()){
            // default stable id per computer
            nodeId = "cc_" + computerId;
        }

        int inDim  = LuaParsing.getInt(spec, "inDim", 8, 1, 4096);
        int outDim = LuaParsing.getInt(spec, "outDim", 8, 1, 4096);

        synchronized (lock){
            if(bindingsByNodeId.containsKey(nodeId)){
                NodeBinding existing = bindingsByNodeId.get(nodeId);
                throw new LuaException("NodeId already registered: " + nodeId + " (computerId=" + existing.computerId + ")");
            }
            if(bindingsByComputer.containsKey(computerId)){
                NodeBinding existing = bindingsByComputer.get(computerId);
                throw new LuaException("This computer already registered as node: " + existing.nodeId);
            }

            FamsCentral c = requireCentral();
            FamsTypes.Node nd = new FamsTypes.Node(nodeId, inDim, outDim);
            c.addNode(nd);

            NodeBinding binding = new NodeBinding(computerId, nodeId, nd);
            bindingsByComputer.put(computerId, binding);
            bindingsByNodeId.put(nodeId, binding);
            return binding;
        }
    }

    public NodeBinding requireBinding(int computerId) throws LuaException{
        NodeBinding b = bindingsByComputer.get(computerId);
        if(b==null) throw new LuaException("This computer is not registered as a FAMS node. Call fams.registerNode() first.");
        return b;
    }

    public NodeBinding requireBindingByNodeId(String nodeId) throws LuaException{
        NodeBinding b = bindingsByNodeId.get(nodeId);
        if(b==null) throw new LuaException("Unknown nodeId: " + nodeId);
        return b;
    }

    public void pushIn(int computerId, Object value) throws LuaException{
        NodeBinding b = requireBinding(computerId);
        double[] arr = LuaParsing.toDoubleArray(value);
        synchronized (lock){
            int n = Math.min(arr.length, b.node.in.length);
            for(int i=0;i<n;i++) b.node.in[i] = arr[i];
            for(int i=n;i<b.node.in.length;i++) b.node.in[i] = 0;
        }
    }

    public double[] pullOut(int computerId) throws LuaException{
        NodeBinding b = requireBinding(computerId);
        synchronized (lock){
            return Arrays.copyOf(b.node.out, b.node.out.length);
        }
    }

    public void setNodeAutoEnabled(String nodeId, boolean enabled) throws LuaException{
        NodeBinding b = requireBindingByNodeId(nodeId);
        synchronized (lock){
            b.autoEnabled = enabled;
            requireCentral().setNodeAutoEnabled(nodeId, enabled);
        }
    }

    public Object step(int computerId) throws LuaException{
        if(!isCentral(computerId)){
            throw new LuaException("Only central computer can call fams.step().");
        }
        synchronized (lock){
            requireCentral().step();
            return Boolean.TRUE;
        }
    }

    public Object setMode(int computerId, String modeName) throws LuaException{
        if(!isCentral(computerId)){
            throw new LuaException("Only central computer can call fams.setMode().");
        }
        synchronized (lock){
            requireCentral().setMode(LuaParsing.parseSystemMode(modeName));
            return Boolean.TRUE;
        }
    }

    public String getMode() throws LuaException{
        synchronized (lock){
            return requireCentral().mode().name();
        }
    }

    public double getRisk() throws LuaException{
        synchronized (lock){
            return requireCentral().safety().risk;
        }
    }

    public double getLastLoss() throws LuaException{
        synchronized (lock){
            return requireCentral().lastLoss();
        }
    }

    public Object stats() throws LuaException{
        synchronized (lock){
            FamsCentral c = requireCentral();
            return Map.of(
                    "mode", c.mode().name(),
                    "risk", c.safety().risk,
                    "lossLast", c.lastLoss()
            );
        }
    }

    public void startScheduler(int intervalMs){
        if(intervalMs <= 0) intervalMs = 50;
        if(schedulerRunning.compareAndSet(false, true)){
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "gfbs-fams-central");
                t.setDaemon(true);
                return t;
            });
            final int iv = intervalMs;
            scheduler.scheduleAtFixedRate(() -> {
                try{
                    synchronized (lock){
                        if(central != null) central.step();
                    }
                }catch(Throwable ignored){
                }
            }, iv, iv, TimeUnit.MILLISECONDS);
        }
    }

    public void stopScheduler(){
        if(schedulerRunning.compareAndSet(true, false)){
            ScheduledExecutorService ex = scheduler;
            scheduler = null;
            if(ex != null){
                ex.shutdownNow();
            }
        }
    }


    public synchronized List<NodeBinding> getAllNodeBindings() {
        return new ArrayList<>(bindingsByNodeId.values());
    }

    public synchronized Map<String, Object> getCentralNodesInfo() throws LuaException {
        Map<String, Object> result = new HashMap<>();

        if (!hasCentral()) {
            throw new LuaException("FAMS central is not set");
        }

        List<Map<String, Object>> nodesList = new ArrayList<>();
        for (NodeBinding binding : bindingsByNodeId.values()) {
            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("nodeId", binding.nodeId);
            nodeInfo.put("computerId", binding.computerId);
            nodeInfo.put("inDim", binding.node.in.length);
            nodeInfo.put("outDim", binding.node.out.length);
            nodeInfo.put("autoEnabled", binding.autoEnabled);
            nodeInfo.put("connected", isConnected(binding.computerId));

            synchronized (lock) {
                nodeInfo.put("currentInput", Arrays.copyOf(binding.node.in, binding.node.in.length));
                nodeInfo.put("currentOutput", Arrays.copyOf(binding.node.out, binding.node.out.length));
            }

            nodesList.add(nodeInfo);
        }

        result.put("totalNodes", nodesList.size());
        result.put("maxNodes", requireCentral().getMaxNodesCapacity()); // 需要在FamsCentral中添加此方法
        result.put("nodes", nodesList);

        return result;
    }
}
