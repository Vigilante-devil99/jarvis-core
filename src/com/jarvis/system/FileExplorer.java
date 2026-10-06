package com.jarvis.system;

public class FileExplorer {
    public static void findFiles(String query) {
        String cmd = "find ~ -maxdepth 3 -iname '*" + query + "*' 2>/dev/null | head -n 10";
        SystemMonitor.execute(cmd, true);
    }
}
