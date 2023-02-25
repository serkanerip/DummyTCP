package com.serkanerip.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.serkanerip.common.Message;
import com.serkanerip.common.MessageSerializer;

public class TCPServer {

    private Selector selector;

    private final Map<SocketChannel, ConnectedClient> connections =
        new ConcurrentHashMap<>();

    private final Consumer<Message> onNewMessage;

    public TCPServer(Consumer<Message> onNewMessage) {
        this.onNewMessage = onNewMessage;
    }

    public void start() throws IOException {
        var ssc = ServerSocketChannel.open();
        ssc.bind(new InetSocketAddress("localhost", 8080));
        ssc.configureBlocking(false);
        selector = Selector.open();
        ssc.register(selector, SelectionKey.OP_ACCEPT);
        while (true) {
            selector.select();
            for (var itKeys = selector.selectedKeys().iterator(); itKeys.hasNext(); ) {
                SelectionKey key = itKeys.next();
                itKeys.remove();
                if (key.isValid()) {
                    if (key.isAcceptable()) { // someone connected to our serversocketchannel
                        accept(key);
                    }
                    if (key.isReadable()) {
                        read(key);
                    }
                    if (key.isWritable()) {
                        write(key);
                    }
                }
            }
        }
    }

    private void write(SelectionKey key) throws IOException {
//        SocketChannel socket = (SocketChannel) key.channel();
//        ByteBuffer poll = pendingData.get(socket).poll();
//        if (poll != null) socket.write(poll);
//        socket.register(key.selector(), SelectionKey.OP_READ);
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        var buffer = ByteBuffer.allocate(65000);
        int read = socket.read(buffer);
        if (read == -1) {
            connections.remove(socket);
            return;
        }
        buffer.flip();
        var connectedClient = connections.get(socket);
        System.out.println("rem:" + buffer.remaining());
        while (buffer.remaining() != 0) {
            if (connectedClient.getReadBuffer() == null) {
                var length = buffer.getInt();
                // System.out.println(length);
                if (length ==0) {
                    throw new RuntimeException("len 0");
                }
                connectedClient.setReadBuffer(
                    ByteBuffer.allocate(
                        length + 20
                    )
                );
                connectedClient.getReadBuffer().position(0);
                buffer.position(0);
            }
            var readBuffer = connectedClient.getReadBuffer();
//            System.out.println("readbuf lim=%d,cap=%d,rem=%d,pos=%d".formatted(
//                readBuffer.limit(), readBuffer.capacity(), readBuffer.remaining(),
//                readBuffer.position()
//            ));
            var readCount = Math.min(buffer.remaining(), readBuffer.remaining());
            // System.out.println("read count=" + readCount);

            for (int i = 0; i < readCount; i++) {
                readBuffer.put(buffer.get());
            }
        }
//        System.out.println("read remaining = " +
//            connectedClient.getReadBuffer().remaining());
        if (connectedClient.getReadBuffer().remaining() == 0) {
            connectedClient.getReadBuffer().position(0);
            var msg = MessageSerializer.deserialize(
                connectedClient.getReadBuffer()
            );
            onNewMessage.accept(msg);
            connectedClient.setReadBuffer(null);
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        var sc = ssc.accept(); // nonblocking, never null
        System.out.println("Connection from " + sc);
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);
        connections.put(sc, new ConnectedClient(sc));
    }
}
