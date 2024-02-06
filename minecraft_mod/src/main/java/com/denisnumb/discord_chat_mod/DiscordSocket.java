package com.denisnumb.discord_chat_mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class DiscordSocket extends BaseSocket {
    private DataInputStream in;
    private DataOutputStream out;
    private Socket discordBot;
    private boolean discordBotConnected = false;
    private boolean listen = true;
    private final ServerSocket receiver;
    private final String discordBotServerIp;
    private final Logger LOGGER = LogUtils.getLogger();

    public DiscordSocket(int port, String discordBotServerIp) throws IOException {
        receiver = new ServerSocket(port);
        this.discordBotServerIp = discordBotServerIp;
        LOGGER.info("DiscordSocket Initialized");
    }

    public void start() {
        new Thread(() -> {
            try {
                waitDiscordBot();
            } catch (IOException ignored) {}
        }).start();
    }

    public void close() throws IOException {
        listen = false;
        receiver.close();
        closeDiscordBotConnection();
    }

    private void waitDiscordBot() throws IOException {
        discordBotConnected = acceptSender();
        if (!discordBotConnected)
            waitDiscordBot();
        in = new DataInputStream(discordBot.getInputStream());
        out = new DataOutputStream(discordBot.getOutputStream());
        LOGGER.info("Discord Bot Connected: " + discordBot.toString());
        receiveDiscordRequests();
    }

    private boolean acceptSender() throws IOException {
        Socket client = receiver.accept();

        if (!client.getInetAddress().getHostAddress().equals(discordBotServerIp)){
            LOGGER.warn("An attempt was made to connect from an invalid address: " + client);
            client.close();
            return false;
        }
        discordBot = client;
        return true;
    }

    private void closeDiscordBotConnection() throws IOException {
        if (discordBotConnected){
            discordBot.close();
            LOGGER.warn("Discord Bot Disconnected");
            discordBotConnected = false;
            if (listen)
                waitDiscordBot();
        }
    }

    private void receiveDiscordRequests() throws IOException {
        while (listen) {
            try {
                ModEvents.executeDiscordRequest(new String(receiveData(in), StandardCharsets.UTF_8));
            } catch (Exception e){
                closeDiscordBotConnection();
            }
        }
    }

    public void sendMessageToDiscord(ChatMessage message) throws IOException {
        if (!discordBotConnected)
            return;
        try {
            sendData(out, message.getJson().getBytes(StandardCharsets.UTF_8));
        } catch (SocketException e){
            closeDiscordBotConnection();
        }
    }
}

class BaseSocket{
    public byte[] receiveData(DataInputStream in) throws IOException {
        short partLength = ByteBuffer.wrap(in.readNBytes(2)).getShort();
        byte[] data = new byte[partLength];
        receivePart(in, data);
        return data;
    }

    public void sendData(DataOutputStream out, byte[] data) throws IOException {
        out.writeShort(data.length);
        out.write(data);
    }

    private void receivePart(DataInputStream in, byte[] data) throws IOException {
        int toReceive = data.length;
        int received = 0;
        int len = Math.min(toReceive, 2048);

        while (received < toReceive){
            in.readNBytes(data, received, len);
            received += len;
            len = Math.min(toReceive - received, 2048);
        }
    }
}