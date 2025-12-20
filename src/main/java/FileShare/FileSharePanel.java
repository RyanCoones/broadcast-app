package FileShare;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

public class FileSharePanel extends JPanel {

    protected JList<String> serverFileList;
    protected DefaultListModel<String> serverFileListModel = new DefaultListModel<>();

    public FileSharePanel() {
        // set the layout of the panel to GridBagLayout
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        // initialize some constraints
        c.insets = new Insets(10, 10, 10, 10);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        c.weightx = 1;

        // button panel to hold both buttons vertically
        JPanel buttonPanel = new JPanel(new GridLayout(3, 2, 10, 10));

        // create upload button
        JButton uploadButton = new JButton("UPLOAD");
        ImageIcon uploadIcon = new ImageIcon("src/main/resources/Icons/upload.png");
        ImageIcon uploadButtonIcon = new ImageIcon(uploadIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        uploadButton.setIcon(uploadButtonIcon);
        uploadButton.setIconTextGap(14);
        buttonPanel.add(uploadButton);

        // create download button
        JButton downloadButton = new JButton("DOWNLOAD");
        ImageIcon downloadIcon = new ImageIcon("src/main/resources/Icons/download.png");
        ImageIcon downloadButtonIcon = new ImageIcon(downloadIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        downloadButton.setIcon(downloadButtonIcon);
        downloadButton.setIconTextGap(14);
        buttonPanel.add(downloadButton);

        // create button to delete an item from the list
        JButton deleteButton = new JButton("DELETE");
        ImageIcon deleteIcon = new ImageIcon("src/main/resources/Icons/delete.png");
        ImageIcon deleteButtonIcon = new ImageIcon(deleteIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        deleteButton.setIcon(deleteButtonIcon);
        deleteButton.setIconTextGap(14);
        buttonPanel.add(deleteButton);

        // create button to refresh list
        JButton refreshButton = new JButton("REFRESH");
        ImageIcon refreshIcon = new ImageIcon("src/main/resources/Icons/refresh.png");
        ImageIcon refreshButtonIcon = new ImageIcon(refreshIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        refreshButton.setIcon(refreshButtonIcon);
        refreshButton.setIconTextGap(14);
        buttonPanel.add(refreshButton, BorderLayout.SOUTH);

        // add the action listeners for the buttons
        uploadButton.addActionListener(e -> uploadAction());
        downloadButton.addActionListener(e -> downloadAction());
        refreshButton.addActionListener(e -> refreshList());
        deleteButton.addActionListener(e -> deleteAction());

        // add button panel
        c.gridx = 0;
        c.gridy = 0;
        c.weighty = 0;
        add(buttonPanel, c);

        // create list panel with BorderLayout
        JPanel listPanel = new JPanel(new BorderLayout());

        // create label for the server file list
        JLabel serverFileListLabel = new JLabel("Server Files:", JLabel.CENTER);
        listPanel.add(serverFileListLabel, BorderLayout.NORTH);

        // create the list and scroll pane
        serverFileList = new JList<>(serverFileListModel);
        JScrollPane scrollPane = new JScrollPane(serverFileList);
        scrollPane.setPreferredSize(new Dimension(0, 0));
        listPanel.add(scrollPane, BorderLayout.CENTER);

        // Add list panel
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(listPanel, c);

        // initial refresh of the list
        refreshList();
    }

    public void uploadAction() {
        sendCommandToServer("UPLOAD"); // send upload request to the server
        refreshList(); // refresh the lists
    }

    public void downloadAction() {
        sendCommandToServer("DOWNLOAD"); // send the download request to the server
        refreshList(); // refresh the lists
    }

    public void deleteAction() {
        sendCommandToServer("DELETE");
        refreshList();
    }

    // function to open a new connection and sending a command to the server
    private void sendCommandToServer(String command) {
        try (Socket socket = new Socket("localhost",8080);
             // after connecting to the server, create a buffered reader and printwriter to communicate with the server
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // send the command to the server
            out.println(command);

            // if the command is upload, we need to send a file name and the contents of that file
            if (command.equalsIgnoreCase("upload")) {
                ArrayList<String> acceptedExtensions = new ArrayList<>(Arrays.asList(".txt",".md",".csv",".log",".doc",".docx",".json",".html",".asc",".msg"));
                String filename = "";
                String filepath = "";
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY); // let the user select files only
                int returnVal = fileChooser.showOpenDialog(this);

                // If the user selected a valid file, set the filename to the chosen file
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    filename = file.getName(); // get the file name to send to server
                    filepath = file.getAbsolutePath(); // get the path to read the content of the file to the server
                    // make sure the file is a supported file type!
                    if (filename.lastIndexOf('.') <= 0 ||
                            !acceptedExtensions.contains(filename.substring(filename.lastIndexOf('.')).toLowerCase())) {
                        JOptionPane.showMessageDialog(this, "Unsupported file type!");
                        return;
                    }
                } else {
                    System.out.println("No selection, operation cancelled.");
                    return;
                }

                out.println(filename);
                readFile(filepath, out);
            }

            // if the command is upload, we need to send a file name and the contents of that file
            if (command.equalsIgnoreCase("delete")) {
                String filename = serverFileList.getSelectedValue();

                //Prompt user to make sure they didn't misclick
                int choice = JOptionPane.showConfirmDialog(this, "Are you sure you would like to delete " + filename + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }

                // If the file selected is null, return
                if (filename == null) {
                    return;
                } else {
                    // send filename to server
                    out.println(filename);
                }

            }

            // if the command is download we need to send
            else if (command.equalsIgnoreCase("download")) {
                // we need the name of the desired file from the user
                String filename = serverFileList.getSelectedValue();

                //Prompt user to make sure they didn't misclick
                int choice = JOptionPane.showConfirmDialog(this, "Are you sure you would like to download " + filename + "?", "Confirm Download", JOptionPane.YES_NO_OPTION);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }

                // If the file selected is null, return
                if (filename == null) {
                    return;
                } else {
                    out.println(filename);
                }

                String downloadDirectory = "";
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // let the user select directories only
                int returnVal = fileChooser.showOpenDialog(this);

                // If the user selected a valid directory, set the filepath to the chosen directory
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    downloadDirectory = file.getAbsolutePath(); // get the path to save the new file to
                }

                File file = new File(downloadDirectory, filename); // create a new file in the client files directory
                System.out.println("Writing the files content to: " + file.getAbsolutePath());

                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) { // make a new writer for the new file
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) { // go through the non-null lines (these are the contents of the text file)
                        writer.println(inputLine);
                        writer.flush();
                    }

                } catch (IOException e) { // print stack trace in the event of an error
                    e.printStackTrace();
                }
            }

            // Read the server's response
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println(response);
            }

        } catch (IOException e) { // let the user know if a problem has occurred
            e.printStackTrace();
        }
    }

    public ArrayList<String> getServerFileList() {
        ArrayList<String> serverFileList = new ArrayList<>(); // create an array for the servers file names
        try (Socket socket = new Socket("localhost",8080);
             // after connecting to the server, create a buffered reader and printwriter to communicate with the server
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // send the command to the server
            out.println("DIR");

            // Read the server's response line by line
            String response;
            while ((response = in.readLine()) != null) {
                serverFileList.add(response); // add each file name to the list of the servers files
            }

        } catch (IOException e) { // notify the user of any problems that occurred
            e.printStackTrace();
        }
        return serverFileList; // return the list of file names
    }

    public void readFile(String filename, PrintWriter out) {
        File file = new File(filename); // get the file
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) { // read the file line by line and send the contents to the server
                out.println(line);
            }
            out.println("end-of-file"); // send a signal to the server marking the end of the file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshList() {
        // Clear the list before we refresh
        serverFileListModel.clear();

        // Get the content of the servers shared file directory by sending the dir command to the server
        ArrayList<String> serverFileList = getServerFileList();
        if (serverFileList != null) {
            for (String serverFile : serverFileList) {
                this.serverFileListModel.addElement(serverFile); // add the file names to the jlist
            }
        }
    }

}