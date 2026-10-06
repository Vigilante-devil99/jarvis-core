package com.jarvis.system;

public class AppLauncher {
    public static void launch(String app) {
        try {
            new ProcessBuilder(app).start();
            System.out.println("Started process: " + app);
        } catch (Exception e) {
            System.err.println("Could not launch " + app + ": " + e.getMessage());
        }
    }
}
