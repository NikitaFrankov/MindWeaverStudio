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

### Cleaning
- `./gradlew clean` - Clean build directory
- `./gradlew cleanAllTests` - Clean all test results

## Build Configuration
- **Main class**: `com.example.mindweaverstudio.MainKt`
- **Package name**: `com.example.mindweaverstudio`
- **Target formats**: DMG (macOS), MSI (Windows), DEB (Linux)
- **JVM configuration**: 3GB heap for Gradle daemon and Kotlin compiler

## Key Technologies
- Kotlin Multiplatform (2.2.0) - Multi-target support (currently JVM only)
- Compose Multiplatform (1.8.2) - Modern declarative UI framework
- Material3 - Google's latest design system
- Decompose (3.2.0) - Navigation and component lifecycle management
- MVIKotlin (4.2.0) - Model-View-Intent architecture implementation
- Ktor (3.0.3) - Asynchronous HTTP client for API communication
- kotlinx.serialization (1.6.3) - JSON serialization/deserialization
- Koin (3.5.6) - Lightweight dependency injection framework

## Directories
- composeApp/src/jvmMain/kotlin/com/example/mindweaverstudio/components/root - root component
- composeApp/src/jvmMain/kotlin/com/example/mindweaverstudio/ui/App.kt - root screen
- composeApp/src/jvmMain/kotlin/com/example/mindweaverstudio/components/pipeline - example of how to create a component and a store for feature
- composeApp/src/jvmMain/kotlin/com/example/mindweaverstudio/ui/pipeline - example of how to create a ui for feature

## Development Workflow

1. Use `./gradlew hotRunJvm` for development with automatic code reloading
2. Make changes to source files and they will be hot-reloaded automatically
3. Run `./gradlew jvmTest` to execute tests
4. Use `./gradlew check` before commits to ensure all checks pass