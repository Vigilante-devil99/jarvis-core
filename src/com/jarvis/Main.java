package com.jarvis;

import com.jarvis.ai.LocalOllamaClient;
import com.jarvis.ai.ModelClient;
import com.jarvis.core.Assistant;
import com.jarvis.core.CommandEngine;
import com.jarvis.core.ConversationMemory;
import com.jarvis.core.PermissionManager;
import com.jarvis.remote.RemoteServer;
import com.jarvis.system.ProjectManager;
import com.jarvis.voice.VoiceEngine;
import java.util.Scanner;

public class Main {
    private static final String BIND_IP = "0.0.0.0";
    private static final int PORT = 443;
    private static final String AUTH_TOKEN = "JARVIS_DEV_SECRET_KEY";

    public static void main(String[] args) {
        System.out.println("=== Jarvis Dual-Console Online ===");

        Scanner scanner = new Scanner(System.in);
        ProjectManager projectManager = new ProjectManager();
        CommandEngine commandEngine = new CommandEngine(projectManager);
        
        // Initializing with the required arguments
        ModelClient aiClient = new LocalOllamaClient("mistral");
        PermissionManager permissions = new PermissionManager(scanner);
        ConversationMemory memory = new ConversationMemory(10);
        VoiceEngine voice = new VoiceEngine(false);

        Assistant assistant = new Assistant(
            commandEngine,
            aiClient,
            permissions,
            memory,
            voice,
            projectManager,
            scanner
        );

        // Start background WebSocket server for Termux link
        RemoteServer server = new RemoteServer(BIND_IP, PORT, assistant, AUTH_TOKEN);
        server.start();

        System.out.println("Jarvis console ready. Enter commands or connect via Termux client.");

        while (true) {
            System.out.print("Jarvis > ");
            if (!scanner.hasNextLine()) break;
            String line = scanner.nextLine().trim();
            if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) break;
            if (!line.isEmpty()) {
                assistant.processInput(line);
            }
        }

        try {
            server.stop();
        } catch (InterruptedException ignored) {}
    }
}
