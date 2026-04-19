# Distributed Chat System (Java)

## Features
- Unique username login (duplicate usernames rejected)
- Chat rooms:
    - create
    - join
    - leave
- Only one active room per user
- Real-time push messaging (no polling)
- BlockingQueue per room
- Multithreaded server
- Thread-safe shared structures using `ReentrantLock`
- JavaFX GUI client

## Requirements
- Java 17+ (recommended)
- JavaFX SDK (if not bundled in your IDE)

## Compile & Run (IntelliJ easiest)
1. Create project and place files under `src/` with package structure.
2. Add JavaFX library to project.
3. Run server:
    - `server.ChatServer`
4. Run multiple clients (at least 3 instances):
    - `client.ChatClientApp`

## Manual test scenario
1. Start server on port `5555`
2. Open 3 clients with usernames: `u1`, `u2`, `u3`
3. Create room `room1` from client 1
4. Join `room1` from all clients
5. Send messages from each client
6. Verify:
    - all receive messages in real-time
    - format includes sender + timestamp
7. Try duplicate username (should be rejected)

## Architecture summary
- `ChatServer` accepts sockets and spawns `ClientHandler`s
- `ClientHandler` handles commands and user state
- `ChatRoom` contains:
    - `BlockingQueue<Message>`
    - dedicated dispatcher thread
    - member set with synchronization
- `NetworkClient` has receiving thread for push events
- JavaFX client updates UI via `Platform.runLater`

## Known issues / possible improvements
- Add persistent message history
- Add private/direct messages
- Add authentication/password
- Add better reconnect logic
- Use thread pool with bounds for production