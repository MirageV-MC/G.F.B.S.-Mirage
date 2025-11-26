package org.mirage.Objects.blocks.Control;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Convex89524
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.mirage.CommandExecutor;
import org.mirage.Objects.blocks.classs.AbstractFluorescentLampBlock;
import org.mirage.Phenomenon.network.Network.ClientEventHandler;

import javax.annotation.Nullable;
import java.util.*;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "mirage_gfbs", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FluorescentTubeClientAPI {

    private static final Set<BlockPos> REGISTERED_TUBES = new HashSet<>();

    private static final List<BlinkTask> BLINK_TASKS = new ArrayList<>();

    static {
        registerNetworkReceivers();
    }

    private FluorescentTubeClientAPI() {
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
        synchronized (BLINK_TASKS) {
            BLINK_TASKS.clear();
            BLINK_TASKS.add(new BlinkTask(durationTicks, frequencyHz, finalState));
        }
    }

    public static void turnOffAll() {
        flashAll(40, 5.0, Boolean.FALSE);
    }

    public static void turnOnAll() {
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
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }

        synchronized (BLINK_TASKS) {
            if (BLINK_TASKS.isEmpty()) {
                return;
            }
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

        BlinkTask(int durationTicks, double averageFrequencyHz, @Nullable Boolean finalState) {
            this.remainingTicks = durationTicks;
            this.totalDurationTicks = durationTicks;
            this.initialFrequencyHz = averageFrequencyHz;
            // 简单线性衰减：从 initialFrequencyHz 衰减到 0
            this.decayRate = averageFrequencyHz / Math.max(durationTicks, 1);
            this.finalState = finalState;
        }

        void tick(Level level) {
            if (isFinished() || !(level instanceof ClientLevel)) {
                return;
            }

            boolean lastTick = remainingTicks == 1;
            remainingTicks--;

            synchronized (REGISTERED_TUBES) {
                if (REGISTERED_TUBES.isEmpty()) {
                    return;
                }

                for (BlockPos pos : REGISTERED_TUBES) {
                    TubeBlinkState tubeState = tubeStates.computeIfAbsent(pos, p -> createInitialState(level, p));

                    if (tubeState.ticksUntilToggle <= 0 && !lastTick) {
                        tubeState.ticksUntilToggle = sampleIntervalTicks();
                        tubeState.currentLit = !tubeState.currentLit;
                    } else if (!lastTick) {
                        tubeState.ticksUntilToggle--;
                    }

                    if (lastTick && finalState != null) {
                        tubeState.currentLit = finalState;
                    }

                    BlockState state = level.getBlockState(pos);
                    if (state.getBlock() instanceof AbstractFluorescentLampBlock) {
                        Boolean worldLit = state.getValue(AbstractFluorescentLampBlock.LIT);
                        if (!Objects.equals(worldLit, tubeState.currentLit)) {
                            if (Boolean.TRUE.equals(tubeState.currentLit)) {
                                playToggleSound(pos);
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
            double u = random.nextDouble(); // 不使用 MC RandomSource
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

        private void playToggleSound(BlockPos pos) {
            CommandExecutor.executeCommand(
                    "playsound mirage_gfbs:surroundings.ding block @a "
                            + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " 1 1 0"
            );
        }
    }

    private static final class TubeBlinkState {
        boolean currentLit;
        int ticksUntilToggle;

        TubeBlinkState(boolean currentLit, int ticksUntilToggle) {
            this.currentLit = currentLit;
            this.ticksUntilToggle = ticksUntilToggle;
        }
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
