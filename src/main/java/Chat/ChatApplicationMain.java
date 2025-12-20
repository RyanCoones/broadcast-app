package Chat;
import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;

public class ChatApplicationMain {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        // DONT FORGET TO RUN THE SERVER(S) FIRST
        ChatApplicationGUI gui = new ChatApplicationGUI();  // Create an instance of the GUI
        if (gui.getUsername() == null) {System.exit(0);} // If the user didnt enter a username, pressed x or cancelled on the username prompt, close the program
        gui.setVisible(true);   // Make the GUI visible
    }
}
