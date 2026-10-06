package com.jarvis.automation;

import com.jarvis.core.JarvisHub;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkflowEngine {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void runWorkflowAsync(String workflowName, List<Task> tasks) {
        executor.submit(() -> {
            JarvisHub hub = JarvisHub.getInstance();
            hub.broadcast("\n[Workflow Engine] Starting: " + workflowName + " (" + tasks.size() + " tasks)");
            long start = System.currentTimeMillis();

            int step = 1;
            for (Task task : tasks) {
                hub.broadcast(String.format("--> [%d/%d] Running: %s", step, tasks.size(), task.getName()));

                if (task.requiresApproval()) {
                    boolean approved = ApprovalManager.getInstance().requestApproval(
                        "Task '" + task.getName() + "' requires confirmation."
                    );

                    if (!approved) {
                        hub.broadcast("[Workflow Engine] Aborted: action denied for " + task.getName());
                        System.out.print("\nJarvis > ");
                        return;
                    }
                }

                try {
                    TaskResult result = task.execute();
                    if (!result.isSuccess()) {
                        hub.broadcast("❌ Step Failed: " + task.getName() + " (Exit code: " + result.getExitCode() + ")");
                        hub.broadcast("Output:\n" + result.getOutput());
                        hub.broadcast("[Workflow Engine] Pipeline halted.");
                        System.out.print("\nJarvis > ");
                        return;
                    } else {
                        if (!result.getOutput().isEmpty()) {
                            hub.broadcast("  " + result.getOutput().replace("\n", "\n  "));
                        }
                    }
                } catch (Exception e) {
                    hub.broadcast("❌ Exception in " + task.getName() + ": " + e.getMessage());
                    System.out.print("\nJarvis > ");
                    return;
                }
                step++;
            }

            long elapsed = System.currentTimeMillis() - start;
            hub.broadcast("[Workflow Engine] Completed '" + workflowName + "' successfully in " + elapsed + "ms.\n");
            System.out.print("Jarvis > ");
        });
    }
}
