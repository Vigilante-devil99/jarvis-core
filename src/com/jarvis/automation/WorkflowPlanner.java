package com.jarvis.automation;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkflowPlanner {
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME = "mistral"; // Or whichever local model you have pulled

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String SYSTEM_PROMPT = 
        "You are an autonomous Linux Mint automation planner for an assistant named Jarvis.\\n" +
        "Convert the user's plain English automation request into a sequential multi-step bash execution plan.\\n" +
        "You MUST respond ONLY with a raw JSON object following this EXACT schema, with no preamble, markdown fences, or explanations:\\n" +
        "{\\n" +
        "  \\\"plan_name\\\": \\\"Short title\\\",\\n" +
        "  \\\"steps\\\": [\\n" +
        "    {\\n" +
        "      \\\"name\\\": \\\"Short description of step\\\",\\n" +
        "      \\\"command\\\": \\\"Exact bash command to execute\\\",\\n" +
        "      \\\"dangerous\\\": true/false\\n" +
        "    }\\n" +
        "  ]\\n" +
        "}\\n" +
        "Rules:\\n" +
        "1. Set 'dangerous' to true if the command deletes files (rm, shred), kills processes (kill, pkill), modifies root/system state (sudo, apt, systemctl), or alters git history.\\n" +
        "2. Keep bash commands safe, non-interactive (e.g., use -y for apt if needed), and strictly relevant to Linux Mint.";

    public List<Task> planWorkflow(String userPrompt) throws Exception {
        // Construct JSON payload for Ollama with format: "json"
        String escapedUserPrompt = userPrompt.replace("\"", "\\\"").replace("\n", " ");
        String requestJson = String.format(
            "{\"model\": \"%s\", \"system\": \"%s\", \"prompt\": \"%s\", \"format\": \"json\", \"stream\": false}",
            MODEL_NAME, SYSTEM_PROMPT, escapedUserPrompt
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(45))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP error: " + response.statusCode());
        }

        // Extract the "response" field from Ollama's API output
        String rawBody = response.body();
        String jsonPayload = extractResponseContent(rawBody);

        return parseTasksFromJson(jsonPayload);
    }

    private String extractResponseContent(String ollamaOutput) {
        Pattern pattern = Pattern.compile("\"response\"\\s*:\\s*\"(.*?)(?<!\\\\)\"", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(ollamaOutput);
        if (matcher.find()) {
            return matcher.group(1)
                    .replace("\\\"", "\"")
                    .replace("\\n", "\n")
                    .replace("\\\\", "\\");
        }
        return ollamaOutput;
    }

    private List<Task> parseTasksFromJson(String planJson) {
        List<Task> tasks = new ArrayList<>();

        // Regex parsing of each task item in the "steps" array
        Pattern stepPattern = Pattern.compile(
            "\\{\\s*\"name\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"command\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"dangerous\"\\s*:\\s*(true|false)\\s*\\}",
            Pattern.DOTALL
        );

        Matcher matcher = stepPattern.matcher(planJson);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String command = matcher.group(2).trim();
            boolean dangerous = Boolean.parseBoolean(matcher.group(3).trim());

            // Secondary hard safety check: enforce approval if destructive patterns are found
            if (command.contains("rm ") || command.contains("sudo") || command.contains("kill")) {
                dangerous = true;
            }

            tasks.add(new ShellTask(name, command, dangerous));
        }

        if (tasks.isEmpty()) {
            throw new IllegalStateException("Planner failed to parse valid steps. Raw output was:\n" + planJson);
        }

        return tasks;
    }
}
