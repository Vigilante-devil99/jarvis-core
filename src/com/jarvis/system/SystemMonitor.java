package com.jarvis.system;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemMonitor {

    public record ExecutionResult(int exitCode, String stdout, String stderr) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }

    public static void showStatus() {
        System.out.println("\n--- System Status ---");
        execute("echo \"OS: $(uname -srm)\" && echo \"Uptime: $(uptime -p)\" && free -h | awk '/^Mem:/ {print \"RAM Usage: \" $3 \" / \" $2}'", true);
        System.out.println("---------------------\n");
    }

    public static void showGpu() {
        execute("nvidia-smi --query-gpu=name,temperature.gpu,utilization.gpu,memory.used,memory.total --format=csv,noheader || echo 'NVIDIA GPU not detected or nvidia-smi missing.'", true);
    }

    public static ExecutionResult execute(String bashCommand, boolean printLive) {
        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();
        int exitCode = -1;

        try {
            Process process = new ProcessBuilder("bash", "-c", bashCommand).start();

            try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {

                String line;
                while ((line = outReader.readLine()) != null) {
                    stdout.append(line).append("\n");
                    if (printLive) System.out.println(line);
                }
                while ((line = errReader.readLine()) != null) {
                    stderr.append(line).append("\n");
                    if (printLive) System.err.println(line);
                }
            }

            exitCode = process.waitFor();
        } catch (Exception e) {
            stderr.append("Execution exception: ").append(e.getMessage()).append("\n");
        }

        return new ExecutionResult(exitCode, stdout.toString().trim(), stderr.toString().trim());
    }
}
