package ru.nsu.badin.javaconcurrency.taskj1.server;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.GenerationRequest;
import ru.nsu.badin.javaconcurrency.taskj1.server.models.KeysWithCertificate;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

public class KeysGeneration implements Runnable {
    private final ConcurrentHashMap<String, KeysWithCertificate> generationResultStorage;
    private final BlockingQueue<GenerationRequest> generationQueue;
    private final String issuerName;
    private final PrivateKey signingKey;

    public KeysGeneration(ConcurrentHashMap<String, KeysWithCertificate> generationResultStorage, BlockingQueue<GenerationRequest> generationQueue, String issuerName, PrivateKey signingKey) {
        this.generationResultStorage = generationResultStorage;
        this.generationQueue = generationQueue;
        this.issuerName = issuerName;
        this.signingKey = signingKey;
    }

    @Override
    public void run() {
        while (true) {
            try {
                GenerationRequest request = generationQueue.take();
                System.out.println("Generating keys for: " + request.clientName());

                long startTime = System.currentTimeMillis();

                KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
                keyPairGenerator.initialize(8192);
                KeyPair keyPair = keyPairGenerator.generateKeyPair();

                X509Certificate certificate = generateCertificate(request.clientName(), keyPair);

                KeysWithCertificate keysWithCertificate = new KeysWithCertificate(keyPair, certificate);
                generationResultStorage.put(request.clientName(), keysWithCertificate);

                long endTime = System.currentTimeMillis();

                System.out.println("Generated keys for " + request.clientName() + " in " + (endTime - startTime) + "ms");

                request.future().complete(keysWithCertificate);
            } catch (NoSuchAlgorithmException | CertificateException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private X509Certificate generateCertificate(String clientName, KeyPair keyPair) throws CertificateException {
        Date notBefore = new Date();
        Date notAfter = new Date(System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000);
        BigInteger serial = new BigInteger(64, new SecureRandom());

        X500Name issuer = new X500Name("CN=" + issuerName);
        X500Name subjectName = new X500Name("CN=" + clientName);
        X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subjectName,
                keyPair.getPublic()
        );

        ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                    .setProvider("BC")
                    .build(signingKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }

        X509CertificateHolder holder = certificateBuilder.build(signer);

        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(holder);
    }
}
