package com.jarvis.ai;

import com.jarvis.core.ConversationMemory.Message;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class GeminiClient implements ModelClient {
    private final String apiKey;
    private final HttpClient client;
    private static final String MODEL = "gemini-1.5-flash";

    private static final String SYSTEM_INSTRUCTION = 
        "You are JARVIS, an autonomous Linux Mint terminal assistant. " +
        "When the user requests terminal actions, disk/RAM inspections, or system tasks, " +
        "respond ONLY with: EXECUTE: <bash command>. Otherwise, answer concisely.";

    public GeminiClient() {
        this.apiKey = System.getenv("GEMINI_API_KEY");
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String generate(String prompt, List<Message> history) {
        if (apiKey == null || apiKey.isBlank()) {
            return "[Error]: GEMINI_API_KEY is not set. Run: export GEMINI_API_KEY=\"...\"";
        }

        try {
            String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" 
                            + MODEL + ":generateContent?key=" + apiKey;

            StringBuilder contentsJson = new StringBuilder();

            // Append history
            for (Message msg : history) {
                String role = msg.role().equals("assistant") ? "model" : "user";
                String safe = msg.content().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
                contentsJson.append(String.format("{\"role\": \"%s\", \"parts\": [{\"text\": \"%s\"}]},", role, safe));
            }

            // Append current prompt
            String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
            contentsJson.append(String.format("{\"role\": \"user\", \"parts\": [{\"text\": \"%s\"}]}", safePrompt));

            String sanitizedSystem = SYSTEM_INSTRUCTION.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");

            String payload = String.format("""
                {
                  "system_instruction": {
                    "parts": [{ "text": "%s" }]
                  },
                  "contents": [%s]
                }
                """, sanitizedSystem, contentsJson);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(60))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                return "[Gemini API Error (" + response.statusCode() + ")]: " + response.body();
            }

            return extractGeminiText(response.body());
        } catch (Exception e) {
            return "[Gemini Request Failed]: " + e.getMessage();
        }
    }

    private static String extractGeminiText(String json) {
        String key = "\"text\": \"";
        int start = json.indexOf(key);
        if (start == -1) {
            key = "\"text\":\"";
            start = json.indexOf(key);
            if (start == -1) return "Could not parse response from Gemini.";
        }

        start += key.length();
        StringBuilder sb = new StringBuilder();
        boolean escaped = false;

        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
