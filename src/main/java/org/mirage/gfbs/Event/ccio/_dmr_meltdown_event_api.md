# DmrMeltdown 事件监听保姆级使用教程

## 目录
- [系统概述](#系统概述)
- [快速开始](#快速开始)
- [API 详细说明](#api-详细说明)
- [事件列表](#事件列表)
- [完整示例](#完整示例)
- [最佳实践](#最佳实践)
- [故障排除](#故障排除)

## 系统概述

DmrMeltdown（暗物质反应堆融毁）事件系统，专为 Minecraft CC:Tweaked 环境设计。它采用事件驱动架构，支持CC电脑实时监听融毁事件流程，无需轮询，性能高效。

### 核心概念

- **事件注册**：Java侧预先定义事件ID，Lua侧可订阅
- **事件驱动**：通过`os.pullEvent()`监听，非循环轮询
- **事件数据**：事件可携带附加数据传递给Lua侧
- **自动清理**：计算机断开时自动取消订阅，防止内存泄漏

## 快速开始

### 环境准备

确保你的 Minecraft 世界已安装以下组件：
- CC:Tweaked 计算机
- MirageGFBS 模组
- 至少一台计算机（作为监控终端）

### 基础配置

```lua
-- 获取 CC:I/O 接口
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 注册你需要的事件（事件会在融毁开始时自动注册，也可以手动预先注册）
-- 这里我们预先注册几个关键事件
local eventsToRegister = {
    "dmr_meltdown_start",
    "dmr_redcode_announced",
    "dmr_shutdown_window_open",
    "dmr_meltdown_end"
}

for _, eventId in ipairs(eventsToRegister) do
    local success, result = pcall(function()
        return ccio.invokeApi("event.register", eventId)
    end)
    if success then
        print("已注册事件:", eventId)
    end
end

-- 订阅事件
for _, eventId in ipairs(eventsToRegister) do
    local success, result = pcall(function()
        return ccio.invokeApi("event.subscribe", eventId)
    end)
    if success then
        print("已订阅事件:", eventId)
    end
end

print("融毁事件监控系统已就绪")
```

### 事件监听基础

```lua
-- 事件监听主循环（基于事件驱动，非轮询）
while true do
    -- 等待事件，第一个参数是事件名称"gfbs_event"，后面是事件数据
    local eventName, eventId, arg1, arg2, arg3, ... = os.pullEvent("gfbs_event")
    
    -- 处理事件
    if eventId == "dmr_meltdown_start" then
        print("[警报] 暗物质反应堆融毁事件开始！")
        -- 触发你的警报系统...
    elseif eventId == "dmr_redcode_announced" then
        print("[紧急] 红色代码已宣布！立即撤离！")
        -- 触发紧急撤离程序...
    elseif eventId == "dmr_shutdown_window_open" then
        print("[提示] 关机窗口已开启！")
        -- 启动关机尝试...
    elseif eventId == "dmr_meltdown_end" then
        print("[结束] 融毁事件已结束")
        -- 执行清理...
    end
end
```

## API 详细说明

### 1. 事件管理 API

#### `event.register` - 注册事件

**权限**: 所有计算机

```lua
-- 注册单个事件
local result = ccio.invokeApi("event.register", "dmr_meltdown_start")
```

**参数**:
- `eventId` (字符串): 要注册的事件ID

**返回值**:
- `true`: 注册成功

#### `event.unregister` - 取消注册事件

**权限**: 所有计算机

```lua
local result = ccio.invokeApi("event.unregister", "dmr_meltdown_start")
```

**参数**:
- `eventId` (字符串): 要取消注册的事件ID

**返回值**:
- `true`: 取消成功

#### `event.isRegistered` - 检查事件是否已注册

**权限**: 所有计算机

```lua
local isRegistered = ccio.invokeApi("event.isRegistered", "dmr_meltdown_start")
if isRegistered then
    print("事件已注册")
else
    print("事件未注册")
end
```

**参数**:
- `eventId` (字符串): 要检查的事件ID

**返回值**:
- `boolean`: 事件是否已注册

#### `event.list` - 列出所有已注册事件

**权限**: 所有计算机

```lua
local registeredEvents = ccio.invokeApi("event.list")
print("已注册的事件:")
for _, eventId in ipairs(registeredEvents) do
    print("-", eventId)
end
```

**返回值**:
- `table`: 已注册事件ID的列表

#### `event.subscribe` - 订阅事件

**权限**: 所有计算机

```lua
local result = ccio.invokeApi("event.subscribe", "dmr_meltdown_start")
```

**参数**:
- `eventId` (字符串): 要订阅的事件ID（必须先注册）

**返回值**:
- `true`: 订阅成功

**注意**: 订阅前必须先调用`event.register`注册该事件

#### `event.unsubscribe` - 取消订阅事件

**权限**: 所有计算机

```lua
local result = ccio.invokeApi("event.unsubscribe", "dmr_meltdown_start")
```

**参数**:
- `eventId` (字符串): 要取消订阅的事件ID

**返回值**:
- `true`: 取消成功

#### `event.unsubscribeAll` - 取消所有事件订阅

**权限**: 所有计算机

```lua
local result = ccio.invokeApi("event.unsubscribeAll")
```

**返回值**:
- `true`: 取消成功

#### `event.getSubscriptions` - 获取当前计算机的订阅列表

**权限**: 所有计算机

```lua
local subscriptions = ccio.invokeApi("event.getSubscriptions")
print("当前订阅的事件:")
for _, eventId in ipairs(subscriptions) do
    print("-", eventId)
end
```

**返回值**:
- `table`: 当前计算机订阅的事件ID列表

#### `event.getListenerCount` - 获取指定事件的监听器数量

**权限**: 所有计算机

```lua
local count = ccio.invokeApi("event.getListenerCount", "dmr_meltdown_start")
print("事件监听器数量:", count)
```

**参数**:
- `eventId` (字符串): 要查询的事件ID

**返回值**:
- `number`: 监听器数量

#### `event.stats` - 获取事件系统统计信息

**权限**: 所有计算机

```lua
local stats = ccio.invokeApi("event.stats")
print("事件系统统计:")
print("- 已注册事件数:", stats.registeredEvents)
print("- 总监听器数:", stats.totalListeners)
print("- 已订阅计算机数:", stats.subscribedComputers)
```

**返回值**:
- `table`: 统计信息表，包含：
  - `registeredEvents`: 已注册事件总数
  - `totalListeners`: 总监听器数
  - `subscribedComputers`: 已订阅的计算机数

## 事件列表

### 融毁事件时序表

| 事件ID | 触发时机 | 附加参数 |
|--------|----------|----------|
| `dmr_meltdown_start` | 融毁事件开始 | 无 |
| `dmr_implosion` | 第一次内爆发生 | 无 |
| `dmr_flash_loop_start` | 闪光循环开始 | 无 |
| `dmr_redcode_announced` | 红色代码宣布 | 无 |
| `dmr_implosion_2` | 第二次内爆发生 | 无 |
| `dmr_shutdown_window_open` | 关机窗口开启 | `code` (6位数字字符串) |
| `dmr_shutdown_success` | 关机成功 | 无 |
| `dmr_shutdown_failure` | 关机失败 | 无 |
| `dmr_countdown_start` | 倒计时开始 | 无 |
| `dmr_hex_cracker_triggered` | 十六进制破解器触发 | 无 |
| `dmr_countdown_end` | 倒计时结束 | 无 |
| `dmr_implosion_3` | 第三次内爆发生 | 无 |
| `dmr_p2_start` | P2阶段开始 | 无 |
| `dmr_shelter_gate_opened` | 避难所闸门开启 | 无 |
| `dmr_lockdown_initiated` | 封锁措施启动 | 无 |
| `dmr_gravity_source_detected` | 强引力源检测 | 无 |
| `dmr_meltdown_end` | 融毁事件结束 | 无 |
| `dmr_explosion_start` | 爆炸开始 | 无 |
| `dmr_explosion_main` | 主爆炸发生 | 无 |
| `dmr_facility_restore` | 设施恢复 | 无 |

### 事件详细说明

#### `dmr_meltdown_start`
**触发时机**: 融毁事件启动时立即触发
**说明**: 这是整个融毁流程的开始信号

#### `dmr_implosion`
**触发时机**: 第一次内爆发生时
**说明**: 此时发生第一次DMR压力释放爆炸，设施受到初步破坏

#### `dmr_implosion_2`
**触发时机**: 第二次内爆发生时
**说明**: P1阶段中期的第二次压力释放爆炸，设施破坏加剧

#### `dmr_flash_loop_start`
**触发时机**: 闪光循环启动时
**说明**: 荧光管开始随机闪烁

#### `dmr_redcode_announced`
**触发时机**: 红色代码宣布时
**说明**: 最高紧急级别，要求立即撤离

#### `dmr_shutdown_window_open`
**触发时机**: 关机窗口开启时
**说明**: 这是尝试关闭反应堆的唯一机会窗口
**附加参数**: `code` - 6位十进制关机代码字符串（例如 "123456"）

#### `dmr_shutdown_success`
**触发时机**: 关机代码验证成功且反应堆温度低于3000时
**说明**: 紧急关机成功，反应堆将安全停机，融毁事件被中止

#### `dmr_shutdown_failure`
**触发时机**: 关机代码验证成功但反应堆温度过高（≥3000）或温度获取失败时
**说明**: 关机尝试失败，融毁事件将继续进行，进入P2阶段

#### `dmr_countdown_start`
**触发时机**: 倒计时开始时
**说明**: 关机窗口倒计时启动

#### `dmr_hex_cracker_triggered`
**触发时机**: 十六进制破解器触发时
**说明**: 系统开始尝试破解关机代码

#### `dmr_countdown_end`
**触发时机**: 倒计时结束时
**说明**: 关机窗口已关闭，无法再通过正常方式关机

#### `dmr_implosion_3`
**触发时机**: 第三次内爆发生时
**说明**: 关机窗口关闭后的第三次压力释放爆炸，标志着进入P2阶段前的最后冲击

#### `dmr_p2_start`
**触发时机**: P2阶段开始时
**说明**: 进入第二阶段，情况进一步恶化

#### `dmr_shelter_gate_opened`
**触发时机**: 设施避难所闸门开启时
**说明**: 撤离通道打开

#### `dmr_lockdown_initiated`
**触发时机**: 封锁措施启动时
**说明**: 设施开始封锁，防爆门将在一分钟后关闭

#### `dmr_gravity_source_detected`
**触发时机**: 强引力源检测时
**说明**: 核心腔室出现强引力源

#### `dmr_meltdown_end`
**触发时机**: 融毁事件结束时
**说明**: 整个融毁流程结束，进入后续阶段

#### `dmr_explosion_start`
**触发时机**: 爆炸开始时
**说明**: DMR最终爆炸开始，标志着融毁后的灾难性破坏

#### `dmr_explosion_main`
**触发时机**: 主爆炸发生时
**说明**: 主爆炸冲击波释放，造成最大范围的破坏

#### `dmr_facility_restore`
**触发时机**: 设施恢复时
**说明**: 设施开始恢复运行，荧光管重新亮起，系统重置

## 完整示例

### 基础监控系统

```lua
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 事件配置
local ALL_EVENTS = {
    "dmr_meltdown_start",
    "dmr_implosion",
    "dmr_implosion_2",
    "dmr_flash_loop_start",
    "dmr_redcode_announced",
    "dmr_shutdown_window_open",
    "dmr_shutdown_success",
    "dmr_shutdown_failure",
    "dmr_countdown_start",
    "dmr_hex_cracker_triggered",
    "dmr_countdown_end",
    "dmr_implosion_3",
    "dmr_p2_start",
    "dmr_shelter_gate_opened",
    "dmr_lockdown_initiated",
    "dmr_gravity_source_detected",
    "dmr_meltdown_end",
    "dmr_explosion_start",
    "dmr_explosion_main",
    "dmr_facility_restore"
}

-- 事件消息映射
local EVENT_MESSAGES = {
    ["dmr_meltdown_start"] = "融毁事件开始",
    ["dmr_implosion"] = "第一次内爆发生",
    ["dmr_implosion_2"] = "第二次内爆发生",
    ["dmr_flash_loop_start"] = "闪光循环启动",
    ["dmr_redcode_announced"] = "红色代码已宣布！立即撤离！",
    ["dmr_shutdown_window_open"] = "关机窗口已开启",
    ["dmr_shutdown_success"] = "关机成功！危机解除",
    ["dmr_shutdown_failure"] = "关机失败！继续撤离",
    ["dmr_countdown_start"] = "倒计时开始",
    ["dmr_hex_cracker_triggered"] = "十六进制破解器触发",
    ["dmr_countdown_end"] = "倒计时结束",
    ["dmr_implosion_3"] = "第三次内爆发生",
    ["dmr_p2_start"] = "进入P2阶段",
    ["dmr_shelter_gate_opened"] = "避难所闸门已开启",
    ["dmr_lockdown_initiated"] = "封锁措施启动",
    ["dmr_gravity_source_detected"] = "强引力源检测",
    ["dmr_meltdown_end"] = "融毁事件结束",
    ["dmr_explosion_start"] = "爆炸开始",
    ["dmr_explosion_main"] = "主爆炸发生",
    ["dmr_facility_restore"] = "设施恢复"
}

-- 事件优先级（用于视觉强调）
local EVENT_PRIORITY = {
    ["dmr_redcode_announced"] = "URGENT",
    ["dmr_shutdown_success"] = "SUCCESS",
    ["dmr_shutdown_failure"] = "HIGH",
    ["dmr_implosion"] = "HIGH",
    ["dmr_implosion_2"] = "HIGH",
    ["dmr_implosion_3"] = "HIGH",
    ["dmr_lockdown_initiated"] = "HIGH",
    ["dmr_gravity_source_detected"] = "CRITICAL",
    ["dmr_explosion_start"] = "CRITICAL",
    ["dmr_explosion_main"] = "CRITICAL"
}

-- 安全的 API 调用
local function safeCall(apiName, ...)
    local success, result = pcall(function()
        return ccio.invokeApi(apiName, ...)
    end)
    if not success then
        print("[警告] API调用失败:", apiName, "-", result)
        return nil
    end
    return result
end

-- 初始化
local function init()
    print("正在初始化融毁事件监控系统...")
    
    -- 注册所有事件
    for _, eventId in ipairs(ALL_EVENTS) do
        safeCall("event.register", eventId)
    end
    
    -- 订阅所有事件
    for _, eventId in ipairs(ALL_EVENTS) do
        safeCall("event.subscribe", eventId)
    end
    
    print("初始化完成，等待事件...")
    print("====================================")
end

-- 格式化时间戳
local function formatTime()
    local time = os.time()
    local h, m = math.floor(time / 1000), (time % 1000)
    return string.format("[%02d:%02d]", h, m)
end

-- 处理事件
local function handleEvent(eventId)
    local message = EVENT_MESSAGES[eventId] or ("未知事件: " .. eventId)
    local priority = EVENT_PRIORITY[eventId] or "NORMAL"
    local timeStr = formatTime()
    
    -- 根据优先级显示不同颜色（如果有显示器）
    if peripheral.find("monitor") then
        local monitor = peripheral.find("monitor")
        if priority == "URGENT" then
            monitor.setTextColor(colors.red)
        elseif priority == "HIGH" then
            monitor.setTextColor(colors.orange)
        elseif priority == "CRITICAL" then
            monitor.setTextColor(colors.pink)
        elseif priority == "SUCCESS" then
            monitor.setTextColor(colors.green)
        else
            monitor.setTextColor(colors.white)
        end
    end
    
    print(timeStr, message)
    
    -- 重置颜色
    if peripheral.find("monitor") then
        peripheral.find("monitor").setTextColor(colors.white)
    end
end

-- 主程序
init()

-- 事件监听循环
while true do
    local eventName, eventId = os.pullEvent("gfbs_event")
    handleEvent(eventId)
end
```

### 紧急响应系统

```lua
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")
local monitor = peripheral.find("monitor")
local speaker = peripheral.find("speaker")

local function safeCall(apiName, ...)
    local success, result = pcall(function()
        return ccio.invokeApi(apiName, ...)
    end)
    return success, result
end

-- 警报函数
local function playAlarm()
    if speaker then
        speaker.playNote("bass", 3, 1)
        sleep(0.2)
        speaker.playNote("bass", 3, 1)
        sleep(0.2)
        speaker.playNote("bass", 3, 1)
    end
end

-- 显示大警报
local function showBigAlert(text)
    if monitor then
        monitor.clear()
        monitor.setCursorPos(1, 1)
        monitor.setTextColor(colors.red)
        monitor.write(string.rep("=", monitor.getSize()))
        monitor.setCursorPos(1, 3)
        monitor.write(text)
        monitor.setCursorPos(1, 5)
        monitor.write(string.rep("=", monitor.getSize()))
        monitor.setTextColor(colors.white)
    end
end

-- 初始化
local CRITICAL_EVENTS = {
    "dmr_meltdown_start",
    "dmr_implosion",
    "dmr_implosion_2",
    "dmr_implosion_3",
    "dmr_redcode_announced",
    "dmr_shutdown_success",
    "dmr_shutdown_failure",
    "dmr_lockdown_initiated",
    "dmr_gravity_source_detected",
    "dmr_explosion_start",
    "dmr_explosion_main"
}

for _, eventId in ipairs(CRITICAL_EVENTS) do
    safeCall("event.register", eventId)
    safeCall("event.subscribe", eventId)
end

print("紧急响应系统已启动")

-- 事件监听
while true do
    local eventName, eventId = os.pullEvent("gfbs_event")
    
    if eventId == "dmr_meltdown_start" then
        showBigAlert("融毁开始！")
        playAlarm()
    elseif eventId == "dmr_implosion" then
        showBigAlert("第一次内爆！")
        playAlarm()
    elseif eventId == "dmr_implosion_2" then
        showBigAlert("第二次内爆！")
        playAlarm()
    elseif eventId == "dmr_implosion_3" then
        showBigAlert("第三次内爆！")
        playAlarm()
    elseif eventId == "dmr_redcode_announced" then
        showBigAlert("红色代码！立即撤离！")
        playAlarm()
        playAlarm()
        playAlarm()
    elseif eventId == "dmr_shutdown_success" then
        showBigAlert("关机成功！危机解除")
    elseif eventId == "dmr_shutdown_failure" then
        showBigAlert("关机失败！继续撤离！")
        playAlarm()
    elseif eventId == "dmr_lockdown_initiated" then
        showBigAlert("封锁启动！")
    elseif eventId == "dmr_gravity_source_detected" then
        showBigAlert("强引力源！")
        playAlarm()
    elseif eventId == "dmr_explosion_start" then
        showBigAlert("爆炸开始！")
        playAlarm()
        playAlarm()
    elseif eventId == "dmr_explosion_main" then
        showBigAlert("主爆炸！")
        playAlarm()
        playAlarm()
        playAlarm()
    elseif eventId == "dmr_facility_restore" then
        showBigAlert("设施恢复中...")
    end
end
```

## 最佳实践

### 1. 错误处理

```lua
-- 安全的事件系统调用
local function safeEventCall(apiName, ...)
    local success, result = pcall(function()
        return ccio.invokeApi(apiName, ...)
    end)
    
    if not success then
        print("[事件系统错误]", result)
        return nil
    end
    
    return result
end

-- 使用示例
if safeEventCall("event.register", "dmr_meltdown_start") then
    safeEventCall("event.subscribe", "dmr_meltdown_start")
end
```

### 2. 选择性订阅

```lua
-- 只订阅你真正需要的事件，减少不必要的处理
local IMPORTANT_EVENTS = {
    "dmr_redcode_announced",
    "dmr_shutdown_window_open",
    "dmr_lockdown_initiated"
}

for _, eventId in ipairs(IMPORTANT_EVENTS) do
    safeEventCall("event.register", eventId)
    safeEventCall("event.subscribe", eventId)
end
```

### 3. 优雅清理

```lua
-- 程序退出时清理
local function cleanup()
    print("正在清理事件订阅...")
    safeEventCall("event.unsubscribeAll")
    print("清理完成")
end

-- 捕获中断信号
local function mainLoop()
    while true do
        local eventData = {os.pullEvent()}
        if eventData[1] == "terminate" then
            cleanup()
            break
        elseif eventData[1] == "gfbs_event" then
            -- 处理事件
        end
    end
end

-- 启动
pcall(mainLoop)
cleanup()
```

### 4. 事件去重

```lua
local lastEventTime = {}
local DEBOUNCE_MS = 1000  -- 1秒内重复事件忽略

local function shouldProcess(eventId)
    local now = os.epoch("utc")
    local last = lastEventTime[eventId] or 0
    
    if now - last < DEBOUNCE_MS then
        return false
    end
    
    lastEventTime[eventId] = now
    return true
end

-- 在事件处理中使用
while true do
    local eventName, eventId = os.pullEvent("gfbs_event")
    
    if shouldProcess(eventId) then
        -- 处理事件
    end
end
```

## 故障排除

### 常见问题及解决方案

#### 问题1：收不到事件

**症状**: 订阅了事件但没有收到

**解决方案**:
```lua
-- 检查事件是否已注册
local isRegistered = safeEventCall("event.isRegistered", "dmr_meltdown_start")
if not isRegistered then
    print("事件未注册，正在注册...")
    safeEventCall("event.register", "dmr_meltdown_start")
end

-- 检查是否已订阅
local subscriptions = safeEventCall("event.getSubscriptions")
local found = false
for _, sub in ipairs(subscriptions or {}) do
    if sub == "dmr_meltdown_start" then
        found = true
        break
    end
end

if not found then
    print("未订阅该事件，正在订阅...")
    safeEventCall("event.subscribe", "dmr_meltdown_start")
end

-- 检查是否找到CCIO设备
if not peripheral.find("gfbs") then
    print("未找到 GFBS 设备，请检查连接")
end
```

#### 问题2：事件ID错误

**症状**: 提示"Event not registered"

**解决方案**:
```lua
-- 查看所有可用事件
local registered = safeEventCall("event.list")
print("已注册的事件:")
for _, id in ipairs(registered or {}) do
    print("-", id)
end

-- 确认你的事件ID是否在列表中
local myEventId = "dmr_meltdown_start"
local exists = false
for _, id in ipairs(registered or {}) do
    if id == myEventId then
        exists = true
        break
    end
end

if not exists then
    print("事件ID不存在，请检查拼写")
end
```

#### 问题3：内存使用过高

**症状**: 计算机运行缓慢

**解决方案**:
```lua
-- 只订阅必要的事件
local MINIMAL_EVENTS = {
    "dmr_redcode_announced",
    "dmr_meltdown_end"
}

-- 取消所有订阅
safeEventCall("event.unsubscribeAll")

-- 只订阅必要的
for _, id in ipairs(MINIMAL_EVENTS) do
    safeEventCall("event.register", id)
    safeEventCall("event.subscribe", id)
end

-- 定期清理垃圾
while true do
    local eventData = {os.pullEvent()}
    -- 处理事件...
    
    -- 每100次事件清理一次
    collectgarbage("collect")
end
```

#### 问题4：计算机断开后重连

**症状**: 重启计算机后需要重新订阅

**解决方案**:
```lua
local function setupEvents()
    local events = {
        "dmr_meltdown_start",
        "dmr_redcode_announced"
    }
    
    for _, id in ipairs(events) do
        safeEventCall("event.register", id)
        safeEventCall("event.subscribe", id)
    end
    
    print("事件订阅完成")
end

-- 启动时设置
setupEvents()

-- 监听peripheral_detach/attach事件，重连时重新设置
while true do
    local event, side = os.pullEvent()
    
    if event == "peripheral" and peripheral.getType(side) == "gfbs" then
        print("检测到GFBS设备连接，重新设置事件...")
        sleep(0.5)  -- 等待稳定
        ccio = peripheral.wrap(side)
        setupEvents()
    elseif event == "gfbs_event" then
        -- 处理你的事件...
    end
end
```

### 调试技巧

#### 启用详细日志

```lua
local DEBUG = true

local function debug(msg)
    if DEBUG then
        print("[DEBUG]", os.epoch("utc"), msg)
    end
end

-- 包装事件处理
while true do
    local eventData = {os.pullEvent()}
    
    if eventData[1] == "gfbs_event" then
        debug("收到事件: " .. tostring(eventData[2]))
        -- 处理事件...
    end
end
```

#### 事件统计监控

```lua
local eventCounts = {}

while true do
    local eventName, eventId = os.pullEvent("gfbs_event")
    
    -- 统计
    eventCounts[eventId] = (eventCounts[eventId] or 0) + 1
    
    -- 每10个事件打印统计
    local total = 0
    for _, count in pairs(eventCounts) do
        total = total + count
    end
    
    if total % 10 == 0 then
        print("=== 事件统计 ===")
        for id, count in pairs(eventCounts) do
            print(id .. ":", count)
        end
        print("==============")
    end
end
```

## 总结

DmrMeltdown 事件系统提供了一个高效、事件驱动的方式来监控暗物质反应堆融毁流程。通过本教程，您可以：

1. **快速上手**：掌握基本的事件注册和订阅
2. **深入使用**：了解所有事件API和事件列表
3. **实战应用**：参考完整的监控和响应系统示例
4. **优化维护**：遵循最佳实践确保系统稳定
5. **故障排除**：快速解决常见问题

关键点：
- 使用`os.pullEvent("gfbs_event")`而不是轮询
- 只订阅真正需要的事件
- 正确处理错误和清理
- 利用事件优先级进行视觉/听觉反馈

希望本教程能帮助您在 CC:Tweaked 环境中成功构建融毁事件监控系统！

## 关机请求 API

当 `dmr_shutdown_window_open` 事件触发时，它会携带一个6位十进制关机代码。您可以通过以下API发送关机请求。

### `dmr.shutdownRequest` - 发送关机请求

**权限**: 所有计算机

```lua
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 发送关机代码（字符串或数字格式）
local result = ccio.invokeApi("dmr.shutdownRequest", "123456")
-- 或
local result = ccio.invokeApi("dmr.shutdownRequest", 123456)

if result.success then
    print("关机成功！")
    print(result.message)
else
    print("关机失败：" .. result.message)
end
```

**参数**:
- `code` (字符串或数字): 6位关机代码

**返回值**:
- `table`: 包含以下字段：
  - `success` (布尔值): 是否成功
  - `message` (字符串): 结果消息

### `dmr.getCrackedDigits` - 获取已破解数字

**权限**: 所有计算机

```lua
local result = ccio.invokeApi("dmr.getCrackedDigits")
print("已破解数量:", result.crackedCount)
print("是否完全破解:", result.fullyCracked)
print("数字:", table.unpack(result.digits))
```

**返回值**:
- `table`: 包含以下字段：
  - `digits` (表): 包含6个元素的数组，-1表示未破解，0-9表示已破解的数字
  - `crackedCount` (数字): 已破解的数字数量
  - `fullyCracked` (布尔值): 是否完全破解

### `dmr.getCrackedDisplay` - 获取已破解显示字符串

**权限**: 所有计算机

```lua
local result = ccio.invokeApi("dmr.getCrackedDisplay")
print("显示:", result.display)  -- 例如 "12--56"
print("已破解:", result.crackedCount)
```

**返回值**:
- `table`: 包含以下字段：
  - `display` (字符串): 显示字符串，未破解的位置显示为 "-"
  - `crackedCount` (数字): 已破解的数字数量
  - `fullyCracked` (布尔值): 是否完全破解

### `dmr.hasActiveCode` - 检查是否存在活动关机代码

**权限**: 所有计算机

```lua
local hasCode = ccio.invokeApi("dmr.hasActiveCode")
if hasCode then
    print("关机窗口已开启，可以尝试关机")
else
    print("关机窗口未开启")
end
```

**返回值**:
- `布尔值`: 是否存在活动的关机代码

### 完整关机示例

```lua
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 注册事件
ccio.invokeApi("event.register", "dmr_shutdown_window_open")
ccio.invokeApi("event.subscribe", "dmr_shutdown_window_open")

print("等待关机窗口...")

while true do
    local eventName, eventId, code = os.pullEvent("gfbs_event")
    
    if eventId == "dmr_shutdown_window_open" then
        print("关机窗口已开启！")
        print("关机代码:", code)
        
        -- 尝试关机
        local result = ccio.invokeApi("dmr.shutdownRequest", code)
        
        if result.success then
            print("关机成功！")
            break
        else
            print("关机失败:", result.message)
        end
    end
end
```

### 配合破解器使用

```lua
local ccio = peripheral.find("gfbs") or peripheral.wrap("right")

-- 注册事件
ccio.invokeApi("event.register", "dmr_shutdown_window_open")
ccio.invokeApi("event.register", "dmr_hex_cracker_triggered")
ccio.invokeApi("event.subscribe", "dmr_shutdown_window_open")
ccio.invokeApi("event.subscribe", "dmr_hex_cracker_triggered")

local shutdownCode = nil

while true do
    local eventName, eventId, arg1 = os.pullEvent("gfbs_event")
    
    if eventId == "dmr_shutdown_window_open" then
        shutdownCode = arg1
        print("关机窗口已开启，代码:", shutdownCode)
        
    elseif eventId == "dmr_hex_cracker_triggered" then
        -- 破解器已启动，定期检查进度
        local cracked = ccio.invokeApi("dmr.getCrackedDisplay")
        print("破解进度:", cracked.display, "已破解", cracked.crackedCount, "位")
        
        if cracked.fullyCracked then
            print("完全破解！代码是:", shutdownCode)
            local result = ccio.invokeApi("dmr.shutdownRequest", shutdownCode)
            if result.success then
                print("关机成功！")
                break
            end
        end
    end
end
```

- 本文档由AI编写，仅供参考，不保证准确性
