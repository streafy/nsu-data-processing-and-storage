package ru.nsu.badin.javaconcurrency.taskj3;

import java.util.List;

public record ServerResponse(
   String endpoint,
   String message,
   List<String> successors
) {}
