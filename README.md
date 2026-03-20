# ByteRace (Multiplayer Snake Game)

## Version 5

ByteRace is a multiplayer snake game built in Java. It operates on a robust Client-Server architecture and supports real-time multiplayer gameplay over a custom network protocol.

### Project Architecture

#### 1. Client-Server Model
- **Server (`Server/`)**: The server orchestrates the game state. It listens for incoming TCP connections on a designated port. For every connecting client, the server spawns a dedicated handler thread (`ClientHandler`) to process inputs concurrently without blocking the game logic.
- **Client (`client/`)**: Features a Graphic User Interface (GUI) to render the game state and capture player keystrokes. It communicates with the server to broadcast the player's moves and listens for game state updates.
- **Shared (`shared/`)**: Contains common models used by both client and server, cleanly separating the network payload definitions from the application logic.

#### 2. Network Protocol
- **Communication Type**: TCP Sockets.
- **Serialization**: The game leverages Java's `ObjectOutputStream` and `ObjectInputStream` to send and receive rich objects natively instead of raw byte streams or serialized strings.
- **Message Types**: Encompasses various lifecycle states including `REQUEST_NAME`, `NAME_RESPONSE`, `INFO_ASSIGN`, and `MOVE`.

#### 3. Game State Management
- The game board is tracked centrally on the Server as a 2D array grid (`char[][] board`).
- Snakes and players (`PlayerInfo`, `Snake`) are instantiated on the server and then distributed to clients. The server synchronizes and validates all moves before broadcasting the new game state.

### Project Structure
- `Server/`: Contains `SnakeServer.java` and `ClientHandler.java` for orchestrating game logic and network communication.
- `client/`: Contains the GUI code (e.g. `SnakeClientGUI.java`) and rendering components.
- `shared/`: Shared data models (`Message.java`, `Snake.java`, `PlayerInfo.java`, `Move.java`).
