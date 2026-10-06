package com.jarvis.core;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class JarvisHub {
    private static final JarvisHub INSTANCE = new JarvisHub();
    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private JarvisHub() {}

    public static JarvisHub getInstance() {
        return INSTANCE;
    }

    public void registerListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void unregisterListener(Consumer<String> listener) {
        listeners.remove(listener);
    }

    public void broadcast(String message) {
        // Print cleanly to laptop terminal
        System.out.println(message);
        // Send to remote Termux client(s)
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(message);
            } catch (Exception ignored) {}
        }
    }
}
