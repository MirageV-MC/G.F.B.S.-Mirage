package org.mirage.gfbs.Objects.blocks.Control;

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.gfbs.ModSoundEvents;
import org.mirage.gfbs.Objects.blocks.Bases.FlBlock.AbstractFluorescentLampBlock;
import org.mirage.gfbs.Phenomenon.network.Network.ClientEventHandler;

import javax.annotation.Nullable;
import java.util.*;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "mirage_gfbs", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FluorescentTubeClientAPI {

    private static final Set<BlockPos> REGISTERED_TUBES = new HashSet<>();

    private static final List<BlinkTask> BLINK_TASKS = new ArrayList<>();

    private static final Random UNSTABLE_RANDOM = new Random();

    /**
     * 客户端全局灯状态：用于区块重新加载/客户端重进时快速恢复视觉状态。
     * true=亮，false=灭
     */
    public static volatile boolean globalState = true;

    /**
     * 不稳定模式枚举
     */
    public enum InstabilityMode {
        NONE,      // 无不稳定
        LOW,       // 轻度不稳定
        HIGH       // 高度不稳定
    }

    /**
     * 当前不稳定模式
     */
    public static volatile InstabilityMode currentInstabilityMode = InstabilityMode.NONE;

    static {
        registerNetworkReceivers();
    }

    private FluorescentTubeClientAPI() {
    }

    /**
     * 设置不稳定模式
     */
    public static void setInstabilityMode(InstabilityMode mode) {
        currentInstabilityMode = mode;
        if (mode == InstabilityMode.NONE) {
            synchronized (REGISTERED_TUBES) {
                TUBE_INSTABILITY_STATES.clear();
            }
            // 不再由客户端主动恢复状态，而是依赖服务端发送的 BlockUpdate
            // restoreAllTubes();
        }
    }

    private static void restoreAllTubes() {
        // 废弃：客户端无法准确判断红石信号，状态恢复完全依赖服务端 refreshAllTubes
    }

    /**
     * 集体闪烁
     *
     * @param durationTicks 持续时间（tick，20 tick = 1 秒）
     * @param frequencyHz   闪烁频率（Hz，每秒几次，作为平均频率）
     * @param finalState    true=结束时全亮，false=结束时全灭，null=结束后不强制状态
     */
    public static void flashAll(int durationTicks, double frequencyHz, @Nullable Boolean finalState) {
        if (durationTicks <= 0 || frequencyHz <= 0.0) {
            return;
        }
        if (finalState != null) {
            globalState = finalState;
        }
        synchronized (BLINK_TASKS) {
            BLINK_TASKS.clear();
            BLINK_TASKS.add(new BlinkTask(durationTicks, frequencyHz, finalState));
        }
    }

    public static void turnOffAll() {
        globalState = false;
        synchronized (REGISTERED_TUBES) {
            TUBE_INSTABILITY_STATES.clear();
        }
        flashAll(40, 5.0, Boolean.FALSE);
    }

    public static void turnOnAll() {
        globalState = true;
        flashAll(40, 5.0, Boolean.TRUE);
    }

    public static void stopBlinking() {
        synchronized (BLINK_TASKS) {
            BLINK_TASKS.clear();
        }
    }

    public static void registerTube(BlockPos pos) {
        if (pos != null) {
            synchronized (REGISTERED_TUBES) {
                REGISTERED_TUBES.add(pos.immutable());
            }
        }
    }

    public static void unregisterTube(BlockPos pos) {
        if (pos != null) {
            synchronized (REGISTERED_TUBES) {
                REGISTERED_TUBES.remove(pos);
            }
        }
    }

    @SubscribeEvent
    public static void onClientLogout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        resetClientState();
    }

    @SubscribeEvent
    public static void onClientLogin(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingIn event) {
        resetClientState();
    }

    private static void resetClientState() {
        globalState = true;
        currentInstabilityMode = InstabilityMode.NONE;
        synchronized (REGISTERED_TUBES) {
            REGISTERED_TUBES.clear();
            TUBE_INSTABILITY_STATES.clear();
        }
        synchronized (BLINK_TASKS) {
            BLINK_TASKS.clear();
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            if (globalState != true || currentInstabilityMode != InstabilityMode.NONE) {
                resetClientState();
            }
            return;
        }

        synchronized (BLINK_TASKS) {
            if (!BLINK_TASKS.isEmpty()) {
                Iterator<BlinkTask> it = BLINK_TASKS.iterator();
                while (it.hasNext()) {
                    BlinkTask task = it.next();
                    if (task.isFinished()) {
                        it.remove();
                    } else {
                        task.tick(level);
                    }
                }
            }
        }

        if (currentInstabilityMode != InstabilityMode.NONE && globalState) {
            synchronized (REGISTERED_TUBES) {
                ensureRegisteredTubes(level);
                
                // 遍历所有注册的灯管，每个灯管独立更新状态
                for (BlockPos pos : REGISTERED_TUBES) {
                    TubeInstabilityState state = TUBE_INSTABILITY_STATES.computeIfAbsent(pos, p -> new TubeInstabilityState());
                    state.tick(level, pos);
                }
                
                // 清理不存在的灯管状态
                TUBE_INSTABILITY_STATES.keySet().removeIf(pos -> !REGISTERED_TUBES.contains(pos));
            }
        }
    }

    /**
     * 注册网络事件（服务端触发 -> 客户端执行）
     */
    private static void registerNetworkReceivers() {
        // 集体闪烁
        ClientEventHandler.registerEvent("fluorescent_tube_flash_all", data -> {
            updateRegisteredTubesFromData(data);
            handleFlashEvent(data);
        });

        // 所有灯点亮
        ClientEventHandler.registerEvent("fluorescent_tube_turn_on_all", data -> {
            updateRegisteredTubesFromData(data);
            turnOnAll();
        });

        // 所有灯熄灭
        ClientEventHandler.registerEvent("fluorescent_tube_turn_off_all", data -> {
            updateRegisteredTubesFromData(data);
            turnOffAll();
        });

        // 设置不稳定模式
        ClientEventHandler.registerEvent("fluorescent_tube_set_instability", data -> {
            String modeStr = data.getString("mode");
            InstabilityMode mode = InstabilityMode.valueOf(modeStr);
            setInstabilityMode(mode);
        });

        // 同步配置
        ClientEventHandler.registerEvent("fluorescent_tube_sync_config", data -> {
            if (data.contains("mode")) {
                String modeStr = data.getString("mode");
                try {
                    InstabilityMode mode = InstabilityMode.valueOf(modeStr);
                    // 仅设置模式，不触发闪烁
                    currentInstabilityMode = mode;
                    if (mode == InstabilityMode.NONE) {
                        synchronized (REGISTERED_TUBES) {
                            TUBE_INSTABILITY_STATES.clear();
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (data.contains("globalState")) {
                boolean newGlobalState = data.getBoolean("globalState");
                if (globalState != newGlobalState) {
                    globalState = newGlobalState;
                    // 同步时不播放动画，仅清空当前任务，等待服务端 BlockUpdate
                    synchronized (BLINK_TASKS) {
                        BLINK_TASKS.clear();
                    }
                    if (!globalState) {
                        synchronized (REGISTERED_TUBES) {
                            TUBE_INSTABILITY_STATES.clear();
                        }
                    }
                }
            }
        });
    }

    private static void handleFlashEvent(CompoundTag data) {
        int duration = data.contains("duration") ? data.getInt("duration") : 40;
        double frequency = data.contains("frequency") ? data.getDouble("frequency") : 2.0D;
        flashAll(duration, frequency, Boolean.TRUE);
    }

    private static void ensureRegisteredTubes(ClientLevel level) {
        synchronized (REGISTERED_TUBES) {
            if (!REGISTERED_TUBES.isEmpty()) {
                return;
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        BlockPos center = mc.player.blockPosition();
        int radius = 32;

        BlockPos min = center.offset(-radius, -radius, -radius);
        BlockPos max = center.offset(radius, radius, radius);

        Set<BlockPos> found = new HashSet<>();

        for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof AbstractFluorescentLampBlock) {
                found.add(pos.immutable());
            }
        }

        synchronized (REGISTERED_TUBES) {
            REGISTERED_TUBES.addAll(found);
        }
    }

    private static void applyLitStateToAll(boolean lit) {
        globalState = lit;
        if (!lit) {
            synchronized (REGISTERED_TUBES) {
                TUBE_INSTABILITY_STATES.clear();
            }
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        ensureRegisteredTubes(level);

        synchronized (REGISTERED_TUBES) {
            for (BlockPos pos : REGISTERED_TUBES) {
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof AbstractFluorescentLampBlock) {
                    Boolean current = state.getValue(AbstractFluorescentLampBlock.LIT);
                    if (!Objects.equals(current, lit)) {
                        level.setBlock(pos, state.setValue(AbstractFluorescentLampBlock.LIT, lit), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    /**
     * 不稳定闪烁 - 每个灯管完全独立的随机闪烁
     * 参考 CoolEffectsScript.lua 中的 FlickerLight 函数
     * 
     * 核心特性：
     * 1. 每个灯管完全独立运行，有自己的闪烁周期
     * 2. 每个灯管的闪烁参数都不同（延迟、持续时间、循环次数）
     * 3. 不是集体同步闪烁，而是各自随机闪烁
     * 
     * Lua 脚本参数：
     * - BreakerMin = 25, BreakerMax = 90, BreakerDivisor = 1000 (毫秒)
     * - 但我们需要更长的闪烁时间和间隔，让效果更明显
     */
    private static final Map<BlockPos, TubeInstabilityState> TUBE_INSTABILITY_STATES = new HashMap<>();

    /**
     * 每个灯管的独立状态机
     */
    private static final class TubeInstabilityState {
        private final Random random = new Random();
        
        enum State {
            IDLE,           // 空闲状态（等待下次闪烁）
            FADING,         // 闪烁循环中（关→开→关→开...）
            ON_STABLE,      // 稳定点亮
            RESTART_DELAY   // 重启延迟
        }

        private State currentState = State.IDLE;
        private int idleTicksRemaining;      // 空闲等待时间
        private int loopCount = 0;
        private int targetLoops;             // 目标闪烁次数（2-5 次）
        private int ticksInCurrentPhase = 0;
        private boolean isLightOn = false;
        private boolean isFinished = false;
        private boolean hasPlayedSoundForThisRound = false;
        private int flickerSpeed;            // 闪烁速度（每切换一次持续多少 tick）

        TubeInstabilityState() {
            initializeRandomParameters();
        }

        /**
         * 初始化随机参数 - 每个灯管都不同
         */
        void initializeRandomParameters() {
            // 随机空闲时间：轻度 10-30 秒，高度 3-10 秒
            if (currentInstabilityMode == InstabilityMode.HIGH) {
                this.idleTicksRemaining = UNSTABLE_RANDOM.nextInt(120) + 60;  // 3-10 秒
            } else {
                this.idleTicksRemaining = UNSTABLE_RANDOM.nextInt(400) + 200; // 10-30 秒
            }
            
            // 随机闪烁次数：2-5 次
            this.targetLoops = UNSTABLE_RANDOM.nextInt(4) + 2;
            
            // 随机闪烁速度：1-4 ticks (0.05-0.2 秒)
            this.flickerSpeed = UNSTABLE_RANDOM.nextInt(4) + 1;
        }

        void tick(ClientLevel level, BlockPos pos) {
            if (isFinished) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AbstractFluorescentLampBlock)) {
                return;
            }

            ticksInCurrentPhase++;

            switch (currentState) {
                case IDLE:
                    handleIdleState(level, pos, state);
                    break;
                case FADING:
                    handleFadingState(level, pos, state);
                    break;
                case ON_STABLE:
                    handleOnStableState(level, pos, state);
                    break;
                case RESTART_DELAY:
                    handleRestartDelayState(level, pos, state);
                    break;
            }
        }

        private void handleIdleState(ClientLevel level, BlockPos pos, BlockState state) {
            if (idleTicksRemaining > 0) {
                idleTicksRemaining--;
                return;
            }

            // 空闲时间结束，开始闪烁
            currentState = State.FADING;
            ticksInCurrentPhase = 0;
            loopCount = 0;
            hasPlayedSoundForThisRound = false;
            
            // 确保灯是灭的开始闪烁
            level.setBlock(pos, state.setValue(AbstractFluorescentLampBlock.LIT, false), Block.UPDATE_ALL);
            isLightOn = false;
        }

        private void handleFadingState(ClientLevel level, BlockPos pos, BlockState state) {
            // 每轮闪烁开始时播放音效
            if (!hasPlayedSoundForThisRound) {
                playBreakerSound(level, pos);
                hasPlayedSoundForThisRound = true;
            }
            
            if (ticksInCurrentPhase >= flickerSpeed) {
                isLightOn = !isLightOn;
                // 使用 !isLightOn 来反转逻辑，确保灯光正确闪烁
                level.setBlock(pos, state.setValue(AbstractFluorescentLampBlock.LIT, !isLightOn), Block.UPDATE_ALL);
                
                loopCount++;
                ticksInCurrentPhase = 0;

                if (loopCount >= targetLoops * 2) {
                    // 闪烁结束，判断是否重启
                    if (currentInstabilityMode == InstabilityMode.HIGH) {
                        double retryChance = 0.3;
                        if (random.nextDouble() < retryChance) {
                            // 30% 概率重启
                            currentState = State.RESTART_DELAY;
                            ticksInCurrentPhase = 0;
                            hasPlayedSoundForThisRound = false;
                            return;
                        }
                    }
                    // 成功启辉，进入稳定状态
                    currentState = State.ON_STABLE;
                }
            }
        }

        private void handleOnStableState(ClientLevel level, BlockPos pos, BlockState state) {
            // 稳定点亮，重置为空闲状态
            initializeRandomParameters();
            currentState = State.IDLE;
        }

        private void handleRestartDelayState(ClientLevel level, BlockPos pos, BlockState state) {
            int restartDelay = 40; // 2 秒延迟

            if (ticksInCurrentPhase >= restartDelay) {
                // 重启延迟结束，重新开始空闲状态
                initializeRandomParameters();
                currentState = State.IDLE;
                ticksInCurrentPhase = 0;
            }
        }

        private void playBreakerSound(ClientLevel level, BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            if (level == null || mc.player == null) {
                return;
            }

            double playerDist = pos.distToCenterSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (playerDist > 64.0 * 64.0) {
                return;
            }

            SoundEvent sound;
            try {
                sound = ModSoundEvents.getSoundOrNull("surroundings.ding");
            } catch (Exception e) {
                return;
            }

            if (sound == null) {
                return;
            }

            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.5;
            double z = pos.getZ() + 0.5;

            float pitch = 1.0F;

            level.playLocalSound(
                    x, y, z,
                    sound,
                    SoundSource.BLOCKS,
                    0.75F,
                    pitch,
                    false
            );
        }
    }

    private static final class BlinkTask {
        private final Random random = new Random();
        private final Map<BlockPos, TubeBlinkState> tubeStates = new HashMap<>();

        private int remainingTicks;
        private final int totalDurationTicks;
        private final double initialFrequencyHz;
        private final double decayRate;

        /**
         * 最终状态：
         * - true  => 结束时强制全部点亮
         * - false => 结束时强制全部熄灭
         * - null  => 只闪烁，不强制最终状态
         */
        @Nullable
        private final Boolean finalState;

        /**
         * 闪烁阶段参数（参考 CoolEffectsScript.lua）
         */
        private static final int INTENSE_FLICKER_DURATION_TICKS = 15; // 高强度闪烁 0.75 秒（15 ticks）
        private static final int MIN_FLICKER_INTERVAL_TICKS = 2;      // 最小闪烁间隔 2 ticks (0.1 秒)
        private static final int MAX_FLICKER_INTERVAL_TICKS = 18;     // 最大闪烁间隔 18 ticks (0.9 秒)
        private static final int MIN_INTENSE_INTERVAL_TICKS = 1;      // 高强度最小间隔 1 tick (0.05 秒)
        private static final int MAX_INTENSE_INTERVAL_TICKS = 3;      // 高强度最大间隔 3 ticks (0.15 秒)
        private static final int NORMAL_FLICKER_COUNT_MIN = 1;        // 普通闪烁最少次数
        private static final int NORMAL_FLICKER_COUNT_MAX = 3;        // 普通闪烁最多次数

        /**
         * 闪烁阶段
         */
        private FlickerPhase currentPhase = FlickerPhase.INTENSE;
        private int intenseFlickerTicksRemaining = INTENSE_FLICKER_DURATION_TICKS;
        private int normalFlickerCountRemaining = 0;
        private int normalFlickerState = 0; // 0=关，1=开

        BlinkTask(int durationTicks, double averageFrequencyHz, @Nullable Boolean finalState) {
            this.remainingTicks = durationTicks;
            this.totalDurationTicks = durationTicks;
            this.initialFrequencyHz = averageFrequencyHz;
            this.decayRate = averageFrequencyHz / Math.max(durationTicks, 1);
            this.finalState = finalState;
            this.currentPhase = FlickerPhase.INTENSE;
            this.intenseFlickerTicksRemaining = INTENSE_FLICKER_DURATION_TICKS;
            this.normalFlickerCountRemaining = random.nextInt(NORMAL_FLICKER_COUNT_MAX - NORMAL_FLICKER_COUNT_MIN + 1) + NORMAL_FLICKER_COUNT_MIN;
            this.normalFlickerState = 0;
        }

        void tick(Level level) {
            soundsPlayedThisTick = 0;

            if (isFinished() || !(level instanceof ClientLevel)) {
                return;
            }

            boolean lastTick = remainingTicks == 1;
            remainingTicks--;

            // 更新闪烁阶段
            updateFlickerPhase();

            synchronized (REGISTERED_TUBES) {
                if (REGISTERED_TUBES.isEmpty()) {
                    return;
                }

                for (BlockPos pos : REGISTERED_TUBES) {
                    TubeBlinkState tubeState = tubeStates.computeIfAbsent(pos, p -> createInitialState(level, p));

                    // 根据当前阶段更新灯管状态
                    if (currentPhase == FlickerPhase.INTENSE) {
                        // 高强度快速闪烁阶段
                        if (tubeState.ticksUntilToggle <= 0 && !lastTick) {
                            tubeState.ticksUntilToggle = sampleIntenseIntervalTicks();
                            tubeState.currentLit = !tubeState.currentLit;
                        } else if (!lastTick) {
                            tubeState.ticksUntilToggle--;
                        }
                    } else if (currentPhase == FlickerPhase.NORMAL) {
                        // 普通闪烁阶段（模拟断路器尝试启辉）
                        if (tubeState.ticksUntilToggle <= 0 && !lastTick) {
                            tubeState.ticksUntilToggle = sampleNormalIntervalTicks();
                            tubeState.currentLit = !tubeState.currentLit;
                        } else if (!lastTick) {
                            tubeState.ticksUntilToggle--;
                        }
                    } else if (currentPhase == FlickerPhase.FINISHED) {
                        // 结束阶段，根据 finalState 设置
                        if (finalState != null) {
                            tubeState.currentLit = finalState;
                        }
                    }

                    if (lastTick && finalState != null) {
                        tubeState.currentLit = finalState;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof AbstractFluorescentLampBlock) {
                        Boolean worldLit = state.getValue(AbstractFluorescentLampBlock.LIT);
                        if (!Objects.equals(worldLit, tubeState.currentLit)) {
                            if (Boolean.TRUE.equals(tubeState.currentLit) && !tubeState.hasPlayedSound) {
                                playToggleSound(pos);
                                tubeState.hasPlayedSound = true;
                            }
                            level.setBlock(
                                    pos,
                                    state.setValue(AbstractFluorescentLampBlock.LIT, tubeState.currentLit),
                                    Block.UPDATE_ALL
                            );
                        }
                    }
                }
            }
        }

        /**
         * 更新闪烁阶段
         */
        private void updateFlickerPhase() {
            if (currentPhase == FlickerPhase.INTENSE) {
                intenseFlickerTicksRemaining--;
                if (intenseFlickerTicksRemaining <= 0) {
                    // 高强度闪烁结束，进入普通闪烁阶段
                    currentPhase = FlickerPhase.NORMAL;
                    normalFlickerCountRemaining = random.nextInt(NORMAL_FLICKER_COUNT_MAX - NORMAL_FLICKER_COUNT_MIN + 1) + NORMAL_FLICKER_COUNT_MIN;
                    normalFlickerState = 0;
                }
            } else if (currentPhase == FlickerPhase.NORMAL) {
                // 普通闪烁阶段已经完成，进入结束阶段
                if (normalFlickerCountRemaining <= 0) {
                    currentPhase = FlickerPhase.FINISHED;
                }
            }
        }

        /**
         * 高强度闪烁阶段的间隔时间采样（5-15 毫秒，参考 Lua 脚本）
         */
        private int sampleIntenseIntervalTicks() {
            return random.nextInt(MAX_INTENSE_INTERVAL_TICKS - MIN_INTENSE_INTERVAL_TICKS + 1) + MIN_INTENSE_INTERVAL_TICKS;
        }

        /**
         * 普通闪烁阶段的间隔时间采样（0.1-0.9 秒）
         */
        private int sampleNormalIntervalTicks() {
            return random.nextInt(MAX_FLICKER_INTERVAL_TICKS - MIN_FLICKER_INTERVAL_TICKS + 1) + MIN_FLICKER_INTERVAL_TICKS;
        }

        private boolean isFinished() {
            return remainingTicks <= 0;
        }

        private double getCurrentFrequency() {
            double elapsedTicks = totalDurationTicks - remainingTicks;
            double currentFreq = initialFrequencyHz - decayRate * elapsedTicks;
            return Math.max(0.0, currentFreq);
        }

        private int sampleIntervalTicks() {
            double currentFreqHz = getCurrentFrequency();
            if (currentFreqHz <= 0.0) {
                return Integer.MAX_VALUE;
            }

            double averageTogglePerTick = Math.max(currentFreqHz / 20.0, 0.0001D);
            double u = random.nextDouble();
            int interval = (int) Math.round(-Math.log(1.0 - u) / averageTogglePerTick);
            return Math.max(1, interval);
        }

        private TubeBlinkState createInitialState(Level level, BlockPos pos) {
            boolean initialLit = false;
            BlockState blockState = level.getBlockState(pos);
            if (blockState.getBlock() instanceof AbstractFluorescentLampBlock) {
                initialLit = blockState.getValue(AbstractFluorescentLampBlock.LIT);
            }
            int firstInterval = sampleIntervalTicks();
            return new TubeBlinkState(initialLit, firstInterval);
        }

        private static final int MAX_SOUNDS_PER_TICK = 6;
        private static final double SOUND_MERGE_DISTANCE = 8.0;
        private static final double PLAYER_SOUND_RANGE = 64.0;
        private static int soundsPlayedThisTick = 0;

        private void playToggleSound(BlockPos pos) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            if (level == null || mc.player == null) {
                return;
            }

            double playerDist = pos.distToCenterSqr(mc.player.getX(), mc.player.getY(), mc.player.getZ());
            if (playerDist > PLAYER_SOUND_RANGE * PLAYER_SOUND_RANGE) {
                return;
            }

            if (soundsPlayedThisTick >= MAX_SOUNDS_PER_TICK) {
                return;
            }

            SoundEvent sound;
            try {
                sound = ModSoundEvents.getSoundOrNull("surroundings.ding");
            } catch (Exception e) {
                return;
            }

            if (sound == null) {
                return;
            }

            BlockPos mergedPos = findNearbySoundPosition(pos, level, this.tubeStates);

            double x = mergedPos.getX() + 0.5;
            double y = mergedPos.getY() + 0.5;
            double z = mergedPos.getZ() + 0.5;

            level.playLocalSound(
                    x, y, z,
                    sound,
                    SoundSource.BLOCKS,
                    0.7F,
                    1.0F,
                    false
            );

            soundsPlayedThisTick++;
        }
    }

    private static BlockPos findNearbySoundPosition(BlockPos originalPos, ClientLevel level, Map<BlockPos, TubeBlinkState> tubeStates) {
        synchronized (REGISTERED_TUBES) {
            int totalX = originalPos.getX();
            int totalY = originalPos.getY();
            int totalZ = originalPos.getZ();
            int count = 1;

            for (BlockPos otherPos : REGISTERED_TUBES) {
                if (!otherPos.equals(originalPos) &&
                        otherPos.distSqr(originalPos) <= BlinkTask.SOUND_MERGE_DISTANCE * BlinkTask.SOUND_MERGE_DISTANCE) {

                    TubeBlinkState otherState = tubeStates.get(otherPos);
                    if (otherState != null && otherState.currentLit) {
                        totalX += otherPos.getX();
                        totalY += otherPos.getY();
                        totalZ += otherPos.getZ();
                        count++;
                    }
                }
            }

            if (count == 1) {
                return originalPos;
            }

            int avgX = totalX / count;
            int avgY = totalY / count;
            int avgZ = totalZ / count;

            return new BlockPos(avgX, avgY, avgZ);
        }
    }

    private static final class TubeBlinkState {
        boolean currentLit;
        int ticksUntilToggle;
        boolean hasPlayedSound;

        TubeBlinkState(boolean currentLit, int ticksUntilToggle) {
            this.currentLit = currentLit;
            this.ticksUntilToggle = ticksUntilToggle;
            this.hasPlayedSound = false;
        }
    }

    /**
     * 闪烁阶段枚举（参考 CoolEffectsScript.lua）
     */
    private enum FlickerPhase {
        /**
         * 高强度快速闪烁阶段（类似 Lua 的 IntenseFlickerLight）
         */
        INTENSE,
        /**
         * 普通闪烁阶段（模拟断路器尝试启辉）
         */
        NORMAL,
        /**
         * 结束阶段
         */
        FINISHED
    }

    private static void updateRegisteredTubesFromData(CompoundTag data) {
        synchronized (REGISTERED_TUBES) {
            REGISTERED_TUBES.clear();
            if (!data.contains("tubes", Tag.TAG_LIST)) {
                return;
            }
            ListTag list = data.getList("tubes", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag t = list.getCompound(i);
                int x = t.getInt("x");
                int y = t.getInt("y");
                int z = t.getInt("z");
                REGISTERED_TUBES.add(new BlockPos(x, y, z));
            }
        }
    }
}
