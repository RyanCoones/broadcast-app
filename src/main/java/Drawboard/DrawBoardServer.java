package Drawboard;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DrawBoardServer {
    private ServerSocket serverSocket;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final List<DrawBoardClient.DrawingData> drawingHistory = new CopyOnWriteArrayList<>();

    /**
     * creates and starts the server on the port and gets ready to accept new clients
     *
     * @param port port number
     */
    public DrawBoardServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Drawing Board Server started on port " + port);
        new Thread(this::acceptClients).start();
    }

    /**
     * continuously accepts new client connections utilizing multithreading.
     */
    private void acceptClients() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(clientSocket);

                synchronized (clients) {
                    clients.add(handler);
                }

                // Send current history to new client
                sendInitialState(handler);

                new Thread(handler).start();
                System.out.println("New client connected. Total clients: " + clients.size());
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.println("Server accept error: " + e.getMessage());
            }
        }
    }

    /**
     * sends first state of canvas size and drawing history to a new client when initialized.
     *
     * @param handler The client handler to initialize
     */
    private void sendInitialState(ClientHandler handler) throws IOException {
        synchronized (drawingHistory) {
            if (drawingHistory.isEmpty()) {
                handler.send(new DrawBoardClient.InitCommand(800, 600));
            } else {
                handler.send(new DrawBoardClient.InitCommand(800, 600));
                for (DrawBoardClient.DrawingData data : drawingHistory) {
                    handler.send(data);
                }
            }
        }
    }

    /**
     * broadcasts data to all connected clients except "exclude" maintaining drawing history.
     *
     * @param data  data to broadcast (DrawingData or ClearCommand)
     * @param exclude client handler to exclude from broadcasting (sender)
     */
    private void broadcast(Object data, ClientHandler exclude) throws IOException {
        if (data instanceof DrawBoardClient.DrawingData) {
            synchronized (drawingHistory) {
                drawingHistory.add((DrawBoardClient.DrawingData) data);
            }
        } else if (data instanceof DrawBoardClient.ClearCommand) {
            synchronized (drawingHistory) {
                drawingHistory.clear();
            }
        }

        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client != exclude) {
                    client.send(data);
                }
            }
        }
    }

    /**
     * removes a client from the active clients list when they disconnect.
     *
     * @param client client handler to remove
     */
    private void removeClient(ClientHandler client) {
        synchronized (clients) {
            clients.remove(client);
        }
        System.out.println("Client disconnected. Total clients: " + clients.size());
    }

    /**
     * client handler for clients ready for multithreading
     */
    private class ClientHandler implements Runnable {
        private final Socket socket;
        private final ObjectOutputStream output;
        private final ObjectInputStream input;

        /**
         * creates a new client handler for the socket.
         *
         * @param socket client's socket connection
         */
        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.output = new ObjectOutputStream(socket.getOutputStream());
            this.input = new ObjectInputStream(socket.getInputStream());
        }

        /**
         * sends data to a client.
         *
         * @param data data to send
         */
        public void send(Object data) throws IOException {
            output.writeObject(data);
            output.flush();
        }

        /**
         * main client handling loop that processes incoming messages and commands
         */
        @Override
        public void run() {
            try {
                while (!socket.isClosed()) {
                    Object received = input.readObject();
                    broadcast(received, this);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Client handler error: " + e.getMessage());
            } finally {
                removeClient(this);
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }

    /**
     * main entry point for starting the drawing board server (testing purposes) creating a server instance listening on port 5050.
     */
    public static void main(String[] args) {
        try {
            new DrawBoardServer(5050);
        } catch (IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
        }
    }
}