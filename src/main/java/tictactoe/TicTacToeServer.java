package tictactoe;

import java.io.*;
import java.net.*;
import java.util.*;

public class TicTacToeServer {
    private static final int PORT = 12345;
    private static Map<String, Game> games = new HashMap<>();
    private static Set<String> activeUsernames = new HashSet<>(); // Track active usernames

    /**
     * Main method to start the Tic-Tac-Toe server.
     * Initializes the server socket and listens for incoming client connections.
     * Each new client connection spawns a new thread to handle the client.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;
        private Game game;
        private String state; // "SETUP_USERNAME", "SETUP_GAME", "GAME"
        private boolean clientDisconnected; // Flag to track if the client has disconnected

        /**
         * Constructor for ClientHandler.
         * Initializes the client socket and sets up input/output streams for communication.
         *
         * @param socket The socket connection to the client.
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.state = "SETUP_USERNAME";
            this.clientDisconnected = false;
            try {
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
                clientDisconnected = true;
            }
        }

        /**
         * Retrieves the username of the client.
         *
         * @return The username of the client, or null if not set.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sends a message to the client over the socket.
         * Logs the message being sent and handles any errors during transmission.
         *
         * @param message The message to send to the client.
         */
        public void sendMessage(String message) {
            try {
                System.out.println("Sending to " + (username != null ? username : "unknown") + ": " + message);
                out.println(message);
                out.flush(); // Ensure the message is sent immediately
            } catch (Exception e) {
                System.out.println("Failed to send message to " + (username != null ? username : "unknown") + ": " + e.getMessage());
            }
        }

        /**
         * Main thread execution method for handling a client.
         * Manages the client's lifecycle, including setup and game phases, and ensures cleanup on disconnection.
         */
        @Override
        public void run() {
            try {
                handleClient();
            } catch (IOException e) {
                System.out.println("Client " + (username != null ? username : "unknown") + " disconnected: " + e.getMessage());
                clientDisconnected = true;
            } finally {
                System.out.println("Running cleanup for client " + (username != null ? username : "unknown"));
                cleanup();
            }
        }

        /**
         * Handles the client's interaction with the server.
         * Manages the state machine for the client: username setup, game selection, and gameplay.
         * Transitions between states based on client input and server responses.
         *
         * @throws IOException If an I/O error occurs during communication with the client.
         */
        private void handleClient() throws IOException {
            while (true) {
                if (state.equals("SETUP_USERNAME")) {
                    // Username setup
                    while (true) {
                        if (clientDisconnected) {
                            return; // Exit if the client is already disconnected
                        }
                        sendMessage("ENTER_USERNAME");
                        username = in.readLine();
                        if (username == null) {
                            clientDisconnected = true;
                            return; // Client disconnected
                        }
                        boolean usernameTaken = false;
                        synchronized (activeUsernames) {
                            if (activeUsernames.contains(username)) {
                                usernameTaken = true;
                            } else {
                                activeUsernames.add(username);
                            }
                        }
                        if (usernameTaken) {
                            sendMessage("USERNAME_TAKEN");
                        } else {
                            sendMessage("USERNAME_ACCEPTED");
                            System.out.println("Client " + username + " username accepted");
                            state = "SETUP_GAME";
                            break;
                        }
                    }
                }

                if (state.equals("SETUP_GAME")) {
                    if (clientDisconnected) {
                        return; // Exit if the client is already disconnected
                    }
                    // Game selection
                    sendMessage("SELECT_OPTION:CREATE,JOIN");
                    String option = in.readLine();
                    if (option == null) {
                        clientDisconnected = true;
                        return; // Client disconnected
                    }
                    System.out.println("Client " + username + " selected option: " + option);

                    if ("CREATE".equals(option)) {
                        System.out.println("Client " + username + " entering CREATE flow");
                        while (true) {
                            if (clientDisconnected) {
                                return;
                            }
                            sendMessage("ENTER_GAME_ID");
                            String gameId = in.readLine();
                            if (gameId == null) {
                                clientDisconnected = true;
                                return; // Client disconnected
                            }
                            System.out.println("Client " + username + " sent game ID: " + gameId);
                            synchronized (games) {
                                if (games.containsKey(gameId)) {
                                    sendMessage("GAME_ID_TAKEN");
                                } else {
                                    sendMessage("SELECT_SYMBOL:X,O");
                                    String symbol = in.readLine();
                                    if (symbol == null) {
                                        clientDisconnected = true;
                                        return; // Client disconnected
                                    }
                                    System.out.println("Client " + username + " selected symbol: " + symbol);
                                    char playerSymbol = symbol.charAt(0);
                                    game = new Game(this, gameId, playerSymbol);
                                    games.put(gameId, game);
                                    sendMessage("GAME_CREATED");
                                    state = "GAME";
                                    break;
                                }
                            }
                        }
                    } else if ("JOIN".equals(option)) {
                        System.out.println("Client " + username + " entering JOIN flow");
                        while (true) {
                            if (clientDisconnected) {
                                return;
                            }
                            String availableGames = getAvailableGames();
                            sendMessage("AVAILABLE_GAMES:" + availableGames);
                            String gameId = in.readLine();
                            if (gameId == null) {
                                clientDisconnected = true;
                                return; // Client disconnected
                            }
                            System.out.println("Client " + username + " selected game ID: " + gameId);
                            synchronized (games) {
                                Game game = games.get(gameId);
                                if (game == null || game.isFull()) {
                                    sendMessage("GAME_NOT_FOUND_OR_FULL");
                                } else {
                                    this.game = game;
                                    game.addPlayer(this);
                                    sendMessage("GAME_JOINED");
                                    state = "GAME";
                                    break;
                                }
                            }
                        }
                    }
                }

                if (state.equals("GAME")) {
                    // Game loop
                    while (game != null && !game.isGameOver()) {
                        if (clientDisconnected) {
                            if (game != null) {
                                game.handleDisconnect(this);
                            }
                            return;
                        }
                        String message = in.readLine();
                        if (message == null) {
                            clientDisconnected = true;
                            if (game != null) {
                                game.handleDisconnect(this);
                            }
                            return; // Exit the loop and clean up
                        }
                        game.handleMessage(this, message);
                    }

                    // Game is over, remove the game from the games map
                    if (game != null) {
                        synchronized (games) {
                            System.out.println("Game " + game.getGameId() + " ended, removing from games map");
                            games.remove(game.getGameId());
                        }
                        game = null; // Reset the game for this client
                    }
                    state = "SETUP_GAME"; // Transition to SETUP_GAME instead of SETUP_USERNAME
                }
            }
        }

        /**
         * Cleans up resources associated with the client.
         * Removes the client's username from active usernames, cleans up the game if necessary,
         * and closes the socket connection.
         */
        private void cleanup() {
            // Remove the username from active usernames
            if (username != null) {
                synchronized (activeUsernames) {
                    if (activeUsernames.remove(username)) {
                        System.out.println("Username " + username + " successfully released");
                    } else {
                        System.out.println("Username " + username + " was not found in activeUsernames");
                    }
                }
            } else {
                System.out.println("No username to release for this client");
            }
            // Clean up the game if this client was part of one
            if (game != null) {
                game.handleDisconnect(this);
                synchronized (games) {
                    if (game != null && game.isEmpty()) {
                        System.out.println("Game " + game.getGameId() + " is empty, removing from games map");
                        games.remove(game.getGameId());
                    }
                }
                game = null;
            }
            // Close the socket
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                    System.out.println("Socket closed for client " + (username != null ? username : "unknown"));
                }
            } catch (IOException e) {
                System.out.println("Error closing socket for client " + (username != null ? username : "unknown") + ": " + e.getMessage());
            }
        }
    }

    /**
     * Retrieves a list of available games that a client can join.
     * Returns a string representation of available games in the format "gameId,creator;gameId,creator".
     *
     * @return A string listing available games, or an empty string if none are available.
     */
    private static String getAvailableGames() {
        StringBuilder sb = new StringBuilder();
        synchronized (games) {
            for (Map.Entry<String, Game> entry : games.entrySet()) {
                Game game = entry.getValue();
                if (!game.isFull()) {
                    if (sb.length() > 0) {
                        sb.append(";");
                    }
                    sb.append(entry.getKey()).append(",").append(game.getPlayer1Username());
                }
            }
        }
        return sb.toString();
    }

    private static class Game {
        private ClientHandler player1, player2;
        private String gameId;
        private char player1Symbol;
        private char[][] board = new char[3][3];
        private boolean player1Turn;
        private boolean gameOver;

        /**
         * Constructor for a new Tic-Tac-Toe game.
         * Initializes the game with the first player, game ID, and their chosen symbol.
         *
         * @param player1 The first player (creator of the game).
         * @param gameId The unique identifier for the game.
         * @param symbol The symbol chosen by the first player ('X' or 'O').
         */
        public Game(ClientHandler player1, String gameId, char symbol) {
            this.player1 = player1;
            this.gameId = gameId;
            this.player1Symbol = symbol;
            initializeBoard();
            player1Turn = (symbol == 'O');
            System.out.println("Game created: Player 1 (" + player1.getUsername() + ") is " + symbol + ", player1Turn = " + player1Turn);
        }

        /**
         * Retrieves the game ID.
         *
         * @return The unique identifier for this game.
         */
        public String getGameId() {
            return gameId;
        }

        /**
         * Adds a second player to the game.
         * Assigns the second player a symbol (opposite of player1's symbol) and starts the game.
         *
         * @param player2 The second player joining the game.
         */
        public void addPlayer(ClientHandler player2) {
            this.player2 = player2;
            char player2Symbol = (player1Symbol == 'X') ? 'O' : 'X';
            player1.sendMessage("START:" + player1Symbol + ":opponent:" + player2.getUsername());
            player2.sendMessage("START:" + player2Symbol + ":opponent:" + player1.getUsername());
            System.out.println("Player 2 (" + player2.getUsername() + ") joined as " + player2Symbol + ", player1Turn = " + player1Turn);
            try {
                Thread.sleep(100); // Add a slight delay to ensure clients process the START message
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (player1Turn) {
                System.out.println("Sending YOUR_TURN to Player 1 (" + player1.getUsername() + ")");
                player1.sendMessage("YOUR_TURN");
            } else {
                System.out.println("Sending YOUR_TURN to Player 2 (" + player2.getUsername() + ")");
                player2.sendMessage("YOUR_TURN");
            }
        }

        /**
         * Checks if the game is full (i.e., has two players).
         *
         * @return True if the game has two players, false otherwise.
         */
        public boolean isFull() {
            return player2 != null;
        }

        /**
         * Checks if the game has no players.
         *
         * @return True if both player slots are empty, false otherwise.
         */
        public boolean isEmpty() {
            return player1 == null && player2 == null;
        }

        /**
         * Checks if a given username is a player in this game.
         *
         * @param username The username to check.
         * @return True if the username matches either player, false otherwise.
         */
        public boolean hasPlayer(String username) {
            return (player1 != null && username.equals(player1.getUsername())) ||
                    (player2 != null && username.equals(player2.getUsername()));
        }

        /**
         * Retrieves the username of the first player.
         *
         * @return The username of player1, or null if player1 is not set.
         */
        public String getPlayer1Username() {
            return player1 != null ? player1.getUsername() : null;
        }

        /**
         * Checks if the game is over.
         *
         * @return True if the game has ended (win, draw, or disconnection), false otherwise.
         */
        public boolean isGameOver() {
            return gameOver;
        }

        /**
         * Initializes the Tic-Tac-Toe board.
         * Sets all board positions to empty (' ').
         */
        private void initializeBoard() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    board[i][j] = ' ';
                }
            }
        }

        /**
         * Processes a message from a client during the game.
         * Handles moves made by the client and updates the game state accordingly.
         *
         * @param client The client sending the message.
         * @param message The message received from the client (e.g., "MOVE:row,col").
         */
        public void handleMessage(ClientHandler client, String message) {
            if (message.startsWith("MOVE:")) {
                String[] parts = message.split(":")[1].split(",");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);
                char symbol = (client == player1) ? player1Symbol : (player1Symbol == 'X') ? 'O' : 'X';
                if (client == (player1Turn ? player1 : player2) && board[row][col] == ' ') {
                    board[row][col] = symbol;
                    client.sendMessage("YOUR_MOVE:" + row + "," + col + ":" + symbol);
                    ClientHandler opponent = (client == player1) ? player2 : player1;
                    if (opponent != null) {
                        opponent.sendMessage("OPPONENT_MOVE:" + row + "," + col + ":" + symbol);
                        checkGameState(client, opponent);
                        player1Turn = !player1Turn;
                        if (!gameOver && opponent != null) {
                            opponent.sendMessage("YOUR_TURN");
                        }
                    }
                }
            }
        }

        /**
         * Handles a client disconnection during the game.
         * Notifies the remaining player of the disconnection and marks the game as over.
         *
         * @param client The client that disconnected.
         */
        public void handleDisconnect(ClientHandler client) {
            ClientHandler opponent = (client == player1) ? player2 : player1;
            if (opponent != null) {
                opponent.sendMessage("OPPONENT_DISCONNECTED");
                System.out.println("Notified " + opponent.getUsername() + " of opponent disconnection");
            }
            if (client == player1) {
                player1 = null;
            } else {
                player2 = null;
            }
            gameOver = true;
            System.out.println("Game " + gameId + " marked as over due to disconnection");
        }

        /**
         * Checks the game state after a move to determine if there is a winner or a draw.
         * Notifies both players of the game result and marks the game as over if necessary.
         *
         * @param client The client who made the move.
         * @param opponent The opponent of the client who made the move.
         */
        private void checkGameState(ClientHandler client, ClientHandler opponent) {
            char symbol = (client == player1) ? player1Symbol : (player1Symbol == 'X') ? 'O' : 'X';
            if (checkWin(symbol)) {
                System.out.println("Game " + gameId + " ended: " + client.getUsername() + " (" + symbol + ") wins");
                client.sendMessage("YOU_WIN");
                if (opponent != null) {
                    opponent.sendMessage("YOU_LOSE");
                }
                client.sendMessage("RESET");
                if (opponent != null) {
                    opponent.sendMessage("RESET");
                }
                gameOver = true;
            } else if (checkDraw()) {
                System.out.println("Game " + gameId + " ended in a draw");
                client.sendMessage("DRAW");
                if (opponent != null) {
                    opponent.sendMessage("DRAW");
                }
                try {
                    Thread.sleep(100); // Small delay to ensure both clients receive the message
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                client.sendMessage("RESET");
                if (opponent != null) {
                    opponent.sendMessage("RESET");
                }
                gameOver = true;
            }
        }

        /**
         * Checks if the current player has won the game.
         * Examines rows, columns, and diagonals for a winning combination.
         *
         * @param symbol The symbol of the player to check for a win ('X' or 'O').
         * @return True if the player has won, false otherwise.
         */
        private boolean checkWin(char symbol) {
            for (int i = 0; i < 3; i++) {
                if (board[i][0] == symbol && board[i][1] == symbol && board[i][2] == symbol) return true;
                if (board[0][i] == symbol && board[1][i] == symbol && board[2][i] == symbol) return true;
            }
            if (board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol) return true;
            if (board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol) return true;
            return false;
        }

        /**
         * Checks if the game has ended in a draw.
         * A draw occurs when the board is full with no winner.
         *
         * @return True if the game is a draw, false otherwise.
         */
        private boolean checkDraw() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j] == ' ') return false;
                }
            }
            return true;
        }
    }
}