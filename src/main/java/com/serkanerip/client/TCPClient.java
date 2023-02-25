package com.serkanerip.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import com.serkanerip.common.Message;
import com.serkanerip.common.MessageSerializer;

public class TCPClient {
    private final String serverIp;

    private final Integer serverPort;

    private SocketChannel channel;

    private boolean isConnected;

    private Selector selector;

    private final Queue<ByteBuffer> writeQueue =
        new ConcurrentLinkedQueue<>();

    private ByteBuffer readBuffer;

    private final Consumer<Message> onMessage;

    private boolean writeMode;

    private final Thread runThread;

    public TCPClient(
        String serverIp, Integer serverPort,
        Consumer<Message> onMessage
    ) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.onMessage = onMessage;
        runThread = new Thread(this::run);
    }

    public void connect() throws IOException {
        if (isConnected) {
            throw new IllegalStateException("Already connected!");
        }
        selector = Selector.open();
        initializeSocket();
        isConnected = true;
        runThread.start();
    }

    private void run() {
        while (!Thread.interrupted()) {
            try {
                selector.select();
                var iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    var key = iterator.next();
                    if (!key.isValid()) continue;
                    if (key.isReadable()) {
                        read();
                    } else if (key.isWritable()) {
                        write(key);
                    }
                    iterator.remove();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void write(SelectionKey key) throws IOException {
        var wChannel = (SocketChannel) key.channel();
        var buf = writeQueue.peek();
        wChannel.write(buf);
        assert buf != null;
        if (buf.remaining() > 0) {
            System.out.println("Couldn't write all data at once!");
        } else {
            writeQueue.poll();
        }
        if (writeQueue.isEmpty()) {
            writeMode = false;
            wChannel.register(selector, SelectionKey.OP_READ);
        }
    }

    private void read() throws IOException {
        ByteBuffer tmpBuffer;
        if (readBuffer == null) {
            tmpBuffer = ByteBuffer.allocate(65000);
            var read = channel.read(tmpBuffer);
            if (read == -1) {
                disconnect();
                throw new RuntimeException("Server disconnected can't read!");
            }
            tmpBuffer.flip();
            var len = tmpBuffer.getInt();
            readBuffer = ByteBuffer.allocate(len + 20);
            tmpBuffer.position(0);
            readBuffer.put(tmpBuffer);
        } else {
            channel.read(readBuffer);
        }

        if (readBuffer.remaining() == 0) {
            readBuffer.position(0);
            var msg = MessageSerializer.deserialize(readBuffer);
            onMessage.accept(msg);
            readBuffer = null;
        }
    }

    private void initializeSocket() throws IOException {
        channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.connect(new InetSocketAddress(serverIp, serverPort));
        while (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        channel.register(selector, SelectionKey.OP_READ);
    }

    public void disconnect() {
        if (!isConnected) {
            throw new RuntimeException("There is no connection yet!");
        }
        try {
            runThread.interrupt();
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void send(byte[] data) throws IOException {
        var uuid = UUID.randomUUID();
        var msg = new Message(uuid, data);
        writeQueue.add(MessageSerializer.serialize(msg));
        if (!writeMode) {
            writeMode = true;
            channel.register(selector, SelectionKey.OP_WRITE);
            selector.wakeup();
        }
    }

}
