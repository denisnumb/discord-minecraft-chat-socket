package com.denisnumb.minecraft_socket_mod;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

enum MessageType{
    @SerializedName("0")
    Message,
    @SerializedName("1")
    PlayerJoin,
    @SerializedName("2")
    PlayerLeft,
    @SerializedName("3")
    PlayerDie,
    @SerializedName("4")
    AdvancementMade
}

public record ChatMessage(
        @SerializedName("message_type")
        MessageType type,
        @SerializedName("player_name")
        String userName,
        @SerializedName("message")
        String message
) {
    public String getJson() {
        return new GsonBuilder().create().toJson(this);
    }
}