package com.jarvis.system;

import java.io.*;
import java.util.*;

public class ProjectManager {
    public record Project(String name, String path, String ide, String serviceHook) {}

    private static final String REGISTRY_PATH = System.getProperty("user.home") + "/.jarvis/projects.csv";
    private final Map<String, Project> projects = new HashMap<>();

    public ProjectManager() {
        loadProjects();
    }

    private void loadProjects() {
        File file = new File(REGISTRY_PATH);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.err.println("Could not create project registry: " + e.getMessage());
            }
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split(",", 4);
                    if (parts.length >= 2) {
                        String name = parts[0].trim();
                        String path = parts[1].trim();
                        String ide = parts.length > 2 && !parts[2].isBlank() ? parts[2].trim() : "vim";
                        String hook = parts.length > 3 && !parts[3].isBlank() ? parts[3].trim() : "none";
                        projects.put(name, new Project(name, path, ide, hook));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Failed reading project registry: " + e.getMessage());
        }
    }

    public void addProject(String name, String path, String ide, String serviceHook) {
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            System.out.println("\033[0;31mError:\033[0m Directory does not exist: " + path);
            return;
        }

        String editor = (ide == null || ide.isBlank()) ? "vim" : ide;
        String hook = (serviceHook == null || serviceHook.isBlank()) ? "none" : serviceHook;
        
        projects.put(name, new Project(name, path, editor, hook));
        saveProjects();
        System.out.println("\033[0;32m[Registered]\033[0m Project '" + name + "' mapped to " + path + " [Editor: " + editor + "]");
    }

    public void addProject(String name, String path) {
        addProject(name, path, "vim", "none");
    }

    private void saveProjects() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(REGISTRY_PATH))) {
            for (Project p : projects.values()) {
                writer.println(String.join(",", p.name(), p.path(), p.ide(), p.serviceHook()));
            }
        } catch (IOException e) {
            System.err.println("Failed to save registry: " + e.getMessage());
        }
    }

    public void listProjectsWithGitStatus() {
        if (projects.isEmpty()) {
            System.out.println("No registered projects found in " + REGISTRY_PATH);
            System.out.println("Add one with: project add <name> <absolute-path>");
            return;
        }

        System.out.println("\n\033[1;34m=== Registered Projects ===\033[0m");
        for (Project p : projects.values()) {
            String gitBranch = getGitBranch(p.path());
            System.out.printf(" • \033[1m%-12s\033[0m %-30s [Editor: %-5s] [Git: %s]\n", 
                p.name(), p.path(), p.ide(), gitBranch);
        }
        System.out.println();
    }

    public boolean launchProject(String name) {
        if (!projects.containsKey(name)) {
            System.out.println("\033[0;31mUnknown project:\033[0m " + name);
            return false;
        }

        Project p = projects.get(name);

        // 1. Docker Compose hook (auto-start containers if compose file exists)
        File composeFile = new File(p.path(), "docker-compose.yml");
        if (composeFile.exists()) {
            System.out.println("\033[0;34m[Docker]\033[0m Starting containers for " + name + "...");
            SystemMonitor.execute("cd \"" + p.path() + "\" && docker compose up -d", false);
        }

        // 2. Launch Editor (Vim in terminal)
        System.out.println("\033[0;32m[Workspace]\033[0m Launching Vim in " + p.path());

        String termCommand;
        if ("vim".equalsIgnoreCase(p.ide()) || "vi".equalsIgnoreCase(p.ide())) {
            termCommand = String.format("gnome-terminal --working-directory=\"%s\" --title=\"Vim - %s\" -- vim .", p.path(), name);
        } else {
            termCommand = String.format("%s \"%s\"", p.ide(), p.path());
        }

        SystemMonitor.execute(termCommand, false);
        return true;
    }

    public boolean stopProject(String name) {
        if (!projects.containsKey(name)) {
            System.out.println("\033[0;31mUnknown project:\033[0m " + name);
            return false;
        }

        Project p = projects.get(name);
        File composeFile = new File(p.path(), "docker-compose.yml");
        if (composeFile.exists()) {
            System.out.println("\033[0;33m[Docker]\033[0m Stopping containers for " + name + "...");
            SystemMonitor.execute("cd \"" + p.path() + "\" && docker compose down", false);
        }
        System.out.println("\033[0;32m[Closed]\033[0m Environment teardown complete for " + name);
        return true;
    }

    public Map<String, Project> getAllProjects() {
        return Collections.unmodifiableMap(projects);
    }

    private String getGitBranch(String dirPath) {
        try {
            Process process = new ProcessBuilder("git", "-C", dirPath, "rev-parse", "--abbrev-ref", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String branch = reader.readLine();
                return (branch != null && !branch.contains("fatal")) ? "\033[0;32m" + branch.trim() + "\033[0m" : "\033[0;90mno git\033[0m";
            }
        } catch (Exception e) {
            return "\033[0;90munknown\033[0m";
        }
    }
}
