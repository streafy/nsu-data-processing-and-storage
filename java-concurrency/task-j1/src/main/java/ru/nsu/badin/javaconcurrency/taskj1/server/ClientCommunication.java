package ru.nsu.badin.javaconcurrency.taskj1.server;

import ru.nsu.badin.javaconcurrency.taskj1.server.models.GenerationRequest;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.GenerationResponse;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.KeysWithCertificate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ClientCommunication {
    private final int port;
    private final BlockingQueue<GenerationRequest> generationQueue;
    private final BlockingQueue<GenerationResponse> responseQueue;

    private final ConcurrentHashMap<String, KeysWithCertificate> generationResultStorage;
    private final ConcurrentHashMap<String, CompletableFuture<KeysWithCertificate>> pendingGenerations = new ConcurrentHashMap<>();

    public ClientCommunication(int port, BlockingQueue<GenerationRequest> generationQueue, BlockingQueue<GenerationResponse> responseQueue, ConcurrentHashMap<String, KeysWithCertificate> generationResultStorage) {
        this.port = port;
        this.generationQueue = generationQueue;
        this.responseQueue = responseQueue;
        this.generationResultStorage = generationResultStorage;
    }

    public void start() {
        System.out.println("Starting client communication");
        new Thread(this::handleClientCommunication).start();
        new Thread(this::handleClientResponse).start();
    }

    public void handleClientCommunication() {
        try (var serverSocketChannel = ServerSocketChannel.open();
             var selector = Selector.open()) {
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Accepting clients on port " + port);

            while (true) {
                selector.select();
                for (var key : selector.selectedKeys()) {
                    if (key.isAcceptable() && key.channel() instanceof ServerSocketChannel channel) {
                        handleAccept(channel, selector);
                    }
                    if (key.isReadable() && key.channel() instanceof SocketChannel client) {
                        handleRead(key, client);
                    }
                }
                selector.selectedKeys().clear();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(ServerSocketChannel channel, Selector selector) throws IOException {
        var client = channel.accept();
        System.out.println("\n\nConnected: " + client.getRemoteAddress());
        client.configureBlocking(false);
        client.register(selector, SelectionKey.OP_READ, ByteBuffer.allocate(256));
    }

    private void handleRead(SelectionKey key, SocketChannel client) {
        try {
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            var bytesRead = client.read(buffer);

            if (bytesRead == -1) {
                client.close();
                return;
            }

            buffer.flip();
            var clientName = new String(buffer.array(), buffer.position(), bytesRead);
            System.out.println("Connected client name: " + clientName);
            buffer.clear();

            handleGenerationRequest(clientName, client);
        } catch (IOException e) {
            System.err.println("Client connection error: " + e.getMessage());
            key.cancel();
            try {
                client.close();
            } catch (IOException closeException) {
                System.err.println("Error closing client channel: " + closeException.getMessage());
            }
        }
    }

    private void handleGenerationRequest(String clientName, SocketChannel clientChannel) {
        KeysWithCertificate existingResult = generationResultStorage.get(clientName);
        if (existingResult != null) {
            System.out.println("Already generated result for " + clientName);
            try {
                responseQueue.put(new GenerationResponse(clientChannel, existingResult));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }

        System.out.println("Adding generation request for " + clientName + " to queue");
        CompletableFuture<KeysWithCertificate> future = pendingGenerations.computeIfAbsent(clientName, k -> {
            CompletableFuture<KeysWithCertificate> newFuture = new CompletableFuture<>();
            try {
                generationQueue.put(new GenerationRequest(clientName, newFuture));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                newFuture.completeExceptionally(e);
            }
            return newFuture;
        });

        future.thenAccept(keysWithCertificate -> {
            try {
                responseQueue.put(new GenerationResponse(clientChannel, keysWithCertificate));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void handleClientResponse() {
        System.out.println("Started client response thread");
        while (true) {
            try {
                GenerationResponse response = responseQueue.take();
                System.out.println("Sending response");
                sendGenerationResponseToClient(response.channel(), response.keysWithCertificate());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                System.err.println("Error sending response: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void sendGenerationResponseToClient(SocketChannel clientChannel, KeysWithCertificate keysWithCertificate) throws Exception {
        if (!clientChannel.isOpen() || !clientChannel.isConnected()) {
            System.out.println("Client has disconnected, terminating send response");
            return;
        }

        byte[] privateKeyBytes = keysWithCertificate.keyPair().getPrivate().getEncoded();
        byte[] certificateBytes = keysWithCertificate.certificate().getEncoded();

        ByteBuffer response = ByteBuffer.allocate(8 + privateKeyBytes.length + certificateBytes.length);
        response.putInt(privateKeyBytes.length);
        response.put(privateKeyBytes);
        response.putInt(certificateBytes.length);
        response.put(certificateBytes);
        response.flip();

        while (response.hasRemaining()) {
            clientChannel.write(response);
        }

        System.out.println("Data was sent to client");
    }
}
