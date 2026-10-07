# JARVIS Core

A lightweight, local-first autonomous AI companion and system orchestrator built in Java. Features an ANSI terminal HUD, real-time dual-console sync (Desktop & Android Termux via WebSockets), local Ollama inference, voice feedback, and system app control.

---

## Features

- **Terminal HUD**: ANSI dashboard displaying active model, daemon port, and directives.
- **Dual-Console Sync**: WebSocket server on port `443` synchronizing laptop and mobile Termux input/output.
- **Local-First LLM**: 100% offline inference using local Ollama models (`qwen2.5-coder:3b`, `llama3.2`).
- **Dynamic Model Switching**: Swap models on the fly (`models`, `models switch <name>`) directly from console.
- **Desktop App Control**: Open, close, and monitor desktop applications (`open firefox`, `close firefox`, `apps`).
- **Voice Feedback**: Speech synthesis via `espeak-ng` for acknowledgments and spoken replies.
- **System Diagnostics**: Real-time auditing for CPU, RAM, disk space, and GPU metrics.

---

## Directory Structure

```text
jarvis/
├── src/com/jarvis/
│   ├── Main.java               # ANSI HUD & dynamic model switcher
│   ├── ai/                     # Local Ollama client & interface
│   ├── core/                   # Assistant, CommandEngine, Memory, Permissions
│   ├── system/                 # AppLauncher, SystemMonitor, ProjectManager
│   ├── remote/                 # RemoteServer (WebSocket daemon)
│   └── voice/                  # VoiceEngine (espeak-ng)
├── lib/                        # Java WebSocket libraries
└── README.md
