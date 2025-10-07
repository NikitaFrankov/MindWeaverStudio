MindWeaverStudio



Overview
MindWeaverStudio is a multi-platform research project aimed at building an intelligent development environment powered by multi-agent AI systems. It leverages specialized AI agents to automate software development tasks, such as code generation, review, and documentation. Built with Kotlin Multiplatform, the project supports Android and Desktop platforms, ensuring cross-platform compatibility while maintaining a native feel through Jetpack Compose and Compose Multiplatform.
The core architecture revolves around orchestrating AI agents using JetBrains' Koog framework, integrated with local LLMs for privacy-focused, autonomous operations. This tool is designed for developers interested in AI-driven workflows, offering an IDE-like interface with AI-assisted coding features.
Key goals:

Automate routine development tasks via AI agents.
Provide a privacy-centric environment with offline LLM inference.
Explore multi-agent systems (MAS) for collaborative AI behaviors.

This is an ongoing research project, open for contributions and experimentation.
Features

Multi-Agent System Architecture: Orchestrates specialized AI agents (e.g., CodeCreator, CodeReviewer, ReleaseNotesGenerator) to handle tasks like code generation, peer review, and release note creation.
Local LLM Integration: Supports offline inference with models like Qwen2.5-VL-72B and DeepSeek via Ollama, ensuring data privacy and autonomy.
Offline Speech-to-Text: Integrated with Vosk for voice-based inputs, enabling hands-free interaction.
IDE-Like Interface: Built with Jetpack Compose (Android) and Compose Multiplatform (Desktop), featuring:

Syntax highlighting for Kotlin, Java, and JSON.
Code editor with real-time AI suggestions.
Tree-based navigation for project structure.
Embedded AI chat for contextual queries and assistance.


Retrieval-Augmented Generation (RAG): Enhances AI responses with context from local data sources.
External Integrations: GitHub API for version control and automation; Redis for caching and state management.
Koog Framework Utilization: Leverages JetBrains' Koog for efficient agent design, reducing boilerplate code, improving reliability, and ensuring secure inter-agent communication.
Cross-Platform Support: Shared logic via Kotlin Multiplatform, with platform-specific UI adaptations.

Tech Stack

Core Language: Kotlin (Multiplatform)
UI Framework: Jetpack Compose (Android), Compose Multiplatform (Desktop)
Architecture: MVI / MVIKotlin, Clean Architecture
AI & Agents: JetBrains Koog, Ollama (Local LLM), RAG, LLM Orchestration
Other Libraries:

Coroutines (for reactive programming)
Ktor (API calls)
Redis (caching)
Vosk (speech-to-text)
Docker (for containerization)
Material3 (UI theming)


Build Tools: Gradle, Git
Testing & Tools: Unit tests with Kotlin Test, integration with GitHub Actions for CI/CD
