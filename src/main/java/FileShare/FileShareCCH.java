package FileShare;

import java.io.*;
import java.net.Socket;


public class FileShareCCH implements Runnable {
    private final Socket clientSocket; // ClientConnectionHandler should have a client socket


    // simple constructor for a ClientConnectionHandler
    public FileShareCCH(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        // Create a new buffered reader and PrintWriter
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String inputLine = in.readLine();
            if (inputLine == null) return;

            // handles the dir command to list the files in the server directory
            if (inputLine.equalsIgnoreCase("dir")) {
                System.out.println("Directory request received!");
                directoryHandler(out);
            }

            //handles the upload command by writing uploaded file to the server
            else if (inputLine.equalsIgnoreCase("upload")) {
                System.out.println("Upload request received!");
                String fileName = in.readLine(); // get the file name from the client
                if (fileName != null) {
                    uploadHandler(out, fileName, in); // handle the users request if the file name is not null
                }

            }

            //handles the download command by sending the file content to the client
            else if (inputLine.equalsIgnoreCase("download")) {
                System.out.println("Download request received!");
                String fileName = in.readLine();
                if (fileName != null) {
                    downloadHandler(out,fileName);
                }
            }

            //handles the delete command by removing the file in SharedFiles
            else if (inputLine.equalsIgnoreCase("delete")) {
                System.out.println("Delete request received");
                String fileName = in.readLine();
                if (fileName != null) {
                    deleteHandler(out,fileName);
                }
            }

        } catch (Exception e) { // print stack trace in the event of an error
            e.printStackTrace();
        }
    }

    private void directoryHandler(PrintWriter out) {
        File folder = new File("src/main/resources/SharedFiles"); // opens the given directory
        String[] listOfFiles = folder.list(); // gets list of files in directory

        if (listOfFiles != null) { // if the directory is empty return
            for (String file : listOfFiles) { // iterates through the files
                out.println(file); // sends each file
            }
        }

        try {
            clientSocket.close(); // close the connection after handling the clients request
        } catch (IOException e) { // print stack trace in the event of an error
            e.printStackTrace();
        }

    }

    private void uploadHandler(PrintWriter out, String fileName, BufferedReader in) {
        File file = new File("src/main/resources/SharedFiles", fileName); // create a new file in the server files directory
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) { // make a new writer for the new file
            String inputLine;
            while (!(inputLine = in.readLine()).equals("end-of-file")) { // go through the non-null lines (these are the contents of the text file)
                writer.println(inputLine);
                writer.flush();
            }

            out.println("Uploaded : " + fileName); // let the client know the file has been uploaded

        } catch (IOException e) { // print stack trace in the event of an error
            e.printStackTrace();
        }

        try {
            clientSocket.close(); // close the connection after handling the clients request
            System.out.println("Connection closed");
        } catch (IOException e) { // print stack trace in the event of an error
            e.printStackTrace();
        }

        }

    private void downloadHandler(PrintWriter out, String fileName) {
        File file = new File ("src/main/resources/SharedFiles",fileName); // try to open the desired file
        if (!file.exists()) { // if the file wasn't found in the server files directory, let the client know
            System.out.println("File not found");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                out.println(inputLine); // send each line to the client
                out.flush(); // ensures the line is sent immediately
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            clientSocket.close();  // close the connection after handling the clients request
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void deleteHandler(PrintWriter out, String fileName) {
        try {
            File file = new File("src/main/resources/SharedFiles", fileName);
            if (!file.exists()) {
                System.out.println("File not found");
                return;
            } else {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            clientSocket.close(); // close the connection after handling the clients request
            System.out.println("Connection closed");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




}




