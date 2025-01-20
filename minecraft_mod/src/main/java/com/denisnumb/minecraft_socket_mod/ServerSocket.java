package com.denisnumb.minecraft_socket_mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class ServerSocket extends BaseSocket {
    private DataInputStream in;
    private DataOutputStream out;
    private Socket client;
    private boolean isClientConnected = false;
    private boolean listen = true;
    private final java.net.ServerSocket receiver;
    private final String clientIp;
    private final Logger LOGGER = LogUtils.getLogger();

    public ServerSocket(int port, String clientIp) throws IOException {
        receiver = new java.net.ServerSocket(port);
        this.clientIp = clientIp;
        LOGGER.info("ServerSocket Initialized");
    }

    public void start() {
        new Thread(() -> {
            try {
                waitClient();
            } catch (IOException ignored) {}
        }).start();
    }

    public void close() throws IOException {
        listen = false;
        receiver.close();
        closeClientConnection();
    }

    private void waitClient() throws IOException {
        isClientConnected = acceptSender();
        if (!isClientConnected)
            waitClient();
        in = new DataInputStream(client.getInputStream());
        out = new DataOutputStream(client.getOutputStream());
        LOGGER.info("Client Connected: " + client.toString());
        receiveRequests();
    }

    private boolean acceptSender() throws IOException {
        Socket client = receiver.accept();

        if (!client.getInetAddress().getHostAddress().equals(clientIp)){
            LOGGER.warn("An attempt was made to connect from an invalid address: " + client);
            client.close();
            return false;
        }
        this.client = client;
        return true;
    }

    private void closeClientConnection() throws IOException {
        if (isClientConnected){
            client.close();
            LOGGER.warn("Client Disconnected");
            isClientConnected = false;
            if (listen)
                waitClient();
        }
    }

    private void receiveRequests() throws IOException {
        while (listen) {
            try {
                ModEvents.executeRequest(new String(receiveData(in), StandardCharsets.UTF_8));
            } catch (Exception e){
                closeClientConnection();
            }
        }
    }

    public void sendMessageToClient(ChatMessage message) throws IOException {
        if (!isClientConnected)
            return;
        try {
            sendData(out, message.getJson().getBytes(StandardCharsets.UTF_8));
        } catch (SocketException e){
            closeClientConnection();
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