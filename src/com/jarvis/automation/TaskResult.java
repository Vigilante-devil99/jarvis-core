package com.jarvis.automation;

public class TaskResult {
    private final boolean success;
    private final int exitCode;
    private final String output;

    public TaskResult(boolean success, int exitCode, String output) {
        this.success = success;
        this.exitCode = exitCode;
        this.output = output;
    }

    public boolean isSuccess() { return success; }
    public int getExitCode() { return exitCode; }
    public String getOutput() { return output; }
}
