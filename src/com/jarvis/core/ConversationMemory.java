package com.jarvis.core;

import java.util.ArrayList;
import java.util.List;

public class ConversationMemory {

    public record Message(String role, String content) {}

    private final List<Message> history = new ArrayList<>();
    private final int maxTurns;

    public ConversationMemory(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public void addUserMessage(String text) {
        addMessage("user", text);
    }

    public void addAssistantMessage(String text) {
        addMessage("assistant", text);
    }

    private synchronized void addMessage(String role, String text) {
        history.add(new Message(role, text));
        // Keep a sliding window (maxTurns * 2 messages for user + assistant pairs)
        while (history.size() > maxTurns * 2) {
            history.remove(0);
        }
    }

    public synchronized List<Message> getHistory() {
        return new ArrayList<>(history);
    }

    public synchronized void clear() {
        history.clear();
    }
}
