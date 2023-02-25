package com.serkanerip.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.serkanerip.common.Message;
import com.serkanerip.common.MessageSerializer;

public class TCPServer {

    private final Consumer<ClientAddress> onNewClient;

    private Selector selector;

    private final Map<ClientAddress, SocketChannel> clientAddressSocketChannelMap =
        new ConcurrentHashMap<>();

    private final Map<SocketChannel, ClientContext> channelContext =
        new ConcurrentHashMap<>();

    private final BiConsumer<ClientAddress, Message> onNewMessage;

    public TCPServer(
        BiConsumer<ClientAddress, Message> onNewMessage,
        Consumer<ClientAddress> onNewClient
    ) {
        this.onNewMessage = onNewMessage;
        this.onNewClient = onNewClient;
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
                if (!key.isValid()) continue;

                if (key.isAcceptable()) { // someone connected to our serversocketchannel
                    accept(key);
                }else if (key.isReadable()) {
                    read(key);
                }else if (key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    public void send(ClientAddress clientAddress, byte[] data) throws ClosedChannelException {
        var sock = clientAddressSocketChannelMap.get(clientAddress);
        if (sock == null) {
            throw new IllegalStateException("Client(%s) doesn't exists!".formatted(
                clientAddress
            ));
        }
        var ctx = channelContext.get(sock);
        var msg = new Message(UUID.randomUUID(), data);
        ctx.getSendQueue().add(MessageSerializer.serialize(msg));
        sock.register(selector, SelectionKey.OP_WRITE);
    }

    private void write(SelectionKey key) throws IOException {
        var socket = (SocketChannel) key.channel();
        var ctx = channelContext.get(socket);
        var buff = ctx.getSendQueue().peek();
        assert buff != null;
        socket.write(buff);
        if (buff.remaining() == 0) {
            ctx.getSendQueue().remove();
            socket.register(key.selector(), SelectionKey.OP_READ);
        }
    }

    private void onDisconnect(SocketChannel sock) {
        System.out.println("Client Disconnected! " + sock);
        try {
            var ctx = channelContext.get(sock);
            sock.close();
            channelContext.remove(sock);
            clientAddressSocketChannelMap.remove(ctx.getClientAddress());
        } catch (IOException e) {
            System.out.println("Error on closing socket: " + e.getMessage());
        }
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socket = (SocketChannel) key.channel();
        var buffer = ByteBuffer.allocate(65000);
        int read = socket.read(buffer);
        if (read == -1) {
            onDisconnect(socket);
            return;
        }
        buffer.flip();
        var connectedClient = channelContext.get(socket);
        while (buffer.remaining() != 0) {
            if (connectedClient.getReadBuffer() == null) {
                var length = buffer.getInt();
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
            var readCount = Math.min(buffer.remaining(), readBuffer.remaining());

            for (int i = 0; i < readCount; i++) {
                readBuffer.put(buffer.get());
            }
        }
        if (connectedClient.getReadBuffer().remaining() == 0) {
            connectedClient.getReadBuffer().position(0);
            var msg = MessageSerializer.deserialize(
                connectedClient.getReadBuffer()
            );
            onNewMessage.accept(connectedClient.getClientAddress(), msg);
            connectedClient.setReadBuffer(null);
        }
    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
        var sc = ssc.accept(); // nonblocking, never null
        System.out.println("Connection from " + sc);
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ);

        var remoteAddr = sc.getRemoteAddress()
            .toString().replace("/", "")
            .split(":");

        var ca = new ClientAddress(remoteAddr[0], remoteAddr[1]);
        channelContext.put(sc, new ClientContext(ca));
        clientAddressSocketChannelMap.put(
            ca, sc
        );
        if (onNewClient != null) {
            onNewClient.accept(ca);
        }
    }
}
