package Chat;

import Customization.CustomizationFrame;
import Drawboard.DrawBoardClient;
import FileShare.FileSharePanel;
import com.formdev.flatlaf.FlatDarkLaf;
import tictactoe.TicTacToeClient;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import java.text.BreakIterator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;


public class ChatApplicationGUI extends JFrame {

    // initialize chat and text area for sending and reading messages
    private JTextPane chatArea;
    private JTextPane chatInput;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;
    //username color palette
    private static final Color[] namecolors = {
            new Color(161, 7, 2), //Red
            new Color(244, 71, 8),//Orange
            new Color(255, 214, 57),//Yellow
            new Color(0, 175, 84),//Green
            new Color(58, 134, 255),//Blue
            new Color(131, 56, 236),//Indigo
            new Color(255, 0, 110)// Violet

    };
    // Maps usernames to certain color index
    private HashMap<String, Integer> nameColors = new HashMap<>();
    public int total_colors;

    // Getter for the username
    public String getUsername() {
        return username;
    }

    public ChatApplicationGUI() {
        // initialize the customization userConfig
        File userConfig = new File("src/main/resources/themes/FlatDarkLaf.properties");

        // Check if the userConfig exists
        if (userConfig.exists()) {
            try {
                // apply user customizations from properties file
                FlatDarkLaf.registerCustomDefaultsSource("themes"); //.toURI().toString()
                FlatDarkLaf.setup();
            } catch (Exception e) {
                System.err.println("Error loading the customizations: " + e.getMessage());
                e.printStackTrace();
                // fallback to default dark theme
                setThemeDarkMode();
            }
        } else {
            System.out.println("Customizations file not found. Using default dark theme.");
            // fallback to default dark theme
            setThemeDarkMode();
        }

        // setup main window
        setTitle("Broadcast");
        setSize(1310, 780);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // use gridbaglayout
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        // gets username
        username = JOptionPane.showInputDialog(this, "Enter Username", "Username", JOptionPane.PLAIN_MESSAGE);
        // terminate creation of the gui if the user did not enter a username
        if (username == null) {
            System.out.println("No username entered.");
            return;
        }

        // panel to hold chat stuff
        JPanel chatPanel = new JPanel(new GridBagLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("Chat"));
        chatPanel.setPreferredSize(new Dimension(900,650));


        // chat display + text entry
        chatArea = new JTextPane();
        // dont let users edit text
        chatArea.setEditable(false);
        // set font based on customization
        JScrollPane scrollPane = new JScrollPane(chatArea);


        // text input for sending messages
        chatInput = new JTextPane();
        chatInput.setPreferredSize(new Dimension(800, 50));
        chatInput.setMinimumSize(new Dimension(800, 50));
        chatInput.setMaximumSize(new Dimension(800, 50));
        chatInput.setCaretPosition(chatInput.getDocument().getLength());


        // input text wrapping
        chatInput.addKeyListener(new KeyAdapter() {
            public void keyTyped(KeyEvent e){
                FontMetrics fm = chatInput.getFontMetrics(chatInput.getFont());
                String text = chatInput.getText();
                String line = chatInput.getText().substring(text.lastIndexOf("\n")+ 1);
                int inputWidth = chatInput.getWidth();
                int fontWidth = fm.stringWidth(line);


                if (fontWidth > inputWidth - 25 ) {

                    BreakIterator boundary = BreakIterator.getWordInstance();
                    boundary.setText(line);
                    int lastSpace = boundary.preceding(line.length());

                    if (lastSpace != BreakIterator.DONE && lastSpace >= 0) {
                        chatInput.setText(text.substring(0, text.lastIndexOf("\n") + 1) + line.substring(0, lastSpace) + "\n" + line.substring(lastSpace));
                    } else {
                        chatInput.setText(text + "\n");
                    }
                }
            }
            // shortcut for bolding text
            public void keyPressed(KeyEvent e) {

                if(e.isControlDown()) {
                    switch(e.getKeyCode()) {
                        case KeyEvent.VK_B:
                            textStyle("bold");
                            break;
                        case KeyEvent.VK_I:
                            textStyle("italic");
                            break;
                        case KeyEvent.VK_U:
                            textStyle("underline");
                            break;

                    }
                }
                else if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                    e.consume();
                }
            }
        });

        // send button
        JButton sendButton = new JButton("Send");
        // when user presses button do something
        sendButton.addActionListener(e -> sendMessage());

        // Add the chat frame to the layout
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.weighty = 0.9;
        constraints.fill = GridBagConstraints.BOTH;
        chatPanel.add(scrollPane, constraints);

        // Add the text input to the layout
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.weighty = 0.01;
        chatPanel.add(chatInput, constraints);

        // Add the send button to the layout
        constraints.gridx = 1;
        constraints.weightx = 0.2;
        chatPanel.add(sendButton, constraints);


        // panel to hold all the buttons
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        // customize Button
        JButton customizeButton = new JButton("Customize");
        ImageIcon customizeIcon = new ImageIcon("src/main/resources/Icons/customize.png");
        ImageIcon customizeButtonIcon = new ImageIcon(customizeIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        customizeButton.setIcon(customizeButtonIcon);
        customizeButton.setIconTextGap(14);
        customizeButton.addActionListener(e -> new CustomizationFrame(this));
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10, 10, 10, 10);
        constraints.fill = GridBagConstraints.BOTH;
        buttonPanel.add(customizeButton, constraints);

        // drawboard Button
        JButton drawboardButton = new JButton("Drawboard");
        ImageIcon drawboardIcon = new ImageIcon("src/main/resources/Icons/drawboard.png");
        ImageIcon drawboardButtonIcon = new ImageIcon(drawboardIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        drawboardButton.setIcon(drawboardButtonIcon);
        drawboardButton.setIconTextGap(14);
        drawboardButton.addActionListener(e -> {
            try {
                new DrawBoardClient("localhost", 5050);
            } catch (IOException error) {
                System.err.println("Could not connect to drawboard server: " + error.getMessage());
                System.exit(1);
            }
        });
        constraints.gridy = 1;
        buttonPanel.add(drawboardButton, constraints);

        // TicTacToe Button
        JButton ticTacToeButton = new JButton("TicTacToe");
        ImageIcon ticTacToeIcon = new ImageIcon("src/main/resources/Icons/tictactoe.png");
        ImageIcon ticTacToeButtonIcon = new ImageIcon(ticTacToeIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        ticTacToeButton.setIcon(ticTacToeButtonIcon);
        ticTacToeButton.setIconTextGap(14);
        ticTacToeButton.addActionListener(e -> {
            try {
                new TicTacToeClient();
            } catch (Exception error) {
                System.err.println("Could not connect to tictactoe server: " + error.getMessage());
            }
        });
        constraints.gridy = 2;
        buttonPanel.add(ticTacToeButton, constraints);

        // about Button
        JButton aboutButton = new JButton("About");
        ImageIcon aboutIcon = new ImageIcon("src/main/resources/Icons/about.png");
        ImageIcon aboutButtonIcon = new ImageIcon(aboutIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        aboutButton.setIcon(aboutButtonIcon);
        aboutButton.setIconTextGap(28);
        aboutButton.addActionListener(e -> {
            File readmeFile = new File("about.txt");
            // Open readme in new frame
            JFrame readmeFrame = new JFrame("About Broadcast");
            readmeFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            readmeFrame.setSize(700, 900);
            readmeFrame.setLocationRelativeTo(null);
            // Create Textarea for the instructions
            JTextArea readmeTextArea = new JTextArea();
            readmeTextArea.setEditable(false);
            readmeTextArea.setLineWrap(true);
            readmeTextArea.setWrapStyleWord(true);

            // Read contents of readme.md
            try(BufferedReader reader = new BufferedReader(new FileReader(readmeFile))){
                String line;
                while ((line = reader.readLine()) != null) {
                    // Write contents to textarea line by line
                    readmeTextArea.append(line+"\n");
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            JScrollPane readmeScrollPane = new JScrollPane(readmeTextArea);
            readmeFrame.add(readmeScrollPane);
            readmeFrame.setVisible(true);
        });
        constraints.gridy = 3;
        buttonPanel.add(aboutButton, constraints);

        // Members list button
        JButton membersButton = new JButton("Members");
        ImageIcon membersIcon = new ImageIcon("src/main/resources/Icons/people.png");
        ImageIcon membersButtonIcon = new ImageIcon(membersIcon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH));
        membersButton.setIcon(membersButtonIcon);
        membersButton.setIconTextGap(28);
        membersButton.addActionListener(e -> {
            getMembers();
        });
        constraints.gridy = 4;
        buttonPanel.add(membersButton, constraints);


        // Add the shared files panel to the button panel
        constraints.gridy = 5;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        buttonPanel.add(new FileSharePanel(),constraints);

        // add both panels to the frame
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0.5;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        add(chatPanel, constraints);

        constraints.gridx = 1;
        constraints.weightx = 0.5;
        add(buttonPanel, constraints);

        chat();
    }

    private void chat(){
        try{
            socket = new Socket("localhost", 5000);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            //join chat message
            out.println(username);
            out.println(username + " has joined the chat");
            //left chat detector
            this.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    out.println(username + " has left the chat");
                    out.flush();
                }
            });


            new Thread(() -> {
                try{
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        if (inputLine.startsWith("Members Connected:")){
                            StringBuilder msg = new StringBuilder(inputLine);
                            while (in.ready()){
                                msg.append("\n").append(in.readLine());

                            }
                            JOptionPane.showMessageDialog(this, msg.toString(), "Members", JOptionPane.INFORMATION_MESSAGE);


                        }else {
                            styleMessage(inputLine);
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }).start();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    // sends message to chat
    private void sendMessage(){
        try {
            String message = chatInput.getText().trim();
            if (!message.isEmpty()) {
                ;
                String msg = (new SimpleDateFormat("hh:mm a").format(new Date()) + " " + username + ": " + message);
                out.println(msg);
                out.flush();

                //styleMessage(msg);
                chatInput.setText("");
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    //styles text
    private void textStyle(String style){
        StyledDocument doc = chatInput.getStyledDocument();
        SimpleAttributeSet attributeSet = new SimpleAttributeSet();
        switch (style){
            case "bold":
                StyleConstants.setBold(attributeSet, true);
                break;
            case "italic":
                StyleConstants.setItalic(attributeSet, true);
                break;
            case "underline":
                StyleConstants.setUnderline(attributeSet, true);
                break;
        }
        int start = chatInput.getSelectionStart();
        int end = chatInput.getSelectionEnd();
        if (start != end){
            doc.setCharacterAttributes(start, end - start, attributeSet, false);
        }
    }
    //sends message to selver

    private void getMembers(){
        out.println("GET_MEMBERS");
    }


    //styles message send to chat
    private void styleMessage(String message) {

        try{


            //change username color
            StyledDocument doc = chatArea.getStyledDocument();

            if(message.contains("has joined the chat")) {
                SimpleAttributeSet attributeSet = new SimpleAttributeSet();
                StyleConstants.setForeground(attributeSet, new Color(0,150,0));


                doc.insertString(doc.getLength(), message + "\n", attributeSet);
            }else if(message.contains("has left the chat")) {
                SimpleAttributeSet attributeSet = new SimpleAttributeSet();
                StyleConstants.setForeground(attributeSet, new Color(255, 70, 70));
                doc.insertString(doc.getLength(), message + "\n", attributeSet);
            } else {
                String[] parts = message.split(" ", 4);

                //name color
                SimpleAttributeSet nameStyle = new SimpleAttributeSet();
                String user = parts[2];
                if(!nameColors.containsKey(user)) {
                    nameColors.put(user, total_colors % namecolors.length);
                    total_colors++;
                }

                Color color = namecolors[nameColors.get(user)];


                StyleConstants.setForeground(nameStyle, color);
                StyleConstants.setBold(nameStyle, true);

                //changes time color
                SimpleAttributeSet timeStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(timeStyle, Color.GRAY);
                StyleConstants.setBold(timeStyle, true);

                //changes am/pm color
                SimpleAttributeSet amPmStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(amPmStyle, Color.GRAY);
                StyleConstants.setBold(amPmStyle, true);

                //changes message color
                SimpleAttributeSet messageStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(messageStyle, new Color(187, 187, 187));

                //splits message into 4 components: username, time, am/pm, message
                if (parts.length == 4) {
                    doc.insertString(doc.getLength(), parts[0] + " ", timeStyle);
                    doc.insertString(doc.getLength(), parts[1] + " ", amPmStyle);
                    doc.insertString(doc.getLength(), parts[2] + " ", nameStyle);
                    doc.insertString(doc.getLength(), parts[3] + "\n", messageStyle);
                } else {
                    doc.insertString(doc.getLength(), message + "\n", messageStyle);
                }
            }


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // checks if a message contains a URL


    // Apply FlatLaf Dark Theme
    public void setThemeDarkMode() {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
            // Update the entire UI to reflect the new theme
            SwingUtilities.updateComponentTreeUI(this);
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
    }
}