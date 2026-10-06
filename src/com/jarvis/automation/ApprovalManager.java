package com.jarvis.automation;

import com.jarvis.core.JarvisHub;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ApprovalManager {
    private static final ApprovalManager INSTANCE = new ApprovalManager();
    private volatile CompletableFuture<Boolean> currentRequest = null;

    private ApprovalManager() {}

    public static ApprovalManager getInstance() {
        return INSTANCE;
    }

    public synchronized boolean requestApproval(String prompt) {
        this.currentRequest = new CompletableFuture<>();

        String message = "\n[SAFETY CHECK] " + prompt + "\nAuthorize execution? (y/n): ";
        JarvisHub.getInstance().broadcast(message);

        try {
            // Wait up to 60s for either terminal or Termux
            Boolean result = currentRequest.get(60, TimeUnit.SECONDS);
            return result != null && result;
        } catch (Exception e) {
            JarvisHub.getInstance().broadcast("[ApprovalManager] Authorization timed out or rejected.");
            return false;
        } finally {
            this.currentRequest = null;
        }
    }

    public synchronized boolean hasPendingApproval() {
        return currentRequest != null && !currentRequest.isDone();
    }

    public synchronized boolean handleInput(String input) {
        if (!hasPendingApproval()) return false;

        String sanitized = input.trim().toLowerCase();
        if (sanitized.equals("y") || sanitized.equals("yes")) {
            currentRequest.complete(true);
            return true;
        } else if (sanitized.equals("n") || sanitized.equals("no")) {
            currentRequest.complete(false);
            return true;
        }
        return false;
    }
}
