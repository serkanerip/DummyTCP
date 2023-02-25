package com.serkanerip.server;

import java.nio.ByteBuffer;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientContext {

    private final ClientAddress clientAddress;

    private final Queue<ByteBuffer> sendQueue;

    private ByteBuffer readBuffer;

    public ClientContext(ClientAddress clientAddress) {
        this.clientAddress = clientAddress;
        this.sendQueue = new ConcurrentLinkedQueue<>();
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public void setReadBuffer(ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }

    public Queue<ByteBuffer> getSendQueue() {
        return sendQueue;
    }

    public ClientAddress getClientAddress() {
        return clientAddress;
    }
}
