package com.denisnumb.discord_chat_mod;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.discordSocket;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.server;

@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID)
public class ChatEvents {
    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) throws IOException {
        discordSocket.sendMessageToDiscord(
                new ChatMessage(
                        MessageType.Message,
                        event.getPlayer().getName().getString(),
                        event.getRawText()
                ).getJson()
        );
    }

    @SubscribeEvent
    public static void onPlayerDieEvent(LivingDeathEvent event) throws IOException {
        if (!(event.getEntity() instanceof Player))
            return;

        discordSocket.sendMessageToDiscord(
                new ChatMessage(
                        MessageType.PlayerDie,
                        event.getEntity().getName().getString(),
                        event.getSource().getLocalizedDeathMessage(event.getEntity()).getString()
                ).getJson()
        );
    }

    @SubscribeEvent
    public static void onPlayerJoinEvent(PlayerEvent.PlayerLoggedInEvent event) throws IOException {
        joinLeaveEvent(event);
    }

    @SubscribeEvent
    public static void onPlayerLeaveEvent(PlayerEvent.PlayerLoggedOutEvent event) throws IOException {
        joinLeaveEvent(event);
    }

    private static void joinLeaveEvent(PlayerEvent event) throws IOException {
        String message = event instanceof PlayerEvent.PlayerLoggedInEvent
                ? "зашел на сервер"
                : "вышел с сервера";

        MessageType messageType = event instanceof PlayerEvent.PlayerLoggedInEvent
                ? MessageType.PlayerJoin
                : MessageType.PlayerLeft;

        discordSocket.sendMessageToDiscord(
                new ChatMessage(
                        messageType,
                        event.getEntity().getName().getString(),
                        message
                ).getJson()
        );
    }

    public static void sendChatMessage(String message){
        if (server.getPlayerCount() == 0)
            return;
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(),
                message
        );
    }
}
