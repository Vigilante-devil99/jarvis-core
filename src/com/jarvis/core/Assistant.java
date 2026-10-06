package com.jarvis.core;

import com.jarvis.ai.ModelClient;
import com.jarvis.core.ConversationMemory.Message;
import com.jarvis.system.ProjectManager;
import com.jarvis.voice.VoiceEngine;
import java.util.List;
import java.util.Scanner;

public class Assistant {
    private final CommandEngine commandEngine;
    private final ModelClient aiClient;
    private final PermissionManager permissions;
    private final ConversationMemory memory;
    private final VoiceEngine voice;
    private final ProjectManager projectManager;
    private final Scanner scanner;

    public Assistant(CommandEngine commandEngine, ModelClient aiClient,
                     PermissionManager permissions, ConversationMemory memory,
                     VoiceEngine voice, ProjectManager projectManager, Scanner scanner) {
        this.commandEngine = commandEngine;
        this.aiClient = aiClient;
        this.permissions = permissions;
        this.memory = memory;
        this.voice = voice;
        this.projectManager = projectManager;
        this.scanner = scanner;
    }

    public void processInput(String input) {
        if (input == null) return;
        String cleanInput = input.trim();
        if (cleanInput.isEmpty()) return;

        // 1. Execute built-in / native commands (start, stop, sync, sys, projects, etc.)
        if (commandEngine.executeNative(cleanInput)) {
            return;
        }

        // 2. Direct natural language project launch fallback
        String lower = cleanInput.toLowerCase();
        if (lower.startsWith("start ")) {
            String target = cleanInput.substring(6).trim();
            if (projectManager.getAllProjects().containsKey(target)) {
                projectManager.launchProject(target);
                return;
            }
        }

        // 3. Fall back to AI Model Client
        try {
            List<Message> history = (memory != null) ? memory.getHistory() : null;
            if (memory != null) {
                memory.addUserMessage(cleanInput);
            }

            String response = aiClient.generate(cleanInput, history);
            System.out.println(response);

            if (memory != null) {
                memory.addAssistantMessage(response);
            }
            if (voice != null) {
                voice.speak(response);
            }
        } catch (Exception e) {
            System.out.println("[AI Error] " + e.getMessage());
        }
    }
}
