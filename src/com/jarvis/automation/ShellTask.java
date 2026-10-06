package com.jarvis.automation;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShellTask implements Task {
    private final String name;
    private final String command;
    private final boolean dangerous;

    public ShellTask(String name, String command, boolean dangerous) {
        this.name = name;
        this.command = command;
        this.dangerous = dangerous;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean requiresApproval() {
        return dangerous;
    }

    @Override
    public TaskResult execute() throws Exception {
        ProcessBuilder builder = new ProcessBuilder("bash", "-c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        return new TaskResult(exitCode == 0, exitCode, sb.toString().trim());
    }
}
