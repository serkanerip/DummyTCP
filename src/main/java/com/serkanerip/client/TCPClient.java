package com.serkanerip.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.serkanerip.common.Message;
import com.serkanerip.common.MessageSerializer;

public class TCPClient {
    private final String serverIp;

    private final Integer serverPort;

    private final Integer connectionTimeoutSeconds;

    private Socket socket;

    private boolean isConnected;

    private OutputStream os;

    private InputStream is;

    private ExecutorService pool = Executors.newFixedThreadPool(1);

    public TCPClient(String serverIp, Integer serverPort, Integer connectionTimeoutSeconds) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public void connect() throws IOException {
        if (isConnected) {
            throw new IllegalStateException("Already connected!");
        }
        socket = new Socket();
        socket.setKeepAlive(true);
        socket.connect(
            new InetSocketAddress(serverIp, serverPort),
            connectionTimeoutSeconds
        );
        is = socket.getInputStream();
        os = socket.getOutputStream();
        isConnected = true;
        pool.submit(this::read);
    }

    public synchronized void send(byte[] data) throws IOException {
        var uuid = UUID.randomUUID();
        var msg = new Message(uuid, data);
        os.write(MessageSerializer.serialize(msg).array());
    }

    private void read() {
        int b = 0;
        while (true) {
            try {
                if (!((b = is.read()) != -1))  break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
