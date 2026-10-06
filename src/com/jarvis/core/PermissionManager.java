package com.jarvis.core;

import java.util.Scanner;

public class PermissionManager {
    private final Scanner scanner;

    public PermissionManager(Scanner scanner) {
        this.scanner = scanner;
    }

    public boolean confirmExecution(String bashCommand) {
        System.out.println("\033[1;33m[Permission Required]\033[0m Command: " + bashCommand);
        System.out.print("\033[1;35mAuthorize execution? [Y/n]: \033[0m");

        if (!scanner.hasNextLine()) return false;
        String input = scanner.nextLine().trim();

        return input.isEmpty() || input.equalsIgnoreCase("y") || input.equalsIgnoreCase("yes");
    }

    public static String cleanCommand(String cmd) {
        return cmd.replace("\\u003e", ">")
                  .replace("\\u003c", "<")
                  .replace("\\u0026", "&")
                  .replace("u003e", ">")
                  .replace("u003c", "<")
                  .replace("u0026", "&")
                  .replace("&amp;", "&");
    }
}
