# MindWeaverStudio

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-blueviolet.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![JetBrains Koog](https://img.shields.io/badge/Koog-JetBrains-orange.svg?style=flat)](https://github.com/JetBrains/koog)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

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
  - Desktop: Compose Multiplatform for consistent UI across platforms.
  - Features include syntax highlighting (Kotlin, Java, JSON), a built-in code editor, tree-view navigation for files/projects, and an integrated AI chat panel for real-time assistance.

## Tech Stack

- **Language & Multiplatform**: Kotlin Multiplatform (KMP) for shared logic.
- **UI**: Compose Multiplatform (Desktop)
- **Architecture Patterns**: MVI/MVIKotlin, Decompose.
- **Reactive Programming**: Coroutines.
- **Networking & APIs**: Ktor.
- **AI Components**: JetBrains Koog (MAS framework), Ollama (local LLM inference), RAG, Vosk (STT).
- **Storage & Caching**: Redis, Docker.
- **Build & Versioning**: Gradle, Git.
