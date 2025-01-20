package com.denisnumb.minecraft_socket_mod;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;

@Mod(MinecraftSocketMod.MODID)
public class MinecraftSocketMod
{
    public static final String MODID = "minecraft_socket_mod";
    public static MinecraftServer server;
    public static ServerSocket serverSocket;

    public MinecraftSocketMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
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
