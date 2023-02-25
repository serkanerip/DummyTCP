package com.serkanerip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.serkanerip.server.TCPServer;

public class ServerApp {
    public static void main(String[] args) throws IOException {
        var server = new TCPServer((msg) -> {
            var dataStr= new String(msg.getData(), StandardCharsets.UTF_8);
            System.out.println(dataStr);
        });
        server.start();
    }
}
