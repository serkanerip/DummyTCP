package com.serkanerip.common;

import java.nio.ByteBuffer;
import java.util.UUID;

public class MessageSerializer {

    public static ByteBuffer serialize(Message msg) {
        var bb = ByteBuffer.allocate(20 + msg.getContentLength());
        bb.putInt(msg.getContentLength());
        bb.put(asBytes(msg.getConversationUUID()));
        bb.put(msg.getData());
        return bb;
    }

    public static Message deserialize(ByteBuffer buffer) {
        var len = buffer.getInt();
        var msb = buffer.getLong();
        var lsb = buffer.getLong();
        var barr = new byte[len];
        for (int i = 0; i < len; i++) {
            barr[i] = buffer.get();
        }
        return new Message(
            new UUID(msb, lsb),
            barr
        );
    }

    private static byte[] asBytes(UUID uuid) {
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();
    }
}
