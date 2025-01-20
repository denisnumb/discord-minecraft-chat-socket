package com.denisnumb.minecraft_socket_mod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = MinecraftSocketMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> CLIENT_IP = BUILDER
            .comment("Specify the IP address of the server on which client is running")
            .define("clientIp", "127.0.0.1");

    private static final ForgeConfigSpec.IntValue SOCKET_SERVER_PORT = BUILDER
            .comment("Port for the socket server receiving messages")
            .defineInRange("socketServerPort", 1337, 0, 65535);

    static final ForgeConfigSpec SPEC = BUILDER.build();
    public static String clientIp;
    public static int socketServerPort;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        clientIp = CLIENT_IP.get();
        socketServerPort = SOCKET_SERVER_PORT.get();
    }
}
