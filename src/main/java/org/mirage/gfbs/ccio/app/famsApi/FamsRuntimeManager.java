package org.mirage.gfbs.ccio.app.famsApi;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class FamsRuntimeManager {
    private static final ConcurrentHashMap<MinecraftServer, FamsRuntime> RUNTIMES = new ConcurrentHashMap<>();

    private FamsRuntimeManager(){}

    public static FamsRuntime get(ServerLevel level){
        Objects.requireNonNull(level, "level");
        MinecraftServer server = level.getServer();
        return RUNTIMES.computeIfAbsent(server, s -> new FamsRuntime());
    }

    public static Optional<FamsRuntime> tryGet(ServerLevel level){
        if(level==null) return Optional.empty();
        MinecraftServer server = level.getServer();
        return Optional.ofNullable(RUNTIMES.get(server));
    }

    public static int computerId(IComputerAccess computer) throws LuaException{
        if(computer==null) throw new LuaException("computer is null");
        return computer.getID();
    }
}
