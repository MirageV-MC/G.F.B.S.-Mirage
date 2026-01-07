# FAMS API 保姆级使用教程

## 目录
- [系统概述](#系统概述)
- [快速开始](#快速开始)
- [API 详细说明](#api-详细说明)
- [完整示例](#完整示例)
- [最佳实践](#最佳实践)
- [故障排除](#故障排除)

## 系统概述

FAMS（Facility Automatic Management System）设施全自动管理系统，专为 Minecraft CC:Tweaked 环境设计。它采用中央-节点架构，支持多节点协同工作，具备智能决策、安全控制和记忆系统等功能。

### 核心概念

- **中央计算机**：系统的大脑，负责协调所有节点，进行全局决策
- **节点计算机**：连接到中央系统的终端设备，负责数据采集和执行控制
- **系统模式**：支持四种运行模式（休眠、部分自动、正式、紧急）
- **记忆系统**：包含短期记忆和长期记忆，用于状态记录和回放

## 快速开始

### 环境准备

确保你的 Minecraft 世界已安装以下组件：
- CC:Tweaked 计算机
- MirageGFBS 模组
- 至少两台计算机（一台作为中央，一台作为节点）

### 中央计算机配置

```lua
-- 获取 CC:I/O 接口
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 初始化中央系统
local result = ccio.invokeApi("fams.setCentral", {
    stateDim = 16,        -- 状态维度
    actionDim = 16,       -- 动作维度
    maxShells = 64,       -- 最大外壳数
    maxNodes = 32,        -- 最大节点数
    tickIntervalMs = 100  -- 自动步进间隔（毫秒）
})

print("中央系统初始化结果:", result)
```

### 节点计算机配置

```lua
-- 获取 CC:I/O 接口
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 连接到中央系统
local connectResult = ccio.invokeApi("fams.connectCentral")
print("连接结果:", connectResult)

-- 注册节点
local nodeInfo = ccio.invokeApi("fams.registerNode", {
    id = "MY_NODE",  -- 节点唯一标识
    inDim = 8,       -- 输入维度
    outDim = 8       -- 输出维度
})

print("节点注册信息:", textutils.serialize(nodeInfo))
```

## API 详细说明

### 1. 中央管理 API

#### `fams.setCentral` - 设置中央系统

**权限**: 仅中央计算机

```lua
local config = {
    stateDim = 16,        -- 状态向量维度 (1-4096)
    actionDim = 16,       -- 动作向量维度 (1-4096)
    maxShells = 64,       -- 最大外壳数量 (1-4096)
    maxNodes = 32,        -- 最大节点数量 (1-4096)
    tickIntervalMs = 100  -- 自动步进间隔 (0-600000ms, 0=禁用自动)
}

local result = ccio.invokeApi("fams.setCentral", config)
```

#### `fams.clearCentral` - 清除中央系统

**权限**: 仅中央计算机

```lua
local result = ccio.invokeApi("fams.clearCentral")
```

### 2. 连接管理 API

#### `fams.connectCentral` - 连接到中央系统

**权限**: 节点计算机

```lua
-- 连接到默认中央
local result = ccio.invokeApi("fams.connectCentral")

-- 连接到指定中央ID
local result = ccio.invokeApi("fams.connectCentral", 123)
```

#### `fams.disconnect` - 断开连接

**权限**: 节点计算机

```lua
local result = ccio.invokeApi("fams.disconnect")
```

### 3. 节点管理 API

#### `fams.registerNode` - 注册节点

**权限**: 已连接的节点计算机

```lua
local nodeSpec = {
    id = "SENSOR_NODE",  -- 节点ID（字符串，唯一）
    inDim = 4,           -- 输入数据维度
    outDim = 4           -- 输出数据维度
}

local nodeInfo = ccio.invokeApi("fams.registerNode", nodeSpec)
```

返回信息：
```lua
{
    computerId = 123,    -- 计算机ID
    id = "SENSOR_NODE",  -- 节点ID
    inDim = 4,          -- 输入维度
    outDim = 4          -- 输出维度
}
```

#### `fams.setNodeAutoEnabled` - 设置节点自动启用状态

**权限**: 中央计算机

```lua
-- 启用节点自动处理
local result = ccio.invokeApi("fams.setNodeAutoEnabled", {"SENSOR_NODE", true})

-- 禁用节点自动处理
local result = ccio.invokeApi("fams.setNodeAutoEnabled", {"SENSOR_NODE", false})
```

### 4. 数据 I/O API

#### `fams.pushIn` - 推送输入数据

**权限**: 已注册的节点计算机

```lua
-- 推送传感器数据
local sensorData = {0.2, 0.7, 0.0, 0.5}
local result = ccio.invokeApi("fams.pushIn", sensorData)
```

#### `fams.pullOut` - 拉取输出数据

**权限**: 已注册的节点计算机

```lua
-- 获取控制指令
local controlSignals = ccio.invokeApi("fams.pullOut")
print("控制信号:", textutils.serialize(controlSignals))
```

### 5. 系统控制 API

#### `fams.step` - 手动执行一步

**权限**: 中央计算机

```lua
local stepResult = ccio.invokeApi("fams.step")
```

#### `fams.setMode` - 设置系统模式

**权限**: 中央计算机

```lua
-- 可用模式：SLEEP, PARTIAL_AUTO, FORMAL, EMERGENCY
local result = ccio.invokeApi("fams.setMode", "FORMAL")
```

#### `fams.getMode` - 获取当前模式

**权限**: 所有计算机

```lua
local currentMode = ccio.invokeApi("fams.getMode")
print("当前模式:", currentMode)
```

#### `fams.stats` - 获取系统统计

**权限**: 所有计算机

```lua
local stats = ccio.invokeApi("fams.stats")
print("系统统计:", textutils.serialize(stats))
```

### 6. 目标管理 API

#### `fams.setGoal` - 设置系统目标

**权限**: 中央计算机

```lua
local goalConfig = {
    target = {0.5, 0.3, 0.7},  -- 目标状态值
    weights = {1.0, 0.8, 0.5}   -- 各维度权重
}

local result = ccio.invokeApi("fams.setGoal", goalConfig)
```

#### `fams.getGoal` - 获取当前目标

**权限**: 中央计算机

```lua
local currentGoal = ccio.invokeApi("fams.getGoal")
print("当前目标:", textutils.serialize(currentGoal))
```

### 7. 安全参数 API

#### `fams.setSafetyLimits` - 设置安全限制

**权限**: 中央计算机

```lua
local safetyConfig = {
    riskHardLimit = 0.85,        -- 风险硬限制
    emergencyTriggerRisk = 0.92, -- 紧急触发风险
    uMin = -1.0,                 -- 动作下限
    uMax = 1.0,                  -- 动作上限
    stateThresholds = {
        soft = 0.8,              -- 状态软阈值
        hard = 1.0               -- 状态硬阈值
    }
}

local result = ccio.invokeApi("fams.setSafetyLimits", safetyConfig)
```

#### `fams.getSafetyLimits` - 获取安全配置

**权限**: 中央计算机

```lua
local safetyConfig = ccio.invokeApi("fams.getSafetyLimits")
print("安全配置:", textutils.serialize(safetyConfig))
```

### 8. 节点信息 API

#### `fams.getNodeInfo` - 获取节点信息

**权限**: 所有计算机

```lua
local nodeInfo = ccio.invokeApi("fams.getNodeInfo", "SENSOR_NODE")
print("节点信息:", textutils.serialize(nodeInfo))
```

#### `fams.listNodes` - 列出所有节点

**权限**: 中央计算机和已连接节点

```lua
-- 基本列表
local nodesList = ccio.invokeApi("fams.listNodes")

-- 包含详细信息
local detailedList = ccio.invokeApi("fams.listNodes", {
    includeDetails = true,
    includeValues = true
})

print("节点列表:", textutils.serialize(detailedList))
```

### 9. 模式配置 API

#### `fams.setModeConfig` - 设置模式参数

**权限**: 中央计算机

```lua
local modeConfig = {
    "FORMAL",  -- 模式名称
    {
        learningEnabled = true,      -- 启用学习
        learningRate = 0.01,         -- 学习率
        riskHardLimit = 0.85,        -- 风险硬限制
        emergencyTriggerRisk = 0.92, -- 紧急触发风险
        monitorIntervalMs = 100,     -- 监测间隔
        enableExternalMonitor = true, -- 启用外部监测
        allowActionDispatch = true   -- 允许动作分发
    }
}

local result = ccio.invokeApi("fams.setModeConfig", modeConfig)
```

#### `fams.getModeConfig` - 获取模式配置

**权限**: 中央计算机

```lua
local formalConfig = ccio.invokeApi("fams.getModeConfig", "FORMAL")
print("正式模式配置:", textutils.serialize(formalConfig))
```

### 10. 记忆系统 API

#### `fams.memoryStats` - 获取记忆统计

**权限**: 中央计算机

```lua
local memoryStats = ccio.invokeApi("fams.memoryStats")
print("记忆统计:", textutils.serialize(memoryStats))
```

#### `fams.memoryConfig` - 获取记忆配置

**权限**: 中央计算机

```lua
local memoryConfig = ccio.invokeApi("fams.memoryConfig")
print("记忆配置:", textutils.serialize(memoryConfig))
```

#### `fams.setMemoryConfig` - 设置记忆配置

**权限**: 中央计算机

```lua
local newConfig = {
    shortTermCapacity = 4096,          -- 短期记忆容量
    shortTermTtlMs = 600000,           -- 短期记忆TTL（毫秒）
    longTermEnabled = true,            -- 启用长期记忆
    longTermQueueCapacity = 10000,     -- 长期记忆队列容量
    longTermFlushIntervalMs = 5000,    -- 长期记忆刷新间隔
    longTermSegmentMaxBytes = 1048576, -- 长期记忆段最大字节
    longTermIndexStride = 100,         -- 长期记忆索引步长
    longTermRetentionDays = 30         -- 长期记忆保留天数
}

local result = ccio.invokeApi("fams.setMemoryConfig", newConfig)
```

### 11. 学习控制 API

#### `fams.setLearningRate` - 设置学习率

**权限**: 中央计算机

```lua
local result = ccio.invokeApi("fams.setLearningRate", 0.01)
```

#### `fams.setLearningEnabled` - 设置学习启用状态

**权限**: 中央计算机

```lua
local result = ccio.invokeApi("fams.setLearningEnabled", true)
```

### 12. 状态监控 API

#### `fams.getState` - 获取系统状态

**权限**: 中央计算机

```lua
local systemState = ccio.invokeApi("fams.getState")
print("系统状态:", textutils.serialize(systemState))
```

#### `fams.getRisk` - 获取当前风险

**权限**: 所有计算机

```lua
local currentRisk = ccio.invokeApi("fams.getRisk")
print("当前风险:", currentRisk)
```

#### `fams.getLastLoss` - 获取最后损失

**权限**: 所有计算机

```lua
local lastLoss = ccio.invokeApi("fams.getLastLoss")
print("最后损失:", lastLoss)
```

### 13. 系统控制 API

#### `fams.shutdown` - 优雅关闭系统

**权限**: 中央计算机

```lua
-- 正常关闭
local result = ccio.invokeApi("fams.shutdown")

-- 关闭并清除配置
local result = ccio.invokeApi("fams.shutdown", true)
```

#### `fams.save` - 保存系统状态

**权限**: 中央计算机

```lua
local result = ccio.invokeApi("fams.save")
```

#### `fams.load` - 加载系统状态

**权限**: 中央计算机

```lua
local result = ccio.invokeApi("fams.load")
```

## 完整示例

### 工业自动化系统

#### 中央监控系统

```lua
-- 工业监控中央
local ccio = peripheral.find("gfbs")

ccio.invokeApi("fams.setCentral", {
    stateDim = 12,
    actionDim = 8,
    maxNodes = 20,
    tickIntervalMs = 50  -- 工业系统需要更快响应
})

-- 工业安全配置（更严格）
ccio.invokeApi("fams.setSafetyLimits", {
    riskHardLimit = 0.7,
    emergencyTriggerRisk = 0.8,
    uMin = -1.0,
    uMax = 1.0
})

-- 配置紧急模式参数
ccio.invokeApi("fams.setModeConfig", {
    "EMERGENCY",
    {
        learningEnabled = false,
        riskHardLimit = 0.6,
        allowActionDispatch = false  -- 紧急时停止动作分发
    }
})

print("工业监控系统已启动")

-- 监控循环
while true do
    local stats = ccio.invokeApi("fams.stats")
    local risk = ccio.invokeApi("fams.getRisk")
    
    if risk > 0.6 then
        print("警告：系统风险升高！当前风险:", risk)
    end
    
    sleep(5)  -- 5秒监控间隔
end
```

## 最佳实践

### 1. 错误处理

```lua
-- 安全的 API 调用函数
function safeFamsCall(apiName, ...)
    local success, result = pcall(function()
        return ccio.invokeApi(apiName, ...)
    end)
    
    if not success then
        print("FAMS API调用失败:", result)
        return nil
    end
    
    return result
end

-- 使用示例
local nodeInfo = safeFamsCall("fams.registerNode", {
    id = "TEST_NODE",
    inDim = 4,
    outDim = 4
})

if nodeInfo then
    print("节点注册成功")
else
    print("节点注册失败，尝试重新连接...")
    safeFamsCall("fams.connectCentral")
end
```

### 2. 连接稳定性

```lua
-- 自动重连机制
function ensureConnected()
    local maxRetries = 3
    for i = 1, maxRetries do
        local result = safeFamsCall("fams.connectCentral")
        if result then
            return true
        end
        print("连接尝试", i, "失败，等待重试...")
        sleep(2)  -- 等待2秒
    end
    return false
end

-- 在主循环中使用
while true do
    if not ensureConnected() then
        print("无法连接到中央系统，退出程序")
        break
    end
    
    -- 正常业务逻辑
    -- ...
    
    sleep(1)
end
```

### 3. 数据验证

```lua
-- 输入数据验证
function validateInputData(data, expectedDim)
    if type(data) ~= "table" then
        return false, "输入数据必须是表"
    end
    
    if #data ~= expectedDim then
        return false, "数据维度不匹配，期望:" .. expectedDim .. "，实际:" .. #data
    end
    
    for i, value in ipairs(data) do
        if type(value) ~= "number" then
            return false, "数据元素必须是数字，索引:" .. i
        end
        if value < -10 or value > 10 then  -- 合理范围检查
            return false, "数据值超出范围，索引:" .. i .. "，值:" .. value
        end
    end
    
    return true
end

-- 使用示例
local sensorData = readSensors()
local valid, errorMsg = validateInputData(sensorData, 5)

if valid then
    safeFamsCall("fams.pushIn", sensorData)
else
    print("数据验证失败:", errorMsg)
end
```

### 4. 性能优化

```lua
-- 批量数据处理
function processBatchData(batchSize)
    local batch = {}
    
    for i = 1, batchSize do
        local data = readSensor()
        table.insert(batch, data)
        
        if i % 10 == 0 then  -- 每10条数据推送一次
            safeFamsCall("fams.pushIn", batch)
            batch = {}  -- 清空批次
        end
    end
    
    -- 处理剩余数据
    if #batch > 0 then
        safeFamsCall("fams.pushIn", batch)
    end
end

-- 内存管理
function cleanupMemory()
    -- 定期清理不必要的变量
    collectgarbage("collect")
    
    -- 监控内存使用
    local usedMemory = math.floor(collectgarbage("count") / 1024)
    if usedMemory > 100 then  -- 超过100KB
        print("内存使用过高:", usedMemory, "KB")
        collectgarbage("collect")
    end
end
```

## 故障排除

### 常见问题及解决方案

#### 问题1：无法连接到中央系统

**症状**: `fams.connectCentral` 调用失败

**解决方案**:
```lua
-- 检查中央系统是否已启动
local centralStatus = safeFamsCall("fams.getMode")
if not centralStatus then
    print("中央系统未启动，请先启动中央计算机")
    return
end

-- 检查网络连接
if not peripheral.find("gfbs") then
    print("未找到 GFBS 外围设备，检查连接")
    return
end

-- 尝试指定中央ID
local nodesList = safeFamsCall("fams.listNodes")
if nodesList then
    for _, node in ipairs(nodesList.nodes) do
        if node.isCentral then
            safeFamsCall("fams.connectCentral", node.computerId)
            break
        end
    end
end
```

#### 问题2：节点注册失败

**症状**: `fams.registerNode` 返回错误

**解决方案**:
```lua
-- 检查节点ID是否唯一
local existingNodes = safeFamsCall("fams.listNodes")
if existingNodes then
    for _, node in ipairs(existingNodes.nodes) do
        if node.id == "MY_NODE" then
            print("节点ID已存在，请使用不同的ID")
            return
        end
    end
end

-- 检查维度设置是否合理
if inDim > 100 or outDim > 100 then
    print("输入输出维度过大，建议小于100")
    return
end

-- 重新连接后重试
safeFamsCall("fams.disconnect")
sleep(1)
safeFamsCall("fams.connectCentral")
```

#### 问题3：系统风险过高

**症状**: `fams.getRisk` 返回高风险值

**解决方案**:
```lua
local risk = safeFamsCall("fams.getRisk")
if risk and risk > 0.8 then
    print("系统风险过高:", risk)
    
    -- 切换到安全模式
    safeFamsCall("fams.setMode", "EMERGENCY")
    
    -- 检查节点状态
    local nodes = safeFamsCall("fams.listNodes", {includeValues = true})
    if nodes then
        for _, node in ipairs(nodes.nodes) do
            if node.currentOutput then
                for i, value in ipairs(node.currentOutput) do
                    if math.abs(value) > 5 then  -- 输出值异常
                        print("节点", node.id, "输出异常:", value)
                    end
                end
            end
        end
    end
    
    -- 调整安全参数
    safeFamsCall("fams.setSafetyLimits", {
        riskHardLimit = 0.7,
        emergencyTriggerRisk = 0.8
    })
end
```

#### 问题4：内存使用过高

**症状**: 系统运行缓慢，内存不足

**解决方案**:
```lua
-- 检查记忆系统配置
local memoryConfig = safeFamsCall("fams.memoryConfig")
if memoryConfig then
    -- 调整短期记忆容量
    if memoryConfig.shortTermCapacity > 10000 then
        safeFamsCall("fams.setMemoryConfig", {
            shortTermCapacity = 5000
        })
    end
    
    -- 调整长期记忆保留时间
    if memoryConfig.longTermRetentionDays > 30 then
        safeFamsCall("fams.setMemoryConfig", {
            longTermRetentionDays = 7
        })
    end
end

-- 手动清理内存
collectgarbage("collect")
```

### 调试技巧

#### 启用详细日志

```lua
-- 调试模式开关
local DEBUG_MODE = true

function debugLog(message)
    if DEBUG_MODE then
        print("[DEBUG]", os.epoch("utc"), message)
    end
end

-- 在关键操作处添加日志
debugLog("开始传感器数据读取")
local data = readSensors()
debugLog("传感器数据:" .. textutils.serialize(data))

local result = safeFamsCall("fams.pushIn", data)
if result then
    debugLog("数据推送成功")
else
    debugLog("数据推送失败")
end
```

#### 性能监控

```lua
-- 性能监控函数
function monitorPerformance()
    local startTime = os.epoch("utc")
    
    -- 执行操作
    local result = safeFamsCall("fams.pullOut")
    
    local endTime = os.epoch("utc")
    local duration = endTime - startTime
    
    if duration > 100 then  -- 超过100ms
        print("性能警告：操作耗时", duration, "ms")
    end
    
    return result
end
```

## 总结

FAMS API 提供了一个强大而灵活的设施全自动管理系统框架，适用于各种复杂的自动化场景。通过本教程，您可以：

1. **快速上手**：掌握基本的中央-节点架构配置
2. **深入使用**：了解所有API的详细功能和参数
3. **实战应用**：参考完整的示例代码构建实际系统
4. **优化维护**：遵循最佳实践确保系统稳定运行
5. **故障排除**：快速解决常见问题

希望本教程能帮助您在 CC:Tweaked 环境中成功构建智能设施管理系统！如有更多问题，请参考官方文档或社区讨论。

- 本文档由AI编写，仅供参考，不保证准确性
