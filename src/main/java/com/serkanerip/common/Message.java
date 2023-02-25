package com.serkanerip.common;

import java.util.UUID;

public class Message {
    private Integer contentLength;

    private UUID conversationUUID;

    private byte[] data;

    public Message(UUID conversationUUID, byte[] data) {
        this.conversationUUID = conversationUUID;
        this.data = data;
        this.contentLength = data.length;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public UUID getConversationUUID() {
        return conversationUUID;
    }

    public byte[] getData() {
        return data;
    }

}
