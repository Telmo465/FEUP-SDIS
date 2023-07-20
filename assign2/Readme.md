# Client-Server Application for Multiplayer Blackjack Game

The client-server application is designed to facilitate multiplayer gameplay of the popular card game Blackjack. Multiple clients can connect to a central server and share a Blackjack experience. The client-server application consists of three key files that work harmoniously to enable seamless communication and interaction between clients and the server: 

The Client.java file represents the client side of the application; it establishes a connection with the server using a socket channel and registers it with a selector for I/O operations. The Client class provides methods for connecting to the server, sending and receiving messages, and handling different game events. In the main method, it creates multiple instances of the Client class to simulate multiple clients connecting to the server; it uses the SocketChannel to send messages to the server and receive responses; the client's status is updated based on the responses received from the server.

The Server.java file represents the server side of the application; It uses a thread pool of workers to process the clients' requests. The server runs an infinite loop using a Selector to handle multiplexed I/O operations on the server socket, thus allowing a great number of connections; Using a ServerSocketChannel, it listens for incoming client connections on a specified port so that when a client connects, it parses requests into messages as defined in MessageModel.java. It then sends the messages to a message queue so the workers can get them to process and answer the requests.

The Worker.java file represents a worker thread that handles the processing of client requests; The Worker class implements the Runnable interface, allowing it to be executed concurrently. The Worker class is responsible for processing client requests; it waits for incoming messages from the client in the MessageQueue, performs the necessary operations (such as game logic), and sends back the responses;

The GameEventTask.java is initiated by the server so that it can help the synchronization of the threads by signaling the workers to wake up and check the status of the games so that it can send the clients the appropriate game events, for example, the start of a game.

The synchronization model between all the threads is done in two ways, simple lock and unlock operations, as well as wait-for operations. The main server thread receives the requests and sends them to the MessageQueue, to do so, it uses a lock and unlock operation to insert the message; meanwhile, the workers are asleep in the waiting state for new messages in the queue. When the main thread of the server locks and unlocks the MessageQueue, it also notifies the worker threads with a wait operation in the conditional lock for the MessageQueue.

When the server runs as ranked, the clients can connect to the server but will not be associated with a specific game until the defined --time-to-wait passes. For this purpose GameEventTask checks if the elapsed time has reached the end of the --time-to-wait. It then sets the game as started, and wakes up the workers, so they can create games and allocate the players by rank to each game.

---

More detailed view of each file and its surrounding functions:

##  SERVER.JAVA

Server class: The main class contains the server's entry point (main method) and other methods for server initialization and handling client connections.

Global Variables:
- RankGameStarted: A boolean flag indicating whether a ranked game has started.
- PlayersLock: A lock for thread synchronization when accessing the Players collection.
- Players: A HashMap to store player information using their token as the key.
- GamesLock: A lock for thread synchronization when accessing the Games collection.
- Games: A HashMap to store game information using game ID as the key.
- MessageQueueLock: A lock for thread synchronization when accessing the MessageQueue.
- notEmpty: A condition variable to signal that the message queue is not empty.
- MessageQueue: An ArrayDeque to store incoming messages from clients.
- ServerOptions: An instance of the ServerOptionsModel class with various server configuration options.
- TimerScheduler: A timer for scheduling game events.

main method: The entry point of the server application. It initializes server options, loads persistence data, creates worker threads, schedules game events, and starts the server.

ProcessArgs method: Parses command-line arguments to customize server options.

LoadPersistance method: Loads player information from a file ("tokens.txt") and populates the Players collection.

SavePersistance method: Saves player information from the Players collection to the file ("tokens.txt").

start method: Sets up the server socket and selector and enters the main listening loop to accept and process client connections and messages.

Listen method: The main loop for handling events from the selector. It checks for new connections, reads messages from clients, and dispatches them to the message queue for processing.

NewConnection method: Handles a new client connection by accepting it, configuring it for non-blocking mode, and registering it with the selector for read events.

ReadFromConnection method: Reads incoming data from a client's socket channel and constructs a message model from the received data. The message model is then added to the message queue for further processing.

sendMessage method: Sends a message to a client by writing the message's string representation to the client's socket channel.

printPlayersStatus method: Prints the server's current status, including the number of active games and players, as well as their respective ranks and scores.

---

## CLIENT.JAVA

The Client class represents a client that connects to a server to play the Blackjack game.

It contains a ClientStatus enum representing the client's status during different game stages.

The client establishes a socket connection with the server using SocketChannel and registers with a Selector to handle I/O operations asynchronously.

The PlayerModel class represents the player's information, such as username, password, and game-related data.

The GameModel class represents the game's state and contains information about players, cards, scores, and game status.

The Client class maintains various lists and variables to keep track of game-specific data, such as the number of players, player turns, dealer cards, player cards, scores, etc.

The Client class has methods for connecting to the server, sending and receiving messages, and updating the game state based on the received messages.

The main method demonstrates how multiple clients can be created and connected to the server to play the game simultaneously.

The ClientsResponse method is used to handle responses from the server and update the client's status accordingly.

The code includes methods for reading buffers, handling messages received from the server, and retrieving player information from persistence storage.

---

## WORKER.JAVA

The run() method is the entry point for the worker thread. It contains an infinite loop that continuously checks for messages to process and works on the game.

The checkMessageQueue() method retrieves a message from the message queue and processes it if available. It uses a lock to ensure thread safety when accessing the message queue.

The checkOnGames() method determines the type of game (ranked or simple) and calls the corresponding method to check the game's status.

In the checkOnGamesRanked() method, if a ranked game is started, it calls the processGameStartRanked() method to handle the game start logic.

The processGameStartRanked() method retrieves a list of players who are not currently in a game and are ready to start. It adds these players to the game and sends update messages to the players.

The checkOnGamesSimple() method handles simple games' start lobby and start game logic.

The processStartLobby() method starts the game lobby countdown when there are enough players. It retrieves a list of games that have not started and can be started, then sends update messages to the players.

The processStartGame() method starts the game when enough players are in the lobby. It retrieves a list of games waiting in the lobby that can be started, then sends update messages to the players.

The sendUpdatePlayers() method sends update messages to the players.

The processMessageQueue() method processes a message received from the message queue. It determines the action of the message and calls the corresponding method to handle it.

The processConnectAction() method handles the connect action, which occurs when a player connects to the server.

It checks if the player already exists, creates a new player if necessary, adds the player to a game, and sends a response message.

The processCloseAction() method handles the close action, which occurs when a player disconnects from the server. It removes the player from the game, sends update messages to the players, and closes the player's socket.

The processPlayAction() method handles the play action, which occurs when a player sends a play request. It retrieves the player, updates their rank, and sends a response message.

The addPlayerToGame() method adds a player to a game. It finds an available game or creates a new one if necessary and adds the player.

The addPlayersToGames() method adds a list of players to games. It creates multiple games based on the number of players and assigns players to these games.

—

# Compiling

```
$ mvn compile
```

# Running

## Server
To run the server, we can tune several settings using the command arguments:

```
$ java -cp target/classes com.BlackJackGame.BlackjackServer.Server -h
Welcome to BlackJackGame!
Options:
        --threads=N      Number of worker threads
        --simple|--ranked        Type of game simple or ranked
        --players-per-game=N     Players per game
        --time-to-wait=N         Time to wait in the lobby
        --persistent-file=N      File to load/save information of players
```

the default values are:
```
--threads=2
--simple
--time-to-wait=30
--persistent-file=tokens.txt
```

### Run Simple server
```
java -cp target/classes com.BlackJackGame.BlackjackServer.Server --simple --threads=2
```
### Run Ranked server
```
java -cp target/classes com.BlackJackGame.BlackjackServer.Server --ranked --threads=2
```

---
## Client

To run the Client

```
$ java -cp target/classes com.BlackJackGame.BlackjackClient.Client [<interactive> <numberClients> <persistantFile>] 
```
By default, the Client assumes the following options:
```
number-of-clients = 2
persistent-file = token.txt
```

### Run only as 1 client in interactive mode

```
$ java -cp target/classes com.BlackJackGame.BlackjackClient.Client i 1
```

### Run 20 clients in auto mode


```
$ java -cp target/classes com.BlackJackGame.BlackjackClient.Client a 20
```