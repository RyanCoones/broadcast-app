package tictactoe;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class TicTacToeClient extends JFrame {
    private JButton[][] buttons = new JButton[3][3];
    private char playerSymbol;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private boolean myTurn;
    private JLabel statusLabel;
    private volatile String clientState; // "SETUP_USERNAME", "SETUP_GAME", "GAME"
    private volatile boolean setupComplete; // Flag to signal setup completion
    private volatile String setupMessage; // To pass setup messages from listener to main thread
    private Queue<String> gameMessageQueue; // To queue game messages during setup
    private volatile boolean listening; // To control the listenToServer thread

    /**
     * Constructor for the TicTacToe client.
     * Initializes the UI and starts the connection to the server.
     */
    public TicTacToeClient() {
        setTitle("Tic Tac Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize UI but don't show the window yet
        initializeUI();
        // Connect to server and show the window after setup
        connectToServer();
    }

    /**
     * Establishes a connection to the server and manages the client's setup process.
     * Handles username setup, game selection, and transitions to the game phase.
     * Displays the game window after setup is complete.
     */
    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            clientState = "SETUP_USERNAME";
            setupComplete = false;
            setupMessage = null;
            gameMessageQueue = new LinkedList<>();
            listening = true;

            // Start the listener thread
            new Thread(this::listenToServer).start();

            // Handle username setup
            while (clientState.equals("SETUP_USERNAME")) {
                synchronized (this) {
                    while (setupMessage == null && clientState.equals("SETUP_USERNAME")) {
                        System.out.println((username != null ? username : "null") + " waiting for setup message in SETUP_USERNAME");
                        wait();
                    }
                    if (setupMessage == null) {
                        throw new IOException("Server disconnected during username setup");
                    }
                    String message = setupMessage;
                    setupMessage = null; // Clear the message after processing

                    if ("ENTER_USERNAME".equals(message)) {
                        username = JOptionPane.showInputDialog(this, "Enter username:");
                        if (username == null) {
                            System.exit(0); // User canceled
                        }
                        System.out.println(username + " sending username: " + username);
                        out.println(username);
                    } else if ("USERNAME_TAKEN".equals(message)) {
                        JOptionPane.showMessageDialog(this, "Username taken, choose another");
                    } else if ("USERNAME_ACCEPTED".equals(message)) {
                        System.out.println("Username accepted: " + username);
                        clientState = "SETUP_GAME";
                    } else {
                        System.out.println(username + " received unexpected message in SETUP_USERNAME: " + message);
                    }
                }
            }

            // Handle game selection
            handleGameSelection();

            // Setup phase is complete, process any queued game messages
            synchronized (this) {
                setupComplete = true;
                while (!gameMessageQueue.isEmpty()) {
                    String queuedMessage = gameMessageQueue.poll();
                    System.out.println(username + " processing queued game message: " + queuedMessage);
                    processGameMessage(queuedMessage);
                }
                notifyAll();
            }

            // Now that setup is complete, show the game window
            SwingUtilities.invokeLater(() -> setVisible(true));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to server: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Manages the game selection phase for the client.
     * Allows the client to create a new game or join an existing one.
     * Transitions to the game phase upon successful game creation or joining.
     *
     * @throws IOException If an I/O error occurs during communication with the server.
     * @throws InterruptedException If the thread is interrupted while waiting for messages.
     */
    private void handleGameSelection() throws IOException, InterruptedException {
        while (clientState.equals("SETUP_GAME")) {
            synchronized (this) {
                while (setupMessage == null && clientState.equals("SETUP_GAME")) {
                    System.out.println(username + " waiting for setup message in SETUP_GAME");
                    wait();
                }
                if (setupMessage == null) {
                    throw new IOException("Server disconnected during game selection");
                }
                String message = setupMessage;
                setupMessage = null; // Clear the message after processing

                if ("SELECT_OPTION:CREATE,JOIN".equals(message)) {
                    String[] options = {"Create Game", "Join Game"};
                    int choice = JOptionPane.showOptionDialog(this,
                            "What would you like to do?",
                            "Game Selection",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);
                    if (choice == -1) {
                        System.exit(0); // User canceled
                    }
                    String option = (choice == 0) ? "CREATE" : "JOIN";
                    System.out.println(username + " sending option: " + option);
                    out.println(option);

                    if ("CREATE".equals(option)) {
                        while (clientState.equals("SETUP_GAME")) {
                            while (setupMessage == null && clientState.equals("SETUP_GAME")) {
                                System.out.println(username + " waiting for setup message in SETUP_GAME (CREATE)");
                                wait();
                            }
                            if (setupMessage == null) {
                                throw new IOException("Server disconnected during game creation");
                            }
                            message = setupMessage;
                            setupMessage = null;

                            if ("ENTER_GAME_ID".equals(message)) {
                                String gameId = JOptionPane.showInputDialog(this, "Enter Game ID:");
                                if (gameId == null) {
                                    System.exit(0); // User canceled
                                }
                                System.out.println(username + " sending game ID: " + gameId);
                                out.println(gameId);
                            } else if ("GAME_ID_TAKEN".equals(message)) {
                                JOptionPane.showMessageDialog(this, "Game ID taken, choose another");
                            } else if ("SELECT_SYMBOL:X,O".equals(message)) {
                                String[] symbols = {"X", "O"};
                                playerSymbol = JOptionPane.showOptionDialog(this,
                                        "Choose your symbol (O goes first)",
                                        "Symbol Selection",
                                        JOptionPane.DEFAULT_OPTION,
                                        JOptionPane.QUESTION_MESSAGE,
                                        null,
                                        symbols,
                                        symbols[1]) == 0 ? 'X' : 'O';
                                System.out.println(username + " sending symbol: " + playerSymbol);
                                out.println(playerSymbol);
                            } else if ("GAME_CREATED".equals(message)) {
                                System.out.println("Game created for " + username);
                                clientState = "GAME";
                                break;
                            } else {
                                System.out.println(username + " received unexpected message in SETUP_GAME (CREATE): " + message);
                                if ("ENTER_USERNAME".equals(message)) {
                                    clientState = "SETUP_USERNAME";
                                    setupMessage = message;
                                    break;
                                }
                            }
                        }
                    } else if ("JOIN".equals(option)) {
                        while (clientState.equals("SETUP_GAME")) {
                            while (setupMessage == null && clientState.equals("SETUP_GAME")) {
                                System.out.println(username + " waiting for setup message in SETUP_GAME (JOIN)");
                                wait();
                            }
                            if (setupMessage == null) {
                                throw new IOException("Server disconnected during game join");
                            }
                            message = setupMessage;
                            setupMessage = null;

                            if (message.startsWith("AVAILABLE_GAMES:")) {
                                String availableGames = message.split(":")[1];
                                Map<String, String> gameMap = new HashMap<>();
                                if (!availableGames.isEmpty()) {
                                    String[] games = availableGames.split(";");
                                    for (String game : games) {
                                        String[] parts = game.split(",");
                                        if (parts.length == 2) {
                                            gameMap.put(parts[0], parts[1]);
                                        }
                                    }
                                }

                                JPanel joinPanel = new JPanel(new BorderLayout());
                                DefaultListModel<String> listModel = new DefaultListModel<>();
                                for (String gameId : gameMap.keySet()) {
                                    listModel.addElement(gameId);
                                }
                                JList<String> gameList = new JList<>(listModel);
                                gameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                                JScrollPane scrollPane = new JScrollPane(gameList);

                                JPanel directJoinPanel = new JPanel();
                                JTextField gameIdField = new JTextField(10);
                                JButton directJoinButton = new JButton("Join");
                                directJoinPanel.add(new JLabel("Enter Game ID:"));
                                directJoinPanel.add(gameIdField);
                                directJoinPanel.add(directJoinButton);

                                joinPanel.add(new JLabel("Available Games:"), BorderLayout.NORTH);
                                joinPanel.add(scrollPane, BorderLayout.CENTER);
                                joinPanel.add(directJoinPanel, BorderLayout.SOUTH);

                                final String[] selectedGameId = {null};

                                gameList.addMouseListener(new MouseAdapter() {
                                    public void mouseClicked(MouseEvent evt) {
                                        if (evt.getClickCount() == 2) {
                                            String gameId = gameList.getSelectedValue();
                                            if (gameId != null) {
                                                selectedGameId[0] = showGameInfoDialog(gameId, gameMap.get(gameId), out);
                                                if (selectedGameId[0] != null) {
                                                    ((JDialog) joinPanel.getTopLevelAncestor()).dispose();
                                                }
                                            }
                                        }
                                    }
                                });

                                directJoinButton.addActionListener(e -> {
                                    String gameId = gameIdField.getText().trim();
                                    if (!gameId.isEmpty()) {
                                        selectedGameId[0] = gameId;
                                        ((JDialog) joinPanel.getTopLevelAncestor()).dispose();
                                    }
                                });

                                JDialog dialog = new JDialog(TicTacToeClient.this, "Join Game", true);
                                dialog.add(joinPanel);
                                dialog.setSize(300, 200);
                                dialog.setLocationRelativeTo(TicTacToeClient.this);
                                dialog.setVisible(true);

                                if (selectedGameId[0] == null) {
                                    System.exit(0); // Exit if no selection made
                                }

                                System.out.println(username + " sending game ID to join: " + selectedGameId[0]);
                                out.println(selectedGameId[0]);
                            } else if ("GAME_NOT_FOUND_OR_FULL".equals(message)) {
                                JOptionPane.showMessageDialog(TicTacToeClient.this, "Game not found or full");
                                continue; // Stay in the loop to try again
                            } else if ("GAME_JOINED".equals(message)) {
                                System.out.println(username + " joined the game");
                                // Reset the board and state before joining a new game
                                for (int i = 0; i < 3; i++) {
                                    for (int j = 0; j < 3; j++) {
                                        if (buttons[i][j] != null) {
                                            buttons[i][j].setText("");
                                        }
                                    }
                                }
                                myTurn = false;
                                System.out.println(username + " initial myTurn after joining: " + myTurn);
                                clientState = "GAME";
                                break;
                            } else {
                                System.out.println(username + " received unexpected message in SETUP_GAME (JOIN): " + message);
                                if ("ENTER_USERNAME".equals(message)) {
                                    clientState = "SETUP_USERNAME";
                                    setupMessage = message;
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    System.out.println(username + " received unexpected message in SETUP_GAME: " + message);
                    if ("ENTER_USERNAME".equals(message)) {
                        clientState = "SETUP_USERNAME";
                        setupMessage = message;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Processes game-related messages received from the server.
     * Updates the game state, board, and UI based on messages like moves, game results, and turn notifications.
     *
     * @param message The message received from the server.
     */
    private void processGameMessage(String message) {
        if (message.startsWith("START:")) {
            String[] parts = message.split(":");
            playerSymbol = parts[1].charAt(0);
            if (statusLabel != null) {
                statusLabel.setText("Playing against " + parts[3] + " (You are " + playerSymbol + ")");
            }
            System.out.println(username + " started game as " + playerSymbol + ", myTurn = " + myTurn);
        } else if ("YOUR_TURN".equals(message)) {
            myTurn = true;
            if (statusLabel != null) {
                statusLabel.setText("Your turn (You are " + playerSymbol + ")");
            }
            System.out.println(username + " received YOUR_TURN, myTurn set to " + myTurn);
        } else if (message.startsWith("YOUR_MOVE:")) {
            String[] parts = message.split(":");
            String[] moveParts = parts[1].split(",");
            int row = Integer.parseInt(moveParts[0]);
            int col = Integer.parseInt(moveParts[1]);
            String symbol = parts[2];
            buttons[row][col].setText(symbol);
            System.out.println(username + " updated board with their move: " + symbol + " at (" + row + "," + col + "), myTurn = " + myTurn);
        } else if (message.startsWith("OPPONENT_MOVE:")) {
            String[] parts = message.split(":");
            String[] moveParts = parts[1].split(",");
            int row = Integer.parseInt(moveParts[0]);
            int col = Integer.parseInt(moveParts[1]);
            String opponentSymbol = parts[2];
            buttons[row][col].setText(opponentSymbol);
            System.out.println(username + " updated board with opponent's move: " + opponentSymbol + " at (" + row + "," + col + "), myTurn = " + myTurn);
        } else if ("YOU_WIN".equals(message)) {
            if (statusLabel != null) {
                statusLabel.setText("You win!");
            }
            endGame("You win!");
        } else if ("YOU_LOSE".equals(message)) {
            if (statusLabel != null) {
                statusLabel.setText("You lose!");
            }
            endGame("You lose!");
        } else if ("DRAW".equals(message)) {
            System.out.println(username + " received DRAW message");
            if (statusLabel != null) {
                statusLabel.setText("Game ended in a tie!");
            }
            endGame("The game is a tie!");
        } else if ("OPPONENT_DISCONNECTED".equals(message)) {
            System.out.println(username + " received OPPONENT_DISCONNECTED message");
            if (statusLabel != null) {
                statusLabel.setText("Opponent disconnected!");
            }
            endGame("Opponent disconnected!");
        } else if ("RESET".equals(message)) {
            System.out.println(username + " received RESET message");
            // No additional action needed; endGame() was already called for YOU_WIN, YOU_LOSE, or DRAW
        } else {
            System.out.println(username + " received unexpected game message: " + message);
        }
    }

    /**
     * Handles the end of a game.
     * Displays the game result in a popup, stops the listener thread, cleans up resources, and exits the application.
     *
     * @param message The game result message to display (e.g., "You win!").
     */
    private void endGame(String message) {
        SwingUtilities.invokeLater(() -> {
            // Show the game result in a message dialog
            JOptionPane.showMessageDialog(this,
                    message,
                    "Game Over",
                    JOptionPane.INFORMATION_MESSAGE);
            // Stop the listener thread
            listening = false;
            // Clean up resources
            try {
                if (socket != null) {
                    socket.close();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Exit the application
            System.exit(0);
        });
    }

    /**
     * Displays a dialog with game information and allows the user to join or cancel.
     * Used when a user selects a game to join from the list of available games.
     *
     * @param gameId The ID of the game.
     * @param creator The username of the game's creator.
     * @param out The PrintWriter to send messages to the server.
     * @return The selected game ID if the user chooses to join, or null if they cancel.
     */
    private String showGameInfoDialog(String gameId, String creator, PrintWriter out) {
        JPanel infoPanel = new JPanel(new GridLayout(3, 1));
        infoPanel.add(new JLabel("Game ID: " + gameId));
        infoPanel.add(new JLabel("Creator: " + creator));
        JPanel buttonPanel = new JPanel();
        JButton joinButton = new JButton("Join");
        JButton cancelButton = new JButton("Cancel");
        buttonPanel.add(joinButton);
        buttonPanel.add(cancelButton);

        final String[] result = {null};

        JDialog infoDialog = new JDialog(this, "Game Info", true);
        infoPanel.add(buttonPanel);
        infoDialog.add(infoPanel);
        infoDialog.pack();
        infoDialog.setLocationRelativeTo(this);

        joinButton.addActionListener(e -> {
            result[0] = gameId;
            infoDialog.dispose();
        });

        cancelButton.addActionListener(e -> {
            result[0] = null;
            infoDialog.dispose();
        });

        infoDialog.setVisible(true);

        return result[0];
    }

    /**
     * Initializes the graphical user interface for the Tic-Tac-Toe game.
     * Sets up the 3x3 grid of buttons, status label, and window properties.
     */
    private void initializeUI() {
        JPanel gamePanel = new JPanel(new GridLayout(3, 3));
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("SansSerif", Font.PLAIN, 40));
                buttons[i][j].addActionListener(new ButtonListener(i, j));
                gamePanel.add(buttons[i][j]);
            }
        }

        statusLabel = new JLabel("Waiting for opponent...");
        add(gamePanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        setSize(300, 350);
        setLocationRelativeTo(null);
    }

    private class ButtonListener implements ActionListener {
        private int row, col;

        /**
         * Constructor for ButtonListener.
         * Associates the listener with a specific button on the Tic-Tac-Toe board.
         *
         * @param row The row of the button.
         * @param col The column of the button.
         */
        public ButtonListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        /**
         * Handles a button click event on the Tic-Tac-Toe board.
         * Sends a move to the server if it's the client's turn and the button is empty.
         *
         * @param e The action event triggered by the button click.
         */
        public void actionPerformed(ActionEvent e) {
            System.out.println(username + " attempting to make a move at (" + row + "," + col + "), myTurn = " + myTurn);
            if (myTurn && buttons[row][col].getText().isEmpty()) {
                out.println("MOVE:" + row + "," + col);
                myTurn = false;
                statusLabel.setText("Opponent's turn");
                System.out.println(username + " made a move, myTurn set to " + myTurn);
            } else {
                System.out.println(username + " cannot move: myTurn = " + myTurn + ", button text = " + buttons[row][col].getText());
            }
        }
    }

    /**
     * Listens for messages from the server in a separate thread.
     * Processes setup messages during the setup phase and game messages during the game phase.
     * Handles server disconnection and notifies the user if the connection is lost.
     */
    private void listenToServer() {
        try {
            while (listening) {
                String message = in.readLine();
                if (message == null) {
                    throw new IOException("Server disconnected");
                }
                System.out.println((username != null ? username : "null") + " received message: " + message + " in state: " + clientState);

                synchronized (this) {
                    if (!setupComplete) {
                        // During setup phase, handle setup messages and queue game messages
                        if (clientState.equals("SETUP_USERNAME") || clientState.equals("SETUP_GAME")) {
                            if (message.equals("ENTER_USERNAME") ||
                                    message.equals("USERNAME_TAKEN") ||
                                    message.equals("USERNAME_ACCEPTED") ||
                                    message.equals("SELECT_OPTION:CREATE,JOIN") ||
                                    message.equals("ENTER_GAME_ID") ||
                                    message.equals("GAME_ID_TAKEN") ||
                                    message.equals("SELECT_SYMBOL:X,O") ||
                                    message.equals("GAME_CREATED") ||
                                    message.startsWith("AVAILABLE_GAMES:") ||
                                    message.equals("GAME_NOT_FOUND_OR_FULL") ||
                                    message.equals("GAME_JOINED")) {
                                // Only set setupMessage if we're expecting a setup message
                                if ((clientState.equals("SETUP_USERNAME") &&
                                        (message.equals("ENTER_USERNAME") || message.equals("USERNAME_TAKEN") || message.equals("USERNAME_ACCEPTED"))) ||
                                        (clientState.equals("SETUP_GAME") &&
                                                (message.equals("SELECT_OPTION:CREATE,JOIN") || message.equals("ENTER_GAME_ID") ||
                                                        message.equals("GAME_ID_TAKEN") || message.equals("SELECT_SYMBOL:X,O") ||
                                                        message.equals("GAME_CREATED") || message.startsWith("AVAILABLE_GAMES:") ||
                                                        message.equals("GAME_NOT_FOUND_OR_FULL") || message.equals("GAME_JOINED")))) {
                                    setupMessage = message;
                                    System.out.println((username != null ? username : "null") + " notifying main thread with setup message: " + message);
                                    notifyAll();
                                } else {
                                    System.out.println((username != null ? username : "null") + " received unexpected setup message: " + message + " in state: " + clientState);
                                }
                            } else {
                                // Queue game-related messages
                                System.out.println((username != null ? username : "null") + " queuing game message during setup: " + message);
                                gameMessageQueue.add(message);
                            }
                        }
                    } else {
                        // Game phase, handle messages directly
                        processGameMessage(message);
                    }
                }
            }
        } catch (IOException e) {
            if (listening) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lost connection to server: " + e.getMessage());
                    System.exit(1);
                });
            }
        }
    }

    /**
     * Main method to start the TicTacToe client application.
     * Launches the client UI on the Event Dispatch Thread.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(TicTacToeClient::new);
    }
}