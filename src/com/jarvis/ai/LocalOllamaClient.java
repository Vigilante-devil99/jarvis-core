package com.jarvis.ai;

import com.jarvis.core.ConversationMemory.Message;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class LocalOllamaClient implements ModelClient {
    private String currentModel;
    private final HttpClient httpClient;

    public LocalOllamaClient(String defaultModel) {
        this.currentModel = defaultModel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void setModel(String newModel) {
        this.currentModel = newModel;
    }

    public String getCurrentModel() {
        return this.currentModel;
    }

    @Override
    public String generate(String prompt, List<Message> history) {
        try {
            StringBuilder fullPrompt = new StringBuilder();
            if (history != null) {
                for (Message msg : history) {
                    fullPrompt.append(msg.role()).append(": ").append(msg.content()).append("\n");
                }
            }
            fullPrompt.append("User: ").append(prompt).append("\nJarvis:");

            String jsonPayload = String.format(
                "{\"model\": \"%s\", \"prompt\": %s, \"stream\": false}",
                currentModel,
                escapeJson(fullPrompt.toString())
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:11434/api/generate"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return extractResponseText(response.body());
            } else {
                return "Error from Ollama [Code " + response.statusCode() + "]: " + response.body();
            }
        } catch (Exception e) {
            return "Failed to connect to local Ollama daemon: " + e.getMessage();
        }
    }

    private String escapeJson(String text) {
        return "\"" + text.replace("\\", "\\\\")
                          .replace("\"", "\\\"")
                          .replace("\n", "\\n")
                          .replace("\r", "\\r")
                          .replace("\t", "\\t") + "\"";
    }

    private String extractResponseText(String json) {
        int idx = json.indexOf("\"response\":\"");
        if (idx == -1) return json;
        int start = idx + 12;
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                if (c == 'n') sb.append("\n");
                else if (c == 't') sb.append("\t");
                else sb.append(c);
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
