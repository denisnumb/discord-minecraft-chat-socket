package com.denisnumb.minecraft_socket_mod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;


@EventBusSubscriber(modid = MinecraftSocketMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> CLIENT_IP = BUILDER
            .comment("Specify the IP address of the server on which client is running")
            .define("clientIp", "127.0.0.1");

    private static final ModConfigSpec.IntValue SOCKET_SERVER_PORT = BUILDER
            .comment("Port for the socket server receiving messages")
            .defineInRange("socketServerPort", 1337, 0, 65535);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static boolean logDirtBlock;
    public static String clientIp;
    public static int socketServerPort;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        clientIp = CLIENT_IP.get();
        socketServerPort = SOCKET_SERVER_PORT.get();
    }
}
