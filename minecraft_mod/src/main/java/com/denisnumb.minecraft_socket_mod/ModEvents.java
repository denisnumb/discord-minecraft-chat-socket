package com.denisnumb.minecraft_socket_mod;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.io.IOException;

import static com.denisnumb.minecraft_socket_mod.MinecraftSocketMod.serverSocket;
import static com.denisnumb.minecraft_socket_mod.MinecraftSocketMod.server;

@EventBusSubscriber(modid = MinecraftSocketMod.MODID)
public class ModEvents {
    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) throws IOException {
        serverSocket.sendMessageToClient(
                new ChatMessage(
                        MessageType.Message,
                        event.getPlayer().getName().getString(),
                        event.getRawText()
                )
        );
    }

    @SubscribeEvent
    public static void onPlayerDieEvent(LivingDeathEvent event) throws IOException {
        if (!(event.getEntity() instanceof Player))
            return;

        serverSocket.sendMessageToClient(
                new ChatMessage(
                        MessageType.PlayerDie,
                        event.getEntity().getName().getString(),
                        event.getSource().getLocalizedDeathMessage(event.getEntity()).getString()
                )
        );
    }

    @SubscribeEvent
    public static void onAdvancementMade(AdvancementEvent.AdvancementEarnEvent event) throws IOException {
        if (event.getAdvancement().value().display().isEmpty())
            return;

        DisplayInfo displayInfo = event.getAdvancement().value().display().get();
        serverSocket.sendMessageToClient(
                new ChatMessage(
                        MessageType.AdvancementMade,
                        event.getEntity().getName().getString(),
                        displayInfo.getTitle().getString()
                )
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

        serverSocket.sendMessageToClient(
                new ChatMessage(
                        messageType,
                        event.getEntity().getName().getString(),
                        message
                )
        );
    }

    public static void executeRequest(String request){
        if (server.getPlayerCount() == 0)
            return;
        executeServerCommand(request);
    }

    private static void executeServerCommand(String command){
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack(),
                command
        );
    }
}