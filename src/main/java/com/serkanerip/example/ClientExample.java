package com.serkanerip.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.serkanerip.client.TCPClient;

public class ClientExample
{
    public static void main( String[] args ) throws InterruptedException {
        var client = new TCPClient(
            "localhost", 8080,
            (msg) -> {
                var msgDataStr = new String(msg.data(), StandardCharsets.UTF_8);
                System.out.println("New message from server: "  + msgDataStr );
            }
        );
        try {
            client.connect();
            for (int i = 0; i < 1000; i++) {
                client.send("Hi, server!".getBytes(StandardCharsets.UTF_8));
                Thread.sleep(1000);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        client.disconnect();
    }
}
