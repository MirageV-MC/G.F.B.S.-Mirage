/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.mirage.gfbs.Phenomenon.ScriptSystem;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import org.mirage.gfbs.MirageGFBS;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptExecutor {
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(ScriptExecutor::shutdownAsyncExecutor));
    }

    // 线程池管理异步任务
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool();
    private static final Map<String, Future<?>> ASYNC_TASKS = new ConcurrentHashMap<>();

    private static final Pattern ASYNC_CALL_PATTERN = Pattern.compile("^async_call\\s+(\\w+)\\s*(?:\\(([^)]*)\\))?\\s*$");
    private static final Pattern TASK_SPAWN_PATTERN = Pattern.compile("^task\\.spawn\\s+(\\w+)\\s*(?:\\(([^)]*)\\))?\\s*$");
    private static final Pattern TASK_CANCEL_PATTERN = Pattern.compile("^task\\.cancel\\s+(\\w+)\\s*$");
    private static final Pattern TASK_WAIT_PATTERN = Pattern.compile("^task\\.wait\\s+(\\w+)\\s*$");
    private static final Pattern TASK_DELAY_PATTERN = Pattern.compile("^task\\.delay\\s+(\\d+)\\s*$");

    // 模式定义
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
    private static final Pattern SET_COMMAND_PATTERN = Pattern.compile("^set\\s+(\\w+)\\s+(.+)$");
    private static final Pattern IF_COMMAND_PATTERN = Pattern.compile("^if\\s+(.+)$");
    private static final Pattern LOOP_COMMAND_PATTERN = Pattern.compile("^loop\\s+(\\d+)\\s*$");
    private static final Pattern WHILE_COMMAND_PATTERN = Pattern.compile("^while\\s+(.+)$");
    private static final Pattern DELAY_COMMAND_PATTERN = Pattern.compile("^delay\\s+(\\d+)\\s*$");
    private static final Pattern FUNCTION_DEF_PATTERN = Pattern.compile("^function\\s+(\\w+)\\s*(?:\\(([^)]*)\\))?\\s*$");
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile("^call\\s+(\\w+)\\s*(?:\\(([^)]*)\\))?\\s*$");
    private static final Pattern RETURN_COMMAND_PATTERN = Pattern.compile("^return\\s*(.+)?$");
    private static final Pattern MATH_EXPRESSION_PATTERN = Pattern.compile("^calc\\s+(.+)$");
    private static final Pattern ARRAY_COMMAND_PATTERN = Pattern.compile("^array\\s+(\\w+)\\s*(?:\\[(\\d+)\\])?\\s*$");
    private static final Pattern ARRAY_SET_PATTERN = Pattern.compile("^array_set\\s+(\\w+)\\[(\\d+)\\]\\s+(.+)$");
    private static final Pattern ARRAY_GET_PATTERN = Pattern.compile("^array_get\\s+(\\w+)\\[(\\d+)\\]\\s+(\\w+)$");

    // 全局变量空间
    private static final Map<String, String> GLOBAL_VARIABLES = new ConcurrentHashMap<>();

    // 实例变量
    private final Map<String, String> localVariables = new HashMap<>();
    private final Map<String, ScriptFunction> functions = new HashMap<>();
    private final Map<String, String[]> arrays = new HashMap<>();
    private final Stack<Boolean> conditionStack = new Stack<>();
    private final Stack<LoopContext> loopStack = new Stack<>();
    private final Stack<Integer> delayStack = new Stack<>();
    private final Stack<FunctionContext> callStack = new Stack<>();

    // 虚拟机配置
    private final boolean debugMode;
    private final int maxExecutionTime; // 最大执行时间(秒)

    public ScriptExecutor() {
        this(false, 30);
    }

    public ScriptExecutor(boolean debugMode, int maxExecutionTime) {
        this.debugMode = debugMode;
        this.maxExecutionTime = maxExecutionTime;
    }

    public static boolean executeScript(String scriptId, CommandSourceStack source) {
        ScriptExecutor executor = new ScriptExecutor();
        return executor.execute(scriptId, source);
    }

    public boolean execute(String scriptId, CommandSourceStack source) {
        return execute(scriptId, source, Collections.emptyMap());
    }

    public boolean execute(String scriptId, CommandSourceStack source, Map<String, String> initialVars) {
        localVariables.clear();
        functions.clear();
        arrays.clear();
        conditionStack.clear();
        loopStack.clear();
        delayStack.clear();
        callStack.clear();

        localVariables.putAll(initialVars);

        Path scriptPath = MirageGFBS.SCRIPTS_DIR.resolve(scriptId + ".txt");

        if (!Files.exists(scriptPath)) {
            source.sendFailure(Component.literal("脚本不存在: " + scriptId));
            return false;
        }

        try {
            List<String> lines = Files.readAllLines(scriptPath, StandardCharsets.UTF_8);

            if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF")) {
                lines.set(0, lines.get(0).substring(1));
            }

            parseScriptStructure(lines);

            return executeFunction("main", source, new String[0]);
        } catch (IOException e) {
            source.sendFailure(Component.literal("读取脚本失败: " + e.getMessage()));
            return false;
        } catch (Exception e) {
            source.sendFailure(Component.literal("脚本执行错误: " + e.getMessage()));
            if (debugMode) {
                e.printStackTrace();
            }
            return false;
        }
    }

    private void parseScriptStructure(List<String> lines) {
        String currentFunction = null;
        List<String> currentFunctionLines = null;
        List<String> currentParams = null;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            Matcher functionMatcher = FUNCTION_DEF_PATTERN.matcher(trimmed);
            if (functionMatcher.matches()) {
                if (currentFunction != null) {
                    functions.put(currentFunction, new ScriptFunction(currentFunction, currentParams, currentFunctionLines));
                }

                currentFunction = functionMatcher.group(1);
                String paramsStr = functionMatcher.group(2);
                currentParams = paramsStr != null ?
                        Arrays.asList(paramsStr.split("\\s*,\\s*")) :
                        Collections.emptyList();
                currentFunctionLines = new ArrayList<>();
                continue;
            }

            if (currentFunction != null) {
                currentFunctionLines.add(trimmed);
            }
        }

        if (currentFunction != null) {
            functions.put(currentFunction, new ScriptFunction(currentFunction, currentParams, currentFunctionLines));
        }
    }

    private boolean executeFunction(String functionName, CommandSourceStack source, String[] args) {
        ScriptFunction function = functions.get(functionName);
        if (function == null) {
            source.sendFailure(Component.literal("函数未定义: " + functionName));
            return false;
        }

        if (args.length != function.getParams().size()) {
            source.sendFailure(Component.literal("函数 " + functionName + " 期望 " +
                    function.getParams().size() + " 个参数，但得到了 " + args.length));
            return false;
        }

        FunctionContext context = new FunctionContext(
                functionName,
                function.getLines(),
                0,
                new HashMap<>()
        );

        for (int i = 0; i < function.getParams().size(); i++) {
            context.localVars.put(function.getParams().get(i), args[i]);
        }

        callStack.push(context);

        long startTime = System.currentTimeMillis();
        boolean success = true;

        while (context.lineIndex < context.lines.size()) {
            if (System.currentTimeMillis() - startTime > maxExecutionTime * 1000) {
                source.sendFailure(Component.literal("脚本执行超时"));
                success = false;
                break;
            }

            String line = context.lines.get(context.lineIndex);
            context.lineIndex++;

            if (!processLine(line, source, context)) {
                success = false;
                break;
            }

            if (context.returnValue != null) {
                break;
            }
        }

        callStack.pop();
        return success;
    }

    private boolean processLine(String line, CommandSourceStack source, FunctionContext context) {
        String trimmedLine = line.trim();
        if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
            return true;
        }

        String processedLine = replaceVariables(line, context);

        if (processedLine.isEmpty() || processedLine.startsWith("#")) {
            return true;
        }

        if (!delayStack.isEmpty()) {
            int delay = delayStack.pop();
            if (delay > 0) {
                delayStack.push(delay - 1);
                context.lineIndex--;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return true;
            }
        }

        if (!loopStack.isEmpty()) {
            LoopContext loop = loopStack.peek();

            if (loop.collecting) {
                if (processedLine.equals("endloop")) {
                    loop.collecting = false;
                } else {
                    loop.lines.add(processedLine);
                }
                return true;
            } else {
                if (loop.iterations > 0 || loop.isWhileLoop) {
                    if (loop.currentLine < loop.lines.size()) {
                        String loopLine = loop.lines.get(loop.currentLine);
                        loop.currentLine++;

                        if (!processLine(loopLine, source, context)) {
                            source.sendFailure(Component.literal("循环内执行命令失败: " + loopLine));
                            return false;
                        }

                        context.lineIndex--;
                        return true;
                    } else {
                        loop.currentLine = 0;

                        if (loop.isWhileLoop) {
                            boolean condition = evaluateCondition(loop.condition, context);
                            if (condition) {
                                return true;
                            } else {
                                loopStack.pop();
                            }
                        } else {
                            loop.iterations--;
                            if (loop.iterations > 0) {
                                return true;
                            } else {
                                loopStack.pop();
                            }
                        }
                    }
                }
            }
        }

        if (!conditionStack.isEmpty() && !conditionStack.peek()) {
            if (processedLine.equals("else")) {
                conditionStack.pop();
                conditionStack.push(true);
            } else if (processedLine.equals("endif")) {
                conditionStack.pop();
            }
            return true;
        }

        Matcher setMatcher = SET_COMMAND_PATTERN.matcher(processedLine);
        if (setMatcher.matches()) {
            String varName = setMatcher.group(1);
            String varValue = setMatcher.group(2);
            setVariable(varName, varValue, context, source);
            return true;
        }

        Matcher ifMatcher = IF_COMMAND_PATTERN.matcher(processedLine);
        if (ifMatcher.matches()) {
            String condition = ifMatcher.group(1);
            boolean result = evaluateCondition(condition, context);
            conditionStack.push(result);
            return true;
        }

        if (processedLine.equals("else")) {
            if (!conditionStack.isEmpty()) {
                boolean previousCondition = conditionStack.pop();
                conditionStack.push(!previousCondition);
            }
            return true;
        }

        if (processedLine.equals("endif")) {
            if (!conditionStack.isEmpty()) {
                conditionStack.pop();
            }
            return true;
        }

        Matcher loopMatcher = LOOP_COMMAND_PATTERN.matcher(processedLine);
        if (loopMatcher.matches()) {
            int count = Integer.parseInt(loopMatcher.group(1));
            loopStack.push(new LoopContext(count, new ArrayList<>(), false, ""));
            return true;
        }

        Matcher whileMatcher = WHILE_COMMAND_PATTERN.matcher(processedLine);
        if (whileMatcher.matches()) {
            String condition = whileMatcher.group(1);
            boolean result = evaluateCondition(condition, context);
            if (result) {
                loopStack.push(new LoopContext(0, new ArrayList<>(), true, condition));
            } else {
                skipLoop();
            }
            return true;
        }

        if (processedLine.equals("endloop")) {
            return true;
        }

        Matcher delayMatcher = DELAY_COMMAND_PATTERN.matcher(processedLine);
        if (delayMatcher.matches()) {
            int delay = Integer.parseInt(delayMatcher.group(1));
            delayStack.push(delay);
            return true;
        }

        Matcher callMatcher = FUNCTION_CALL_PATTERN.matcher(processedLine);
        if (callMatcher.matches()) {
            String funcName = callMatcher.group(1);
            String argsStr = callMatcher.group(2);
            String[] args = parseFunctionArgs(argsStr, context);
            return executeFunction(funcName, source, args);
        }

        Matcher returnMatcher = RETURN_COMMAND_PATTERN.matcher(processedLine);
        if (returnMatcher.matches()) {
            String returnValue = returnMatcher.group(1);
            if (returnValue != null) {
                context.returnValue = replaceVariables(returnValue, context);
            }
            context.lineIndex = context.lines.size();
            return true;
        }

        Matcher mathMatcher = MATH_EXPRESSION_PATTERN.matcher(processedLine);
        if (mathMatcher.matches()) {
            String expression = mathMatcher.group(1);
            String result = evaluateExpression(expression, context);
            if (debugMode) {
                source.sendSystemMessage(Component.literal("计算结果: " + result));
            }
            return true;
        }

        Matcher arrayMatcher = ARRAY_COMMAND_PATTERN.matcher(processedLine);
        if (arrayMatcher.matches()) {
            String arrayName = arrayMatcher.group(1);
            String sizeStr = arrayMatcher.group(2);
            int size = sizeStr != null ? Integer.parseInt(sizeStr) : 0;
            arrays.put(arrayName, new String[size]);
            return true;
        }

        Matcher arraySetMatcher = ARRAY_SET_PATTERN.matcher(processedLine);
        if (arraySetMatcher.matches()) {
            String arrayName = arraySetMatcher.group(1);
            int index = Integer.parseInt(arraySetMatcher.group(2));
            String value = arraySetMatcher.group(3);
            String[] array = arrays.get(arrayName);
            if (array == null) {
                source.sendFailure(Component.literal("数组未定义: " + arrayName));
                return false;
            }
            if (index < 0 || index >= array.length) {
                source.sendFailure(Component.literal("数组索引越界: " + index));
                return false;
            }
            array[index] = replaceVariables(value, context);
            return true;
        }

        Matcher arrayGetMatcher = ARRAY_GET_PATTERN.matcher(processedLine);
        if (arrayGetMatcher.matches()) {
            String arrayName = arrayGetMatcher.group(1);
            int index = Integer.parseInt(arrayGetMatcher.group(2));
            String varName = arrayGetMatcher.group(3);
            String[] array = arrays.get(arrayName);
            if (array == null) {
                source.sendFailure(Component.literal("数组未定义: " + arrayName));
                return false;
            }
            if (index < 0 || index >= array.length) {
                source.sendFailure(Component.literal("数组索引越界: " + index));
                return false;
            }
            setVariable(varName, array[index], context, source);
            return true;
        }

        //多线程
        Matcher asyncCallMatcher = ASYNC_CALL_PATTERN.matcher(processedLine);
        if (asyncCallMatcher.matches()) {
            String funcName = asyncCallMatcher.group(1);
            String argsStr = asyncCallMatcher.group(2);
            String[] args = parseFunctionArgs(argsStr, context);
            spawnAsyncTask(funcName, source, args, null);
            return true;
        }

        Matcher taskSpawnMatcher = TASK_SPAWN_PATTERN.matcher(processedLine);
        if (taskSpawnMatcher.matches()) {
            String funcName = taskSpawnMatcher.group(1);
            String argsStr = taskSpawnMatcher.group(2);
            String[] args = parseFunctionArgs(argsStr, context);
            String taskId = spawnAsyncTask(funcName, source, args, null);
            setVariable("$task_id", taskId, context, source);
            return true;
        }

        Matcher taskCancelMatcher = TASK_CANCEL_PATTERN.matcher(processedLine);
        if (taskCancelMatcher.matches()) {
            String taskId = replaceVariables(taskCancelMatcher.group(1), context);
            cancelAsyncTask(taskId, source);
            return true;
        }

        Matcher taskWaitMatcher = TASK_WAIT_PATTERN.matcher(processedLine);
        if (taskWaitMatcher.matches()) {
            String taskId = replaceVariables(taskWaitMatcher.group(1), context);
            waitAsyncTask(taskId, source);
            return true;
        }

        Matcher taskDelayMatcher = TASK_DELAY_PATTERN.matcher(processedLine);
        if (taskDelayMatcher.matches()) {
            int delay = Integer.parseInt(taskDelayMatcher.group(1));
            delayAsync(delay, source);
            return true;
        }

        return executeCommand(processedLine, source);
    }

    private String replaceVariables(String line, FunctionContext context) {
        Matcher matcher = VARIABLE_PATTERN.matcher(line);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = null;

            if (context != null && context.localVars.containsKey(varName)) {
                replacement = context.localVars.get(varName);
            }

            if (replacement == null && localVariables.containsKey(varName)) {
                replacement = localVariables.get(varName);
            }

            if (replacement == null && GLOBAL_VARIABLES.containsKey(varName)) {
                replacement = GLOBAL_VARIABLES.get(varName);
            }

            if (replacement == null) {
                replacement = "${" + varName + "}";
            }

            matcher.appendReplacement(result, replacement.replace("\\", "\\\\").replace("$", "\\$"));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private void setVariable(String name, String value, FunctionContext context, CommandSourceStack source) {
        if (name.startsWith("global.")) {
            String globalVarName = name.substring(7);
            GLOBAL_VARIABLES.put(globalVarName, value);
            if (debugMode) {
                MinecraftServer server = source.getServer();
                if (server != null) {
                    server.sendSystemMessage(Component.literal("设置全局变量: " + globalVarName + " = " + value));
                }
            }
        }
        else if (context != null && context.localVars.containsKey(name)) {
            context.localVars.put(name, value);
        } else {
            localVariables.put(name, value);
        }
    }

    private String spawnAsyncTask(String functionName, CommandSourceStack source, String[] args, String customTaskId) {
        String taskId = customTaskId != null ? customTaskId : UUID.randomUUID().toString();

        Future<?> future = ASYNC_EXECUTOR.submit(() -> {
            try {
                ScriptExecutor asyncExecutor = new ScriptExecutor(this.debugMode, this.maxExecutionTime);
                asyncExecutor.localVariables.putAll(this.localVariables);
                asyncExecutor.functions.putAll(this.functions);
                asyncExecutor.arrays.putAll(this.arrays);

                asyncExecutor.executeFunction(functionName, source, args);

                if (debugMode) {
                    source.sendSystemMessage(Component.literal("异步任务完成: " + taskId));
                }
            } catch (Exception e) {
                source.sendFailure(Component.literal("异步任务执行错误: " + e.getMessage()));
                if (debugMode) {
                    e.printStackTrace();
                }
            } finally {
                ASYNC_TASKS.remove(taskId);
            }
        });

        ASYNC_TASKS.put(taskId, future);

        if (debugMode) {
            source.sendSystemMessage(Component.literal("启动异步任务: " + taskId + " -> " + functionName));
        }

        return taskId;
    }

    private boolean cancelAsyncTask(String taskId, CommandSourceStack source) {
        Future<?> future = ASYNC_TASKS.get(taskId);
        if (future != null) {
            boolean cancelled = future.cancel(true);
            ASYNC_TASKS.remove(taskId);

            if (debugMode) {
                if (cancelled) {
                    source.sendSystemMessage(Component.literal("已取消异步任务: " + taskId));
                } else {
                    source.sendSystemMessage(Component.literal("无法取消异步任务: " + taskId));
                }
            }

            return cancelled;
        } else {
            source.sendFailure(Component.literal("异步任务不存在: " + taskId));
            return false;
        }
    }

    private boolean waitAsyncTask(String taskId, CommandSourceStack source) {
        Future<?> future = ASYNC_TASKS.get(taskId);
        if (future != null) {
            try {
                future.get();
                return true;
            } catch (InterruptedException | ExecutionException | CancellationException e) {
                source.sendFailure(Component.literal("等待异步任务时出错: " + e.getMessage()));
                return false;
            }
        } else {
            source.sendFailure(Component.literal("异步任务不存在: " + taskId));
            return false;
        }
    }

    private void delayAsync(int ticks, CommandSourceStack source) {
        try {
            long millis = ticks * 50L;
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            source.sendFailure(Component.literal("异步延迟被中断"));
        }
    }

    // 添加关闭线程池的方法
    public static void shutdownAsyncExecutor() {
        ASYNC_EXECUTOR.shutdown();
        try {
            if (!ASYNC_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                ASYNC_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            ASYNC_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
        ASYNC_TASKS.clear();
    }

    private boolean evaluateCondition(String condition, FunctionContext context) {
        String processedCondition = replaceVariables(condition, context);

        if (processedCondition.contains("==")) {
            String[] parts = processedCondition.split("==", 2);
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                return left.equals(right);
            }
        } else if (processedCondition.contains("!=")) {
            String[] parts = processedCondition.split("!=", 2);
            if (parts.length == 2) {
                String left = parts[0].trim();
                String right = parts[1].trim();
                return !left.equals(right);
            }
        } else if (processedCondition.contains("<")) {
            String[] parts = processedCondition.split("<", 2);
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim());
                    double right = Double.parseDouble(parts[1].trim());
                    return left < right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        } else if (processedCondition.contains(">")) {
            String[] parts = processedCondition.split(">", 2);
            if (parts.length == 2) {
                try {
                    double left = Double.parseDouble(parts[0].trim());
                    double right = Double.parseDouble(parts[1].trim());
                    return left > right;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }

        return !processedCondition.trim().isEmpty() &&
                !processedCondition.trim().equals("0") &&
                !processedCondition.trim().equalsIgnoreCase("false");
    }

    private String evaluateExpression(String expression, FunctionContext context) {
        String processedExpr = replaceVariables(expression, context);

        try {
            if (processedExpr.contains("+")) {
                String[] parts = processedExpr.split("\\+", 2);
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return String.valueOf(left + right);
            } else if (processedExpr.contains("-")) {
                String[] parts = processedExpr.split("-", 2);
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return String.valueOf(left - right);
            } else if (processedExpr.contains("*")) {
                String[] parts = processedExpr.split("\\*", 32);
                double left =Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                return String.valueOf(left * right);
            } else if (processedExpr.contains("/")) {
                String[] parts = processedExpr.split("/", 2);
                double left = Double.parseDouble(parts[0].trim());
                double right = Double.parseDouble(parts[1].trim());
                if (right == 0) {
                    return "0";
                }
                return String.valueOf(left / right);
            }

            return processedExpr;
        } catch (NumberFormatException e) {
            return processedExpr;
        }
    }

    private String[] parseFunctionArgs(String argsStr, FunctionContext context) {
        if (argsStr == null || argsStr.trim().isEmpty()) {
            return new String[0];
        }

        String[] args = argsStr.split("\\s*,\\s*");
        for (int i = 0; i < args.length; i++) {
            args[i] = replaceVariables(args[i], context);
        }

        return args;
    }

    private void skipLoop() {
        int depth = 1;
        FunctionContext context = callStack.peek();

        while (context.lineIndex < context.lines.size()) {
            String line = context.lines.get(context.lineIndex);
            context.lineIndex++;

            if (line.trim().equals("loop") || line.trim().startsWith("while")) {
                depth++;
            } else if (line.trim().equals("endloop")) {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
        }
    }

    private boolean executeCommand(String command, CommandSourceStack source) {
        MinecraftServer server = source.getServer();
        try {
            int result = server.getCommands().performPrefixedCommand(source, command);
            return result > 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("执行命令时出错: " + command + " - " + e.getMessage()));
            return false;
        }
    }

    private static class ScriptFunction {
        private final String name;
        private final List<String> params;
        private final List<String> lines;

        public ScriptFunction(String name, List<String> params, List<String> lines) {
            this.name = name;
            this.params = params;
            this.lines = lines;
        }

        public String getName() { return name; }
        public List<String> getParams() { return params; }
        public List<String> getLines() { return lines; }
    }

    private static class FunctionContext {
        private final String functionName;
        private final List<String> lines;
        private int lineIndex;
        private final Map<String, String> localVars;
        private String returnValue;

        public FunctionContext(String functionName, List<String> lines, int lineIndex, Map<String, String> localVars) {
            this.functionName = functionName;
            this.lines = lines;
            this.lineIndex = lineIndex;
            this.localVars = localVars;
        }
    }

    private static class LoopContext {
        private int iterations;
        private final List<String> lines;
        private boolean collecting = true;
        private int currentLine = 0;
        private final boolean isWhileLoop;
        private final String condition;

        public LoopContext(int iterations, List<String> lines, boolean isWhileLoop, String condition) {
            this.iterations = iterations;
            this.lines = lines;
            this.isWhileLoop = isWhileLoop;
            this.condition = condition;
        }
    }

    public static void setGlobalVariable(String name, String value) {
        GLOBAL_VARIABLES.put(name, value);
    }

    public static String getGlobalVariable(String name) {
        return GLOBAL_VARIABLES.get(name);
    }

    public static void removeGlobalVariable(String name) {
        GLOBAL_VARIABLES.remove(name);
    }

    public static void clearGlobalVariables() {
        GLOBAL_VARIABLES.clear();
    }

    public static Map<String, String> getGlobalVariables() {
        return new HashMap<>(GLOBAL_VARIABLES);
    }
}