package com.jarvis.voice;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceEngine {
    private final ExecutorService speechExecutor = Executors.newSingleThreadExecutor();
    private boolean voiceEnabled;

    public VoiceEngine(boolean enabled) {
        this.voiceEnabled = enabled;
    }

    public void speak(String text) {
        if (!voiceEnabled || text == null || text.isBlank()) {
            return;
        }

        String cleaned = sanitizeForSpeech(text);
        if (cleaned.isBlank()) return;

        speechExecutor.submit(() -> {
            try {
                Process process = new ProcessBuilder(
                    "espeak-ng",
                    "-v", "en-us",
                    "-s", "165",
                    "-p", "45",
                    cleaned
                ).start();
                process.waitFor();
            } catch (Exception ignored) {
            }
        });
    }

    public void toggleVoice() {
        this.voiceEnabled = !this.voiceEnabled;
        System.out.println("\033[0;33m[Voice Output: " + (voiceEnabled ? "ENABLED" : "DISABLED") + "]\033[0m");
    }

    public boolean isVoiceEnabled() {
        return voiceEnabled;
    }

    public void shutdown() {
        speechExecutor.shutdownNow();
    }

    private String sanitizeForSpeech(String input) {
        return input
            .replaceAll("```[\\s\\S]*?```", " Code snippet. ")
            .replaceAll("`.*?`", "")
            .replaceAll("[*#_>~]", "")
            .replaceAll("https?://\\S+", "link")
            .replaceAll("/[a-zA-Z0-9_./-]+", "file path")
            .replaceAll("\\s+", " ")
            .trim();
    }
}
