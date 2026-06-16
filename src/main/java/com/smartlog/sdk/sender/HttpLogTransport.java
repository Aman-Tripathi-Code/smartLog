package com.smartlog.sdk.sender;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class HttpLogTransport implements LogTransport {

    private final HttpClient httpClient;
    private final URI endpoint;

    public HttpLogTransport(String endpoint) {
        this(URI.create(endpoint), HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
    }

    HttpLogTransport(URI endpoint, HttpClient httpClient) {
        this.endpoint = endpoint;
        this.httpClient = httpClient;
    }

    @Override
    public boolean post(String jsonPayload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();
        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        return response.statusCode() >= 200 && response.statusCode() < 300;
    }
}
