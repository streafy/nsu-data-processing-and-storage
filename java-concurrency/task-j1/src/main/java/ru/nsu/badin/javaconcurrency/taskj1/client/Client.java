package ru.nsu.badin.javaconcurrency.taskj1.client;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Client {
    private final String name;
    private final String serverAddress;
    private final int serverPort;
    private final int delay;
    private final boolean exitEarly;

    public Client(String name, String serverAddress, int serverPort, int delay, boolean exitEarly) {
        this.name = name;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.delay = delay;
        this.exitEarly = exitEarly;
    }

    public void start() throws Exception {
        System.out.println(
                "Client started with parameters:\n" +
                        "name = " + name + "\n" +
                        "server address = " + serverAddress + "\n" +
                        "server port = " + serverPort + "\n" +
                        "delay = " + delay + "\n" +
                        "exit early = " + exitEarly + "\n"
        );

        try (var serverChannel = SocketChannel.open()) {
            serverChannel.connect(new InetSocketAddress(serverAddress, serverPort));
            System.out.println("Connection to " + serverAddress + ":" + serverPort + " established");

            ByteBuffer buffer = ByteBuffer.wrap((name + "\0").getBytes(StandardCharsets.US_ASCII));
            while (buffer.hasRemaining()) {
                serverChannel.write(buffer);
            }
            System.out.println("Sent name to server");

            if (delay > 0) {
                System.out.println("Waiting " + delay + " seconds before reading response...");
                Thread.sleep(delay * 1000L);
            }

            if (exitEarly) {
                System.out.println("Exiting before reading response");
                serverChannel.close();
                System.exit(0);
            }

            System.out.println("Reading response...");

            ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
            readFully(serverChannel, lengthBuffer);
            lengthBuffer.flip();
            int privateKeyLength = lengthBuffer.getInt();

            ByteBuffer privateKeyBuffer = ByteBuffer.allocate(privateKeyLength);
            readFully(serverChannel, privateKeyBuffer);
            privateKeyBuffer.flip();
            byte[] privateKeyBytes = new byte[privateKeyLength];
            privateKeyBuffer.get(privateKeyBytes);

            lengthBuffer.clear();
            readFully(serverChannel, lengthBuffer);
            lengthBuffer.flip();
            int certLength = lengthBuffer.getInt();

            ByteBuffer certBuffer = ByteBuffer.allocate(certLength);
            readFully(serverChannel, certBuffer);
            certBuffer.flip();
            byte[] certBytes = new byte[certLength];
            certBuffer.get(certBytes);

            serverChannel.close();

            saveKeys(privateKeyBytes, certBytes);

            System.out.println("Keys received and saved successfully!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void readFully(SocketChannel channel, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            int bytesRead = channel.read(buffer);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream");
            }
        }
    }

    private void saveKeys(byte[] privateKeyBytes, byte[] certBytes) throws Exception {
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n" +
                Base64.getEncoder().encodeToString(privateKeyBytes).replaceAll("(.{64})", "$1\n") +
                "\n-----END PRIVATE KEY-----\n";

        String keyFileName = name + ".key";
        try (FileWriter keyWriter = new FileWriter(keyFileName)) {
            keyWriter.write(privateKeyPem);
        }

        String certPem = "-----BEGIN CERTIFICATE-----\n" +
                Base64.getEncoder().encodeToString(certBytes).replaceAll("(.{64})", "$1\n") +
                "\n-----END CERTIFICATE-----\n";

        String certFileName = name + ".crt";
        try (FileWriter certWriter = new FileWriter(certFileName)) {
            certWriter.write(certPem);
        }

        System.out.println("Private key saved to: " + keyFileName);
        System.out.println("Certificate saved to: " + certFileName);
    }

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java Client.java <name> <server-address> <server-port> [delaySeconds] [exitEarly]");
            System.exit(1);
        }
        String name = args[0];
        String address = args[1];
        int port = Integer.parseInt(args[2]);
        int delay = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        boolean exitEarly = args.length > 4 && Boolean.parseBoolean(args[4]);

        try {
            new Client(name, address, port, delay, exitEarly).start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}