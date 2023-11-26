package com.denisnumb.discord_chat_mod;

enum MessageType{
    Message, PlayerJoin, PlayerLeft, PlayerDie
}

public class ChatMessage {
    public final String userName;
    public final String message;
    public final MessageType messageType;

    public ChatMessage(MessageType type, String userName, String message){
        this.messageType = type;
        this.userName = userName;
        this.message = message;
    }

    public String getJson(){
        return String.format(
                "{\"message_type\": %d, \"player_name\": \"%s\", \"message\": \"%s\"}",
                messageType.ordinal(),
                userName,
                message.replace("\\", "\\\\")
        );
    }
}
