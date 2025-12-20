package Drawboard;

import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;

public class DrawBoardClient {
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private DrawBoardGUI gui;

    /**
     * creates a DrawBoardClient and connects to the specified server.
     * starts the GUI and starts a thread to listen for server updates.
     *
     * @param serverAddress The IP address or hostname of the server
     * @param serverPort The port number the server is listening on
     */
    public DrawBoardClient(String serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.output = new ObjectOutputStream(socket.getOutputStream());
        this.input = new ObjectInputStream(socket.getInputStream());

        SwingUtilities.invokeLater(() -> {
            gui = new DrawBoardGUI(this);
            gui.setVisible(true);
        });

        new Thread(this::listenForUpdates).start();
    }

    /**
     * Sends drawing data (line coordinates, color, and size) to the server.
     *
     * @param x1 first x-coordinate
     * @param y1 first y-coordinate
     * @param x2 last x-coordinate
     * @param y2 last y-coordinate
     * @param colorRGB color of the line
     * @param size thickness
     */
    public void sendDrawingData(int x1, int y1, int x2, int y2, int colorRGB, int size) throws IOException {
        output.writeObject(new DrawingData(x1, y1, x2, y2, colorRGB, size));
        output.flush();
    }

    /**
     * Sends a clear command to the server to clear all drawings.
     */
    public void sendClearCommand() throws IOException {
        output.writeObject(new ClearCommand());
        output.flush();
    }

    /**
     * uses multithreading listens continuously for updates from the server and processes them and handles three types of commands: initialization, drawing data, and clear commands.
     */
    private void listenForUpdates() {
        try {
            while (!socket.isClosed()) {
                Object received = input.readObject();

                if (received instanceof InitCommand) {
                    InitCommand init = (InitCommand) received;
                    SwingUtilities.invokeLater(() -> gui.initializeCanvas(init.width, init.height));
                }
                else if (received instanceof DrawingData) {
                    DrawingData data = (DrawingData) received;
                    SwingUtilities.invokeLater(() ->
                            gui.updateDrawing(data.x1, data.y1, data.x2, data.y2, data.colorRGB, data.size));
                }
                else if (received instanceof ClearCommand) {
                    SwingUtilities.invokeLater(() -> gui.clearAll());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!socket.isClosed()) {
                System.err.println("Error in client listener: " + e.getMessage());
            }
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    /**
     * serializable class representing drawing data to be sent between client and server.
     */
    public static class DrawingData implements Serializable {
        public final int x1, y1, x2, y2, colorRGB, size;

        public DrawingData(int x1, int y1, int x2, int y2, int colorRGB, int size) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.colorRGB = colorRGB;
            this.size = size;
        }
    }

    /**
     * serializable class representing a command to clear the drawing canvas.
     */
    public static class ClearCommand implements Serializable {}

    /**
     * serializable class representing initialization data for the drawing canvas and contains width and height information for the canvas.
     */
    public static class InitCommand implements Serializable {
        public final int width, height;
        public InitCommand(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}