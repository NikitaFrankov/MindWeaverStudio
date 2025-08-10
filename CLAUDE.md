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

### Project Structure
```
composeApp/src/jvmMain/kotlin/com/example/mindweaverstudio/
├── main.kt                     # Application entry point
├── ui/
│   ├── App.kt                  # Root composable UI entry point
│   └── chat/
│       └── ChatScreen.kt       # Chat UI implementation
├── components/
│   ├── root/
│   │   ├── RootComponent.kt    # Root component interface
│   │   └── DefaultRootComponent.kt # Root component with DI logic
│   └── chat/
│       ├── ChatComponent.kt    # Chat component interface
│       ├── DefaultChatComponent.kt # Chat component implementation
│       ├── ChatStore.kt        # MVI store interface
│       └── ChatStoreFactory.kt # Store implementation factory
├── data/
│   ├── model/
│   │   ├── NeuralNetworkProvider.kt # Provider definitions
│   │   └── deepseek/
│   │       ├── ChatMessage.kt  # Message data model
│   │       ├── ChatRequest.kt  # API request model
│   │       └── ChatResponse.kt # API response model
│   ├── network/
│   │   └── DeepSeekApiClient.kt # Ktor HTTP client
│   └── repository/
│       ├── NeuralNetworkRepository.kt # Repository interface
│       └── deepseek/
│           └── DeepSeekRepositoryImpl.kt # Repository implementation
└── di/
    └── AppModule.kt            # Koin dependency injection module
```

### Main Components
- **main.kt** - Creates the desktop window using Compose Desktop `application` and `Window`
- **App.kt** - Root composable UI entry point that initializes the RootComponent
- **RootComponent** - Foundation component managing business logic, DI setup, and child component creation
- **ChatScreen.kt** - Material3-based chat interface with message history and input field
- **ChatComponent** - Decompose component managing chat state and user interactions

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

**Flow**: User Intent → Store → State Update → UI Re-composition

### Network Integration
- **API Provider**: Currently integrated with OpenRouter (proxying DeepSeek)
- **Base URL**: `https://openrouter.ai/api/v1`
- **Model**: `deepseek/deepseek-chat`
- **Authentication**: Bearer token authentication
- **Error Handling**: Comprehensive error handling with user-friendly messages

### Decompose Component Pattern
Components follow a structured approach:

```kotlin
interface ChatComponent {
    val state: StateFlow<ChatStore.State>
    fun onIntent(intent: ChatStore.Intent)
}

class DefaultChatComponent(
    componentContext: ComponentContext,
    private val chatStoreFactory: ChatStoreFactory
) : ChatComponent, ComponentContext by componentContext
```

**Key Elements**:
- **Component Interface**: Defines state and intent handling contracts
- **StateFlow**: Reactive state management for UI updates
- **Intent Processing**: User actions processed through `onIntent()`
- **Store Integration**: MVIKotlin store handles business logic

### Chat Store (MVI State Management)

```kotlin
data class State(
    val messages: List<ChatMessage> = emptyList(),
    val currentMessage: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedModel: String = "deepseek-chat"
)

sealed class Intent {
    data class UpdateMessage(val message: String) : Intent()
    data object SendMessage : Intent()
    data object ClearError : Intent()
    data object ClearChat : Intent()
    data class ChangeModel(val model: String) : Intent()
}
```

### UI Layer Architecture
The UI follows a two-function pattern:

1. **Public Screen Function**: `ChatScreen(component: ChatComponent)`
   - Collects state from component
   - Passes state and intent handler to private implementation

2. **Private Implementation**: `ChatScreen(state: State, intentHandler: (Intent) -> Unit)`
   - Renders Material3 UI components
   - Handles user interactions
   - Displays loading states and errors

### Repository Pattern
Clean separation between data sources and business logic:

```kotlin
interface NeuralNetworkRepository {
    suspend fun sendMessage(
        messages: List<ChatMessage>,
        model: String = "deepseek-chat",
        temperature: Double = 0.7,
        maxTokens: Int = 1000
    ): Result<String>
}
```

**Implementation**:
- **DeepSeekRepositoryImpl**: Concrete implementation using DeepSeekApiClient
- **Result<T>** wrapper for safe error handling
- **Flexible parameters** for different AI models and configurations

## Data Models

### Core Models
```kotlin
@Serializable
data class ChatMessage(
    val role: String,    // "user", "assistant", "system"
    val content: String
)

@Serializable  
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 1000,
    val stream: Boolean = false
)
```

### API Response Handling
Flexible response model supporting both success and error cases:

```kotlin
@Serializable
data class ChatResponse(
    val id: String? = null,
    @SerialName("object") val chatObject: String? = null,
    val created: Long? = null,
    val model: String? = null,
    val choices: List<Choice>? = null,
    val usage: Usage? = null,
    val error: ErrorDetail? = null    // Error handling
)
```

## Dependency Injection

### Current Setup (Component-Based DI)
The application uses manual dependency injection within the `RootComponent`:

**App.kt** (Clean UI entry point):
```kotlin
@Composable
fun App() {
    MaterialTheme {
        val lifecycle = remember { LifecycleRegistry() }
        val componentContext = remember { DefaultComponentContext(lifecycle) }
        
        val rootComponent = remember {
            DefaultRootComponent(componentContext)
        }
        
        ChatScreen(rootComponent.chatComponent)
    }
}
```

**DefaultRootComponent** (Business logic and DI):
```kotlin
class DefaultRootComponent(componentContext: ComponentContext) : RootComponent {
    override val chatComponent: ChatComponent by lazy {
        val apiClient = DeepSeekApiClient(apiKey)
        val repository = DeepSeekRepositoryImpl(apiClient)
        val storeFactory = DefaultStoreFactory()
        val chatStoreFactory = ChatStoreFactory(storeFactory, repository)
        
        DefaultChatComponent(this, chatStoreFactory)
    }
}
```

### Available Koin Module
A Koin module exists for future use:

```kotlin
val appModule = module {
    single<StoreFactory> { DefaultStoreFactory() }
    single { DeepSeekApiClient(System.getenv("DEEPSEEK_API_KEY")) }
    single<NeuralNetworkRepository> { DeepSeekRepositoryImpl(get()) }
    single { ChatStoreFactory(get(), get()) }
}
```

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

## API Configuration

### Environment Variables
- `DEEPSEEK_API_KEY` - Set this environment variable with your API key
- Default fallback key is embedded in the code for development

### Neural Network Provider Configuration
The app is designed to support multiple AI providers through the `NeuralNetworkProvider` model:

```kotlin
data class NeuralNetworkProvider(
    val id: String,
    val name: String,
    val baseUrl: String,
    val models: List<String>
)
```

Currently configured for DeepSeek via OpenRouter, but extensible for other providers.