package FileShare;

import java.net.ServerSocket;
import java.net.Socket;

public class FileShareServer {

    public static void main(String[] args) {
        // try to start listening on port 8080
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("Listening on port 8080...");
            // Continuously listen for client connections
            while (true) {
                Socket clientSocket = serverSocket.accept();  // Accept an incoming client connection
                System.out.println("Accepted connection from " + clientSocket.getRemoteSocketAddress());
                new Thread(new FileShareCCH(clientSocket)).start(); // Create a new thread to handle the client connection
            }
        } catch (Exception e) { // if there was an error starting the server socket, print the stack trace
            e.printStackTrace();
        }
    }

}