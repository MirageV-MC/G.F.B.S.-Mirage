package org.mirage.ccio.peripheral;

/**
 * G.F.B.S. Mirage (mirage_gfbs) - A Minecraft Mod
 * Copyright (C) 2025-2029 Mirage-MC
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

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.AttachedComputerSet;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.mirage.ccio.api.CCIoApiRegistry;
import org.mirage.ccio.blockentity.CCIoBridgeBlockEntity;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public final class CCIoBridgePeripheral implements IPeripheral {
    private final CCIoBridgeBlockEntity be;
    private AttachedComputerSet computers = new AttachedComputerSet();
    private boolean invalid = false;

    private static final String STREAM_EVENT = "gfbs_ccio_stream";
    private static final AtomicLong STREAM_SEQ = new AtomicLong(1);
    private static final ConcurrentHashMap<Long, Future<?>> STREAM_TASKS = new ConcurrentHashMap<>();
    private static final ExecutorService STREAM_EXEC = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gfbs-ccio-stream");
        t.setDaemon(true);
        return t;
    });

    public CCIoBridgePeripheral(CCIoBridgeBlockEntity be) {
        this.be = be;
    }

    public void invalidate() {
        invalid = true;
        computers = new AttachedComputerSet();
    }

    private void requireValid() throws LuaException {
        if (invalid || be.isRemoved()) throw new LuaException("Peripheral is invalid");
    }

    private ServerLevel requireServerLevel() throws LuaException {
        Level level = be.getLevel();
        if (!(level instanceof ServerLevel sl)) throw new LuaException("Not on server");
        return sl;
    }

    private MinecraftServer requireServer() throws LuaException {
        MinecraftServer server = requireServerLevel().getServer();
        if (server == null) throw new LuaException("Server is null");
        return server;
    }

    @Override
    public String getType() {
        return "gfbs";
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        if (this == other) return true;
        if (!(other instanceof CCIoBridgePeripheral o)) return false;
        return be.getBlockPos().equals(o.be.getBlockPos()) && be.getLevel() == o.be.getLevel();
    }

    @Override
    public void attach(IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(IComputerAccess computer) {
        computers.remove(computer);
    }

    @LuaFunction
    public final String ping() throws LuaException {
        requireValid();
        return "BRO, you pinged the G.F.B.S. CC I/O !!!";
    }

    @LuaFunction(mainThread = true)
    public final Object invokeApi(ILuaContext context, IComputerAccess computer, String name, IArguments args) throws LuaException {
        requireValid();
        if (name == null || name.isBlank()) throw new LuaException("API name is empty");
        ServerLevel level = requireServerLevel();
        return CCIoApiRegistry.invoke(level, be.getBlockPos(), computer, name, args == null ? new Object[0] : args.getAll());
    }

    @LuaFunction(mainThread = true)
    public final long invokeApiStream(ILuaContext context, IComputerAccess computer, IArguments args) throws LuaException {
        requireValid();

        if (args == null || args.count() <= 0) throw new LuaException("Expected: invokeApiStream(name, [options], ...)");
        String name = args.getString(0);

        int chunkSize;
        int argOffset = 1;

        if (args.count() >= 2) {
            Object maybeOpt = args.get(1);
            if (maybeOpt instanceof java.util.Map<?, ?> optMap) {
                Object cs = optMap.get("chunkSize");
                if (cs instanceof Number n) {
                    int v = n.intValue();
                    if (v <= 0) throw new LuaException("options.chunkSize must be > 0");
                    chunkSize = v;
                } else {
                    chunkSize = 256;
                }
                argOffset = 2;
            } else {
                chunkSize = 256;
            }
        } else {
            chunkSize = 256;
        }

        Object[] apiArgs = args.drop(argOffset).getAll();

        Object result = org.mirage.ccio.api.CCIoApiRegistry.invoke(
                requireServerLevel(),
                be.getBlockPos(),
                computer,
                name,
                apiArgs
        );

        long streamId = STREAM_SEQ.getAndIncrement();

        if (result instanceof org.mirage.ccio.api.ICCIoStreamResult streamResult) {
            Future<?> f = STREAM_EXEC.submit(() -> {
                AtomicInteger seq = new AtomicInteger();
                try {
                    streamResult.stream(chunk -> {
                        computer.queueEvent(STREAM_EVENT, streamId, chunk, seq.getAndIncrement(), false);
                    });
                    computer.queueEvent(STREAM_EVENT, streamId, "", seq, true);
                } catch (Exception e) {
                    computer.queueEvent(STREAM_EVENT, streamId, "[STREAM_ERROR] " + e.getMessage(), seq, true);
                } finally {
                    STREAM_TASKS.remove(streamId);
                }
            });
            STREAM_TASKS.put(streamId, f);
            return streamId;
        }

        final String text = (result == null) ? "null" : String.valueOf(result);

        Future<?> f = STREAM_EXEC.submit(() -> {
            try {
                int seq = 0;
                int len = text.length();
                int i = 0;

                if (len == 0) {
                    computer.queueEvent(STREAM_EVENT, streamId, "", seq, true);
                    return;
                }

                while (i < len) {
                    int end = Math.min(i + chunkSize, len);
                    String chunk = text.substring(i, end);
                    boolean done = (end >= len);

                    computer.queueEvent(STREAM_EVENT, streamId, chunk, seq, done);

                    seq++;
                    i = end;

                    Thread.yield();
                }
            } finally {
                STREAM_TASKS.remove(streamId);
            }
        });

        STREAM_TASKS.put(streamId, f);

        return streamId;
    }

    @LuaFunction
    public final boolean cancelStream(long streamId) throws LuaException {
        requireValid();
        Future<?> f = STREAM_TASKS.remove(streamId);
        if (f == null) return false;
        return f.cancel(true);
    }

    @LuaFunction(mainThread = true)
    public final String runCommand(String command) throws LuaException {
        requireValid();

        if (command == null || command.isBlank()) {
            throw new LuaException("Command cannot be empty");
        }

        MinecraftServer server = requireServer();
        ServerLevel level = requireServerLevel();

        Vec3 pos = Vec3.atCenterOf(be.getBlockPos());
        CommandSourceStack source = new CommandSourceStack(
                Objects.requireNonNull(server.createCommandSourceStack().getEntity()),
                pos,
                Vec2.ZERO,
                level,
                2,
                "CCIoBridge",
                Component.literal("CCIoBridge"),
                server,
                null
        );

        try {
            int result = server.getCommands().performPrefixedCommand(source, command);
            return "Command executed with result: " + result;
        } catch (Exception e) {
            throw new LuaException("Failed to execute command: " + e.getMessage());
        }
    }
}
