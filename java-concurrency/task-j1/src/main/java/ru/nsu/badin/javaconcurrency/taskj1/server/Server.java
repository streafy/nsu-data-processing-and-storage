package ru.nsu.badin.javaconcurrency.taskj1.server;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.GenerationRequest;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.GenerationResponse;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.KeysWithCertificate;

import java.io.IOException;
import java.security.*;
import java.util.concurrent.*;

public class Server {

    private final int port;
    private final int threadCount;
    private final String issuerName;
    private final PrivateKey signingKey;

    private static final int DEFAULT_THREAD_COUNT = 8;
    private final ExecutorService keyGenerationPool;

    private final ConcurrentHashMap<String, KeysWithCertificate> generationResultStorage = new ConcurrentHashMap<>();

    private final BlockingQueue<GenerationRequest> generationQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<GenerationResponse> responseQueue = new LinkedBlockingQueue<>();

    public Server(int port, int threadCount, String issuerName, PrivateKey signingKey) {
        this.port = port;
        this.threadCount = threadCount;
        this.issuerName = issuerName;
        this.signingKey = signingKey;

        keyGenerationPool = Executors.newFixedThreadPool(threadCount);
    }

    public Server(int port, String issuerName, PrivateKey signingKey) {
        this(port, DEFAULT_THREAD_COUNT, issuerName, signingKey);
    }

    public void start() throws IOException {
        System.out.println("Server started");
        Security.addProvider(new BouncyCastleProvider());

        new ClientCommunication(port, generationQueue, responseQueue, generationResultStorage).start();

        for (int i = 0; i < threadCount; i++) {
            keyGenerationPool.submit(new KeysGeneration(generationResultStorage, generationQueue, issuerName, signingKey));
        }
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.out.println("Usage: java Server.java <port> <generation-threads-count> <issuer-name> <signing-key-file>");
            System.exit(1);
        }

        int port = Integer.parseInt(args[0]);
        int threads = Integer.parseInt(args[1]);
        String issuer = args[2];
        String signingKeyFile = args[3];

        try {
            KeyPairGenerator caKeyGen = KeyPairGenerator.getInstance("RSA");
            caKeyGen.initialize(4096);
            KeyPair caKeyPair = caKeyGen.generateKeyPair();
            PrivateKey signingKey = caKeyPair.getPrivate();

            new Server(port, threads, issuer, signingKey).start();
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
