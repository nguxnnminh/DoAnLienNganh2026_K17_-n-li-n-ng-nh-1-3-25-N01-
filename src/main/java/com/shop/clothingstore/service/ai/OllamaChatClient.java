package com.shop.clothingstore.service.ai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shop.clothingstore.config.ChatbotAiProperties;

@Component
public class OllamaChatClient {

    private final ChatbotAiProperties props;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OllamaChatClient(ChatbotAiProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(1_000, props.getTimeoutMs())))
                .build();
    }

    public boolean isConfigured() {
        return props.isEnabled()
                && props.getOllamaBaseUrl() != null
                && !props.getOllamaBaseUrl().isBlank()
                && props.getOllamaModel() != null
                && !props.getOllamaModel().isBlank();
    }

    public String chat(List<Map<String, Object>> messages) throws IOException, InterruptedException {
        String baseUrl = props.getOllamaBaseUrl().replaceAll("/+$", "");
        URI uri = URI.create(baseUrl + "/api/chat");

        Map<String, Object> payload = Map.of(
                "model", props.getOllamaModel(),
                "stream", false,
                "messages", messages
        );

        String json = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(Math.max(2_000, props.getTimeoutMs())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            int status = response.statusCode();
            String body = truncate(response.body(), 1000);
            throw new AiRequestException(
                    status,
                    "Ollama request failed: HTTP " + status + " - " + body,
                    body
            );
        }

        Map<String, Object> decoded = objectMapper.readValue(response.body(), new TypeReference<>() {});
        Object messageObj = decoded.get("message");
        if (!(messageObj instanceof Map<?, ?> msgMap)) {
            throw new IOException("Ollama response missing message object");
        }
        Object contentObj = msgMap.get("content");
        String content = contentObj == null ? "" : String.valueOf(contentObj);
        return content == null ? "" : content.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max) + "...";
    }
}

