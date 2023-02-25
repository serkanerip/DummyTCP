package com.serkanerip.example;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;

import com.serkanerip.common.Message;
import com.serkanerip.server.ClientAddress;
import com.serkanerip.server.TCPServer;

public class ServerExample {

    private static TCPServer server;

    public static void main(String[] args) throws IOException {
        server = new TCPServer(ServerExample::onMessage, ServerExample::onNewClient);
        server.start();
    }

    private static void onNewClient(ClientAddress clientAddress) {
        try {
            server.send(clientAddress, "Welcome, client!".getBytes(StandardCharsets.UTF_8));
        } catch (ClosedChannelException e) {
            throw new RuntimeException(e);
        }
    }

    private static void onMessage(ClientAddress clientAddress, Message msg) {
        var dataStr = new String(msg.data(), StandardCharsets.UTF_8);
        System.out.println("[%s:%s] %s".formatted(
            clientAddress.ip(), clientAddress.port(),
            dataStr
        ));
    }
}
