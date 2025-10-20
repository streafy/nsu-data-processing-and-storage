package ru.nsu.badin.javaconcurrency.taskj3;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSpider {
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final Gson gson = new Gson();
    private final ConcurrentHashMap<String, CompletableFuture<ServerResponse>> endpointFutures = new ConcurrentHashMap<>();

    public WebSpider(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<String> start() {
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Void> rootFuture = processEndpoint("", executor);
            rootFuture.join();

            return endpointFutures
                    .values()
                    .stream()
                    .map(future -> {
                        try {
                            ServerResponse response = future.getNow(null);
                            return response != null ? response.message() : null;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
        }
    }

    private CompletableFuture<Void> processEndpoint(String endpoint, ExecutorService executor) {
        CompletableFuture<ServerResponse> responseFuture = endpointFutures.computeIfAbsent(
                endpoint,
                key -> CompletableFuture.supplyAsync(() -> fetchEndpointData(key), executor)
        );

        return responseFuture
                .thenComposeAsync(response -> {
                    if (response == null || response.successors().isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }

                    var childFutures = response
                            .successors()
                            .stream()
                            .map(successor -> processEndpoint(successor, executor))
                            .toArray(CompletableFuture[]::new);

                    return CompletableFuture.allOf(childFutures);
                }, executor)
                .exceptionally(throwable -> {
                    System.err.println("/" + endpoint + ": Ошибка обработки: " + throwable.getMessage());
                    return null;
                });
    }

    private ServerResponse fetchEndpointData(String endpoint) {
        String url = baseUrl + "/" + endpoint;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                JsonObject jsonObject = gson.fromJson(response.body(), JsonObject.class);

                String message = jsonObject.has("message") ?
                        jsonObject.get("message").getAsString() : "";

                List<String> successors = new ArrayList<>();
                if (jsonObject.has("successors") && jsonObject.get("successors").isJsonArray()) {
                    jsonObject.getAsJsonArray("successors").forEach(
                            element -> successors.add(element.getAsString())
                    );
                }

                System.out.println("/" + endpoint + ": " + message + " " + successors);

                return new ServerResponse(endpoint, message, successors);
            } else {
                System.err.println("/" + endpoint + ": " + response.statusCode());
            }

        } catch (Exception e) {
            System.err.println("/" + endpoint + ": " + e.getMessage());
        }

        return null;
    }
}
