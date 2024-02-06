package com.denisnumb.discord_chat_mod;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;

@Mod(DiscordChatMod.MODID)
public class DiscordChatMod
{
    public static final String MODID = "discord_chat_mod";
    public static MinecraftServer server;
    public static DiscordSocket discordSocket;

    public DiscordChatMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) throws IOException {
        server = event.getServer();
        discordSocket = new DiscordSocket(Config.socketServerPort, Config.discordBotServerIp);
        discordSocket.start();
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) throws IOException {
        discordSocket.close();
    }
}
