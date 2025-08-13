# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MindWeaver Studio is a Kotlin Multiplatform Compose desktop application targeting JVM that provides a neural network chat interface. The application integrates with external AI APIs (specifically DeepSeek/OpenRouter) to enable conversational AI interactions through a modern Material3 UI.

## Development Commands

### Building and Running
- `./gradlew run` - Run the application in development mode
- `./gradlew hotRunJvm` - Run with hot reload enabled (preferred for development)
- `./gradlew build` - Build the project
- `./gradlew assemble` - Assemble the outputs without running tests

### Testing
- `./gradlew test` or `./gradlew jvmTest` - Run JVM tests
- `./gradlew allTests` - Run tests for all targets with aggregated report
- `./gradlew check` - Run all checks including tests

### Hot Reload Development
- `./gradlew hotRunJvm` - Start application with hot reload
- `./gradlew reload` - Trigger hot reload for all running applications
- `./gradlew hotReloadJvmMain` - Hot reload main compilation

### Packaging
- `./gradlew createDistributable` - Create distributable package
- `./gradlew packageDistributionForCurrentOS` - Package for current OS
- `./gradlew packageDmg` - Create DMG package (macOS)
- `./gradlew packageMsi` - Create MSI package (Windows)
- `./gradlew packageDeb` - Create DEB package (Linux)

### Cleaning
- `./gradlew clean` - Clean build directory
- `./gradlew cleanAllTests` - Clean all test results

## Architecture

### Key Technologies
- **Kotlin Multiplatform** (2.2.0) - Multi-target support (currently JVM only)
- **Compose Multiplatform** (1.8.2) - Modern declarative UI framework
- **Material3** - Google's latest design system
- **Decompose** (3.2.0) - Navigation and component lifecycle management
- **MVIKotlin** (4.2.0) - Model-View-Intent architecture implementation
- **Ktor** (3.0.3) - Asynchronous HTTP client for API communication
- **kotlinx.serialization** (1.6.3) - JSON serialization/deserialization
- **Koin** (3.5.6) - Lightweight dependency injection framework

## Architecture Patterns

### MVI (Model-View-Intent) Pattern
The application implements MVI using MVIKotlin:

1. **Model (State)**: Immutable data representing the UI state
2. **View**: Composable functions that render the state  
3. **Intent**: User actions that trigger state changes
4. **Flow**: User Intent → Store → State Update → UI Re-composition

### Decompose Component Pattern

**Key Elements**:
- **Component Interface**: Defines state and intent handling contracts
- **StateFlow**: Reactive state management for UI updates
- **Intent Processing**: User actions processed through `onIntent()`
- **Store Integration**: MVIKotlin store handles business logic

### UI Layer Architecture

1. **Public Screen Function**: `ChatScreen(component: ChatComponent)`
   - Collects state from component
   - Passes state and intent handler to private implementation

2. **Private Implementation**: `ChatScreen(state: State, intentHandler: (Intent) -> Unit)`
   - Renders Material3 UI components
   - Handles user interactions
   - Displays loading states and errors


### Build Configuration
- **Main class**: `com.example.mindweaverstudio.MainKt`
- **Package name**: `com.example.mindweaverstudio`
- **Target formats**: DMG (macOS), MSI (Windows), DEB (Linux)
- **JVM configuration**: 3GB heap for Gradle daemon and Kotlin compiler

## Development Workflow

1. Use `./gradlew hotRunJvm` for development with automatic code reloading
2. Make changes to source files and they will be hot-reloaded automatically
3. Run `./gradlew jvmTest` to execute tests
4. Use `./gradlew check` before commits to ensure all checks pass
5. Package with `./gradlew packageDistributionForCurrentOS` for distribution