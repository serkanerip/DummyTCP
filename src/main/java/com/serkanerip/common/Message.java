package com.serkanerip.common;

import java.util.UUID;

public record Message(
    UUID conversationUUID,
    byte[] data
) {
    public Integer contentLength() {
        return data.length;
    }
}
