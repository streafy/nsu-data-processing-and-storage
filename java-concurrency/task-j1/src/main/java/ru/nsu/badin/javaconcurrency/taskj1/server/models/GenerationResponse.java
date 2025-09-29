package ru.nsu.badin.javaconcurrency.taskj1.server.models;

import java.nio.channels.SocketChannel;

public record GenerationResponse(SocketChannel channel, KeysWithCertificate keysWithCertificate) { }
