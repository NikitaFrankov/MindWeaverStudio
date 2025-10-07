# MindWeaverStudio

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blueviolet.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![JetBrains Koog](https://img.shields.io/badge/Koog-JetBrains-orange.svg?style=flat)](https://github.com/JetBrains/koog)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Build Status](https://img.shields.io/github/actions/workflow/status/NikitaFrankov/MindWeaverStudio/ci.yml?label=CI)](https://github.com/NikitaFrankov/MindWeaverStudio/actions) <!-- Add if CI is set up -->

## Overview

MindWeaverStudio is an experimental multi-platform application designed as a research project in multi-agent AI systems (MAS). It aims to create an intelligent, IDE-like environment where specialized AI agents collaborate to automate software development tasks, such as code generation, reviewing, and documentation creation. The project is built using Kotlin Multiplatform for cross-platform compatibility, supporting both Android and Desktop (JVM) targets. 

At its core, it utilizes JetBrains' Koog framework for agent orchestration, integrated with local large language models (LLMs) via Ollama for privacy-preserving, offline operations. This setup enables autonomous AI-driven workflows, making it ideal for developers exploring AI-assisted coding without relying on cloud services.

Key objectives:
- Automate repetitive dev tasks through collaborative AI agents.
- Ensure data privacy with local inference and offline capabilities.
- Experiment with MAS architectures for scalable, intelligent systems.

The project is currently in active development and welcomes contributions from the community.

## Features

- **Multi-Agent Orchestration**: Implements a system of specialized agents (e.g., `CodeCreatorAgent`, `CodeReviewerAgent`, `ReleaseNotesGeneratorAgent`) using Koog to handle tasks like generating code snippets, performing code reviews, and creating release notes based on context.
- **Local LLM Support**: Integrates Ollama for running models such as Qwen2.5-VL-72B and DeepSeek offline, enabling RAG (Retrieval-Augmented Generation) for context-aware responses.
- **Offline Speech-to-Text**: Uses Vosk for voice input, allowing hands-free commands and queries within the app.
- **Cross-Platform UI**: 
  - Android: Jetpack Compose for modern, declarative interfaces.
  - Desktop: Compose Multiplatform for consistent UI across platforms.
  - Features include syntax highlighting (Kotlin, Java, JSON), a built-in code editor, tree-view navigation for files/projects, and an integrated AI chat panel for real-time assistance.
- **Integrations and Tools**:
  - GitHub API for version control and automation (e.g., pulling repos, committing changes).
  - Redis for efficient caching and state management in agent interactions.
  - RAG pipelines to augment AI outputs with retrieved data.
- **Architecture Benefits from Koog**: Reduces boilerplate, enhances agent reliability and security, accelerates iteration, and improves transparency in AI logic.
- **Additional Capabilities**: Docker support for containerized services (e.g., Ollama, Redis); Material3 theming for a polished look.

## Tech Stack

- **Language & Multiplatform**: Kotlin Multiplatform (KMP) for shared logic.
- **UI**: Jetpack Compose (Android), Compose Multiplatform (Desktop), Material3.
- **Architecture Patterns**: MVI/MVIKotlin, Clean Architecture.
- **Reactive Programming**: Coroutines, RxJava.
- **Networking & APIs**: Retrofit/Websocket (for external services), Ktor (if applicable for server-side).
- **AI Components**: JetBrains Koog (MAS framework), Ollama (local LLM inference), RAG, Vosk (STT).
- **Storage & Caching**: Redis, Docker for orchestration.
- **Build & Versioning**: Gradle (with KMP plugins), Git.
- **Testing**: Kotlin Test for units; potential GitHub Actions for CI/CD.
