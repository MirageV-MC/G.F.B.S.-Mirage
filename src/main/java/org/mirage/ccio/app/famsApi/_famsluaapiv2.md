# FAMS CC:Tweaked Lua API 教程

## 概述

FAMS（Facility Autonomous Management System）是一个分布式自主管理系统，通过中央计算机和节点计算机的协作实现设施的智能控制。本教程介绍如何使用CC:Tweaked Lua API与FAMS系统交互。
本文档由AI创作。

## 快速开始

### 中央计算机设置

```lua
-- 包装CC I/O Bridge外围设备
local ccio = peripheral.find("gfbs")

-- 初始化FAMS中央系统
local result = ccio.invokeApi("fams.setCentral", {
    stateDim = 16,      -- 状态维度
    actionDim = 16,     -- 动作维度  
    maxShells = 64,     -- 最大壳数量
    maxNodes = 64,      -- 最大节点数量
    tickIntervalMs = 50 -- 自动步进间隔(毫秒)
})

print("中央系统初始化:", result)
```

### 节点计算机连接

```lua
-- 包装CC I/O Bridge外围设备
local ccio = peripheral.find("gfbs")

-- 连接到中央计算机
local result = ccio.invokeApi("fams.connectCentral")
-- 或者指定中央计算机ID
-- local result = ccio.invokeApi("fams.connectCentral", {centralId})

print("连接结果:", result)

-- 注册节点
local nodeInfo = ccio.invokeApi("fams.registerNode", {
    id = "DMR_CONTROL",  -- 节点唯一标识符
    inDim = 8,           -- 输入维度
    outDim = 8           -- 输出维度
})

print("节点注册成功:", textutils.serialize(nodeInfo))
```

## 核心操作

### 数据输入输出

```lua
-- 推送输入数据到节点
local inputData = {0.2, 0.7, 0.0, 0.5, 0.3, 0.9, 0.1, 0.4}
local result = ccio.invokeApi("fams.pushIn", inputData)

-- 从节点拉取输出数据
local outputData = ccio.invokeApi("fams.pullOut")
print("输出数据:", textutils.serialize(outputData))
```

### 系统控制

```lua
-- 手动执行一步计算
local stepResult = ccio.invokeApi("fams.step")
print("步进完成:", stepResult)

-- 设置系统模式
local modes = {"SLEEP", "PARTIAL_AUTO", "FORMAL", "EMERGENCY"}
local modeResult = ccio.invokeApi("fams.setMode", "FORMAL")

-- 获取当前模式
local currentMode = ccio.invokeApi("fams.getMode")
print("当前模式:", currentMode)

-- 获取系统统计信息
local stats = ccio.invokeApi("fams.stats")
print("系统统计:", textutils.serialize(stats))
```

## 高级配置

### 目标设置

```lua
-- 仅在中央计算机上执行
local goalResult = ccio.invokeApi("fams.setGoal", {
    target = {0.5, 0.3, 0.7},  -- 目标状态值
    weights = {1.0, 0.8, 0.5}  -- 各维度权重
})

-- 获取当前目标配置
local currentGoal = ccio.invokeApi("fams.getGoal")
```

### 安全参数配置

```lua
-- 设置安全限制
local safetyResult = ccio.invokeApi("fams.setSafetyLimits", {
    riskHardLimit = 0.85,
    emergencyTriggerRisk = 0.92,
    uMin = -1.0,  -- 动作下限
    uMax = 1.0,   -- 动作上限
    stateThresholds = {
        soft = 0.8,  -- 状态软阈值
        hard = 1.0   -- 状态硬阈值
    }
})

-- 获取安全配置
local safetyConfig = ccio.invokeApi("fams.getSafetyLimits")
```

### 节点管理

```lua
-- 设置节点自动启用状态
local autoResult = ccio.invokeApi("fams.setNodeAutoEnabled", {"DMR_CONTROL", true})

-- 获取节点信息
local nodeInfo = ccio.invokeApi("fams.getNodeInfo", "DMR_CONTROL")

-- 列出所有节点
local nodesList = ccio.invokeApi("fams.listNodes")
-- 包含详细信息和当前值
local detailedList = ccio.invokeApi("fams.listNodes", {
    includeDetails = true,
    includeValues = true
})
```

## 记忆系统

### 记忆统计

```lua
-- 获取记忆系统统计信息
local memoryStats = ccio.invokeApi("fams.memoryStats")
print("记忆统计:", textutils.serialize(memoryStats))

-- 获取记忆配置
local memoryConfig = ccio.invokeApi("fams.memoryConfig")

-- 配置记忆系统
local configResult = ccio.invokeApi("fams.setMemoryConfig", {
    shortTermCapacity = 8192,      -- 短期记忆容量
    shortTermTtlMs = 600000,        -- 短期记忆存活时间
    longTermEnabled = true,         -- 启用长期记忆
    longTermQueueCapacity = 4096,   -- 长期记忆队列容量
    longTermRetentionDays = 30     -- 长期记忆保留天数
})
```

## 学习控制

```lua
-- 设置学习率
local lrResult = ccio.invokeApi("fams.setLearningRate", 0.01)

-- 启用/禁用学习
local learnResult = ccio.invokeApi("fams.setLearningEnabled", true)
```

## 实时监控

```lua
-- 获取当前状态
local currentState = ccio.invokeApi("fams.getState")

-- 获取当前风险值
local currentRisk = ccio.invokeApi("fams.getRisk")

-- 获取上次损失值
local lastLoss = ccio.invokeApi("fams.getLastLoss")
```

## 系统管理

### 持久化操作

```lua
-- 保存系统状态
local saveResult = ccio.invokeApi("fams.save", "backup_20240102")

-- 加载系统状态  
local loadResult = ccio.invokeApi("fams.load", "backup_20240102")
```

### 系统关闭

```lua
-- 优雅关闭系统
local shutdownResult = ccio.invokeApi("fams.shutdown")

-- 关闭并清除中央配置
local shutdownClear = ccio.invokeApi("fams.shutdown", true)
```

## 模式配置

```lua
-- 配置特定模式的参数
local modeConfigResult = ccio.invokeApi("fams.setModeConfig", {
    "FORMAL",  -- 模式名称
    {
        learningEnabled = true,
        learningRate = 0.01,
        riskHardLimit = 0.85,
        allowActionDispatch = true
    }
})

-- 获取模式配置
local formalConfig = ccio.invokeApi("fams.getModeConfig", "FORMAL")
```

## 完整示例

### 中央计算机完整配置

```lua
local ccio = peripheral.find("gfbs")

-- 初始化系统
ccio.invokeApi("fams.setCentral", {
    stateDim = 16,
    actionDim = 16,
    maxShells = 64,
    maxNodes = 32,
    tickIntervalMs = 100
})

-- 设置目标
ccio.invokeApi("fams.setGoal", {
    target = {0.5, 0.6, 0.4, 0.7},
    weights = {1.0, 0.9, 0.8, 0.7}
})

-- 配置安全参数
ccio.invokeApi("fams.setSafetyLimits", {
    riskHardLimit = 0.8,
    emergencyTriggerRisk = 0.9
})

-- 设置为正式模式
ccio.invokeApi("fams.setMode", "FORMAL")

print("FAMS中央系统初始化完成")
```

### 节点计算机完整工作流

```lua
local ccio = peripheral.find("gfbs")

-- 连接和注册
ccio.invokeApi("fams.connectCentral")
local nodeInfo = ccio.invokeApi("fams.registerNode", {
    id = "SENSOR_NODE",
    inDim = 4,
    outDim = 4
})

-- 工作循环
while true do
    -- 读取传感器数据
    local sensorData = readSensors()
    
    -- 推送到FAMS
    ccio.invokeApi("fams.pushIn", sensorData)
    
    -- 获取控制指令
    local controlSignals = ccio.invokeApi("fams.pullOut")
    
    -- 执行控制动作
    executeControl(controlSignals)
    
    sleep(0.1)  -- 100ms间隔
end
```

## 错误处理

```lua
local function safeFamsCall(apiName, ...)
    local success, result = pcall(function()
        return ccio.invokeApi(apiName, ...)
    end)
    
    if not success then
        print("FAMS API调用失败:", result)
        return nil
    end
    
    return result
end

-- 使用安全调用
local nodeInfo = safeFamsCall("fams.registerNode", {
    id = "TEST_NODE",
    inDim = 4,
    outDim = 4
})

if nodeInfo then
    print("节点注册成功")
end
```

## 最佳实践

1. **连接稳定性**: 在节点计算机启动时立即连接中央系统
2. **错误恢复**: 实现重试机制处理临时连接问题
3. **数据验证**: 确保输入输出数据的维度和范围正确
4. **资源管理**: 及时断开不再需要的连接
5. **监控日志**: 记录重要的系统状态变化和异常情况

这个教程涵盖了FAMS CC:Tweaked Lua API的主要功能和使用方法，可以帮助用户快速上手并构建稳定的自主管理系统。