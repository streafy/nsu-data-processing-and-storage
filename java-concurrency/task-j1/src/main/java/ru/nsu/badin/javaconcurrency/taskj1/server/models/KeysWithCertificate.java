package ru.nsu.badin.javaconcurrency.taskj1.server.models;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

public record KeysWithCertificate(KeyPair keyPair, X509Certificate certificate) { }
