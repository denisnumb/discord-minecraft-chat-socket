package com.denisnumb.minecraft_socket_mod;

import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.io.IOException;

@Mod(MinecraftSocketMod.MODID)
public class MinecraftSocketMod
{
    public static final String MODID = "minecraft_socket_mod";
    public static MinecraftServer server;
    public static ServerSocket serverSocket;

    public MinecraftSocketMod(IEventBus modEventBus, ModContainer modContainer)
    {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) throws IOException {
        server = event.getServer();
        serverSocket = new ServerSocket(Config.socketServerPort, Config.clientIp);
        serverSocket.start();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) throws IOException {
        serverSocket.close();
    }
}
