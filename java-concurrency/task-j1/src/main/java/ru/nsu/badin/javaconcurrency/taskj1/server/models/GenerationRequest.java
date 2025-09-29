package ru.nsu.badin.javaconcurrency.taskj1.server.models;

import java.util.concurrent.CompletableFuture;

public record GenerationRequest(String clientName, CompletableFuture<KeysWithCertificate> future) {}
