package com.denisnumb.discord_chat_mod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> DISCORD_BOT_SERVER_IP = BUILDER
            .comment("Specify the IP address of the server on which the discord bot is running")
            .define("discordBotServerIp", "127.0.0.1");

    private static final ForgeConfigSpec.IntValue SOCKET_SERVER_PORT = BUILDER
            .comment("Port for the socket server receiving messages")
            .defineInRange("socketServerPort", 1337, 0, 65535);

    static final ForgeConfigSpec SPEC = BUILDER.build();
    public static String discordBotServerIp;
    public static int socketServerPort;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        discordBotServerIp = DISCORD_BOT_SERVER_IP.get();
        socketServerPort = SOCKET_SERVER_PORT.get();
    }
}
