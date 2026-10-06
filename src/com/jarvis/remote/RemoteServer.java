package com.jarvis.remote;

import com.jarvis.core.Assistant;
import com.jarvis.core.JarvisHub;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class RemoteServer extends WebSocketServer {
    private final Assistant assistant;
    private final String expectedToken;
    private final Map<WebSocket, Boolean> authenticatedSessions = new ConcurrentHashMap<>();
    private final Map<WebSocket, Consumer<String>> clientListeners = new ConcurrentHashMap<>();

    public RemoteServer(String host, int port, Assistant assistant, String expectedToken) {
        super(new InetSocketAddress(host, port));
        this.assistant = assistant;
        this.expectedToken = expectedToken;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        authenticatedSessions.put(conn, false);

        Consumer<String> listener = msg -> {
            if (conn.isOpen() && Boolean.TRUE.equals(authenticatedSessions.get(conn))) {
                conn.send("{\"type\":\"STREAM\",\"message\":\"" + escapeJson(msg) + "\"}");
            }
        };
        clientListeners.put(conn, listener);
        JarvisHub.getInstance().registerListener(listener);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        authenticatedSessions.remove(conn);
        Consumer<String> listener = clientListeners.remove(conn);
        if (listener != null) {
            JarvisHub.getInstance().unregisterListener(listener);
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        boolean isAuthed = authenticatedSessions.getOrDefault(conn, false);

        if (!isAuthed) {
            if (message.contains("\"token\":\"" + expectedToken + "\"") ||
                message.contains("\"token\": \"" + expectedToken + "\"")) {
                authenticatedSessions.put(conn, true);
                conn.send("{\"status\":\"auth_ok\",\"message\":\"Authenticated\"}");
            } else {
                conn.send("{\"status\":\"auth_failed\",\"message\":\"Unauthorized: Invalid Token\"}");
                conn.close();
            }
            return;
        }

        // Process inbound Termux command
        String commandText = extractCommandText(message);
        if (!commandText.isEmpty()) {
            JarvisHub.getInstance().broadcast("[Remote Input] " + commandText);
            assistant.processInput(commandText);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {}

    @Override
    public void onStart() {
        System.out.println("[Remote Daemon] Listening on " + getAddress());
    }

    private String extractCommandText(String json) {
        int idx = json.indexOf("\"text\":");
        if (idx == -1) return "";
        int start = json.indexOf("\"", idx + 7) + 1;
        int end = json.indexOf("\"", start);
        return (start > 0 && end > start) ? json.substring(start, end) : "";
    }

    private String escapeJson(String raw) {
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\r", "")
                  .replace("\n", "\\n");
    }
}
