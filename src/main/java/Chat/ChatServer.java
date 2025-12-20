package Chat;

import java.net.*;
import java.io.*;
import java.util.*;



public class ChatServer {
    private static final int PORT = 5000; // Port Number
    private static final Set<ClientHandler> clientHandlers = new HashSet<>(); // Set to store client handlers




    public static void main(String[] args) {
        System.out.println("Chat server starting at port " + PORT);
        try (ServerSocket serverSocket = new ServerSocket(PORT)){ // create server socket
            while (true){
                new ClientHandler(serverSocket.accept()).start(); // accept all incoming connections and start a new thread

            }

        }catch (IOException e){
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread{
        private Socket socket; //
        private PrintWriter out;
        private BufferedReader in;
        private String username;


        public ClientHandler(Socket socket){
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                username = in.readLine(); // Read username sent by client


                synchronized (clientHandlers) {
                    clientHandlers.add(this); // add client to the set of handlers
                }

                String inputLine;
                while ((inputLine = in.readLine()) != null) { // read messages from client
                    if (inputLine.equals("GET_MEMBERS")) {
                        setMemberList(); // send the message list to client upon request
                    } else {
                        sendMessage(inputLine); // send message to all clients
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                removeClient();

            }
        }
        // sends message to all clients that are connected
        private void sendMessage(String message){
            synchronized (clientHandlers){
                for (ClientHandler clientHandler : clientHandlers){
                    clientHandler.out.println(message);
                }
            }
        }
        //sendslist of connected members upon client request
        private void setMemberList(){
            StringBuilder members = new StringBuilder("Members Connected: \n\n");
            synchronized (clientHandlers){
                for (ClientHandler clientHandler : clientHandlers){
                    members.append(clientHandler.username).append("\n");
                }
            }
            out.println(members.toString());

        }
        //remove client handler and close the socket
        private void removeClient() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (clientHandlers) {
                clientHandlers.remove(this); // remove this client handler from handlers set
            }

        }
    }
}
