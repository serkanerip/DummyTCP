package com.serkanerip.server;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ConnectedClient {

    private SocketChannel channel;

    private ByteBuffer sendBuffer;

    private ByteBuffer readBuffer;

    public ConnectedClient(SocketChannel channel) {
        this.channel = channel;
    }

    public ByteBuffer getSendBuffer() {
        return sendBuffer;
    }

    public void setSendBuffer(ByteBuffer sendBuffer) {
        this.sendBuffer = sendBuffer;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public void setReadBuffer(ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }
}
