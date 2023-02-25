package com.serkanerip;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.serkanerip.client.TCPClient;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException, InterruptedException {
        var client = new TCPClient("localhost", 8080, 10);
        client.connect();

        for (int i = 0; i < 1000; i++) {
            client.send("Selam Server".getBytes(StandardCharsets.UTF_8));
            Thread.sleep(100);
        }
    }
}
