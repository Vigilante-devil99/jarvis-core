package com.jarvis.automation;

public interface Task {
    String getName();
    boolean requiresApproval();
    TaskResult execute() throws Exception;
}
