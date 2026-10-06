package com.jarvis.core;

import com.jarvis.automation.ShellTask;
import com.jarvis.automation.Task;
import com.jarvis.automation.WorkflowEngine;
import com.jarvis.automation.WorkflowPlanner;
import com.jarvis.system.FileExplorer;
import com.jarvis.system.ProjectManager;
import com.jarvis.system.SystemMonitor;
import java.util.List;

public class CommandEngine {
    private final ProjectManager projectManager;
    private final WorkflowEngine workflowEngine = new WorkflowEngine();
    private final WorkflowPlanner workflowPlanner = new WorkflowPlanner();

    public CommandEngine(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    public boolean executeNative(String input) {
        String trimmed = input.trim();
        String lower = trimmed.toLowerCase();

        if (lower.equals("status") || lower.equals("sysinfo")) {
            SystemMonitor.showStatus();
            return true;
        }

        if (lower.equals("gpu") || lower.equals("nvidia")) {
            SystemMonitor.showGpu();
            return true;
        }

        if (lower.startsWith("find ")) {
            String query = trimmed.substring(5).trim();
            FileExplorer.findFiles(query);
            return true;
        }

        if (lower.equals("projects") || lower.equals("project list")) {
            projectManager.listProjectsWithGitStatus();
            return true;
        }

        // Static Workflows
        if (lower.equals("sys-audit") || lower.equals("audit")) {
            List<Task> auditPipeline = List.of(
                new ShellTask("Check Disk Space", "df -h / | awk 'NR==2 {print $5 \" used (\" $4 \" free)\"}'", false),
                new ShellTask("Check Memory", "free -h | awk '/Mem:/ {print $3 \" used / \" $2 \" total\"}'", false),
                new ShellTask("Check Failed Services", "systemctl --failed --quiet", false),
                new ShellTask("Check Top CPU Consumer", "ps -eo %cpu,cmd --sort=-%cpu | head -n 3", false)
            );
            workflowEngine.runWorkflowAsync("System Health Audit", auditPipeline);
            return true;
        }

        if (lower.equals("clean-cache") || lower.equals("cleanup")) {
            List<Task> cleanPipeline = List.of(
                new ShellTask("Check Trash Size", "du -sh ~/.local/share/Trash 2>/dev/null || echo '0B'", false),
                new ShellTask("Empty System Trash", "rm -rf ~/.local/share/Trash/*", true),
                new ShellTask("Clean Apt Cache", "sudo apt-get clean", true)
            );
            workflowEngine.runWorkflowAsync("System Cleanup", cleanPipeline);
            return true;
        }

        // Dynamic Natural-Language Autonomous Planning
        if (lower.startsWith("auto ") || lower.startsWith("plan ") || lower.startsWith("task ")) {
            String goal = trimmed.replaceFirst("(?i)^(auto|plan|task)\\s+", "").trim();
            System.out.println("[Jarvis Planner] Synthesizing execution graph for: \"" + goal + "\" via Ollama...");
            
            new Thread(() -> {
                try {
                    List<Task> dynamicTasks = workflowPlanner.planWorkflow(goal);
                    workflowEngine.runWorkflowAsync("Autonomous: " + goal, dynamicTasks);
                } catch (Exception e) {
                    System.out.println("❌ Planning failed: " + e.getMessage());
                    System.out.print("Jarvis > ");
                }
            }).start();

            return true;
        }

        // Project management handlers
        if (lower.startsWith("project add ")) {
            String[] parts = trimmed.substring("project add ".length()).trim().split("\\s+");
            if (parts.length >= 2) {
                String name = parts[0];
                String path = parts[1];
                String ide = parts.length >= 3 ? parts[2] : "code";
                String serviceHook = parts.length >= 4 ? trimmed.substring(trimmed.indexOf(ide) + ide.length()).trim() : "none";

                projectManager.addProject(name, path, ide, serviceHook);
                System.out.println("\033[1;32m[Registered]\033[0m Project '" + name + "' bound to " + path + " with IDE " + ide + " (Service: " + serviceHook + ")");
            } else {
                System.out.println("Usage: project add <name> <path> [ide] [custom_service_cmd]");
            }
            return true;
        }

        if (lower.startsWith("open project ") || lower.startsWith("launch project ") || lower.startsWith("open ")) {
            String projName = lower.replace("open project ", "")
                                   .replace("launch project ", "")
                                   .replace("open ", "")
                                   .trim();
            if (projectManager.launchProject(projName)) {
                return true;
            }
        }

        if (lower.startsWith("stop project ") || lower.startsWith("close project ") || lower.startsWith("stop ")) {
            String projName = lower.replace("stop project ", "")
                                   .replace("close project ", "")
                                   .replace("stop ", "")
                                   .trim();
            if (projectManager.stopProject(projName)) {
                return true;
            }
        }

        return false;
    }
}
