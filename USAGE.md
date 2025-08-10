# MindWeaver Studio - DeepSeek Chat Integration

## Overview

MindWeaver Studio now includes a fully functional chat interface for communicating with the DeepSeek neural network API. The application features a modern Material3 UI with real-time messaging capabilities.

## Features

- **Real-time Chat Interface**: Clean, modern chat UI with message bubbles
- **DeepSeek Integration**: Direct integration with DeepSeek's API for neural network responses
- **Error Handling**: Comprehensive error handling with user-friendly messages
- **Loading States**: Visual feedback during API requests
- **Message History**: Persistent conversation history within the session
- **Clear Chat**: Ability to clear conversation history

## Setup

### API Key Configuration

To use the DeepSeek integration, you need to set up your API key:

1. **Environment Variable** (Recommended):
   ```bash
   export DEEPSEEK_API_KEY="your-actual-deepseek-api-key"
   ```

2. **Default Placeholder**: The app uses `"sk-your-api-key-here"` as a placeholder if no environment variable is set.

### Running the Application

```bash
# Development with hot reload
./gradlew hotRunJvm

# Standard run
./gradlew run

# Build and package
./gradlew build
```

## Architecture

The application follows modern architectural patterns:

- **MVI (Model-View-Intent)**: State management using MVIKotlin
- **Decompose**: Component lifecycle and navigation management
- **Ktor**: HTTP client for API communication
- **Material3**: Modern UI components and design system

### Key Components

1. **ChatScreen**: The main UI component with message list and input
2. **ChatComponent**: Decompose component handling state and events
3. **ChatStore**: MVIKotlin store managing chat state
4. **DeepSeekApiClient**: Ktor-based HTTP client for API communication
5. **Repository Layer**: Abstraction over network calls

## Usage

1. **Start a Conversation**: Type a message in the input field at the bottom
2. **Send Messages**: Click the send button or press enter
3. **View Responses**: DeepSeek responses appear as assistant messages
4. **Clear Chat**: Use the clear button in the header to reset conversation
5. **Error Handling**: Errors are displayed with dismiss options

## API Integration

The app communicates with DeepSeek's chat completion endpoint:
- **Endpoint**: `https://api.deepseek.com/v1/chat/completions`
- **Model**: `deepseek-chat` (default)
- **Features**: Temperature control, token limits, message history

## Extending the Application

The architecture is designed for extensibility:

- **Multiple Providers**: Add new neural network providers by implementing `NeuralNetworkRepository`
- **Model Selection**: Extend UI to allow model switching
- **Settings**: Add configuration options for temperature, max tokens, etc.
- **Persistence**: Implement local storage for conversation history

## Troubleshooting

1. **API Key Issues**: Ensure your DeepSeek API key is valid and has sufficient credits
2. **Network Errors**: Check internet connectivity and API endpoint availability
3. **Build Issues**: Ensure all dependencies are correctly configured in `gradle/libs.versions.toml`

The application provides comprehensive error messages to help diagnose and resolve issues.