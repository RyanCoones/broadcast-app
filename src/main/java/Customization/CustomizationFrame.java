package Customization;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Objects;

/**
* The customization frame allows users to customize elements like button colors,
* font, text color, etc. The customizations are then applied through UIManager and saved to the properties file
*/
public class CustomizationFrame extends JFrame {
    // Instance variables for customizing properties
    private Color buttonColor;
    private Color buttonHoverColor;
    private Color buttonPressedColor;
    private Color buttonTextColor;
    private Color buttonBorderColor;
    private JComboBox<Integer> buttonBorderWidthComboBox;
    private JComboBox<Integer> buttonArcComboBox;
    private Color backgroundColor;
    private JComboBox<String> textFontComboBox;
    private JComboBox<Integer> textSizeComboBox;
    private JComboBox<String> textStyleComboBox;
    private Color textColor;

    /**
     * CustomizationFrame constructor with a parent JFrame as the parameter.
     * Sets up the layout, customizations, and UI components for the user to adjust.
     *
     * @param parent used to start in the middle of the main frame
     */
    public CustomizationFrame(JFrame parent) {
        // start off loading darktheme
        FlatDarkLaf.setup();
        // create the popout frame
        setTitle("Customize Application");
        setSize(800, 600);
        setResizable(false);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // using gridbaglayout
        setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(10,10,10,5);

        // --------- OPTIONS ------------
        // button color customization
        addColorCustomization("Button Color:", "Edit Button Color", "buttonColor", buttonColor, new Color(85, 88, 90), constraints, 0);
        // button hover color customization
        addColorCustomization("Button Hover Color:", "Edit Button Hover Color", "buttonHoverColor", buttonHoverColor, new Color(85, 88, 90), constraints, 1);
        // button pressed color customization
        addColorCustomization("Button Pressed Color:", "Edit Button Pressed Color", "buttonPressedColor", buttonPressedColor, new Color(93, 95, 98), constraints, 2);
        // Button Text Color Customization
        addColorCustomization("Button Text Color:", "Edit Text Color", "buttonTextColor", buttonTextColor, new Color(187, 187, 187), constraints, 3);
        // Button Border Color Customization
        addColorCustomization("Button Border Color:", "Edit Border Color", "buttonBorderColor", buttonBorderColor, new Color(96, 98, 99), constraints, 4);

        // Button border width
        JLabel buttonBorderWidthLabel = new JLabel("Button Border Width: ");
        Integer[] buttonBorderWidthChoices = {0,2,4,8,10};
        // button to see the change
        JButton buttonBorderButton = new JButton("Test");
        buttonBorderWidthComboBox = new JComboBox<Integer>(buttonBorderWidthChoices);
        buttonBorderButton.addActionListener(e -> {
            // Update the button border width in UIManager
            int selectedWidth = (int) buttonBorderWidthComboBox.getSelectedItem();
            UIManager.put("Button.borderWidth", selectedWidth);
            // try refresh
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                throw new RuntimeException(ex);
            }
            SwingUtilities.updateComponentTreeUI(this);
        });
        constraints.gridx = 0;
        constraints.gridy = 5;
        add(buttonBorderWidthLabel, constraints);
        constraints.gridx = 1;
        add(buttonBorderButton, constraints);
        constraints.gridx = 2;
        add(buttonBorderWidthComboBox, constraints);

        // Button roundness
        JLabel buttonArcLabel = new JLabel("Button Arc: ");
        Integer[] buttonArcSizeChoices = {0,2,4,6,8,10,12,14,16,18,20};
        // button to see the change
        JButton buttonArcButton = new JButton("Test");
        buttonArcComboBox = new JComboBox<Integer>(buttonArcSizeChoices);
        buttonArcButton.addActionListener(e -> {
            int selectedArc = (int) buttonArcComboBox.getSelectedItem();
            UIManager.put("Button.arc", selectedArc);
            // try refresh
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (UnsupportedLookAndFeelException ex) {
                throw new RuntimeException(ex);
            }
            SwingUtilities.updateComponentTreeUI(this);
        });
        // set it in the layout
        constraints.gridx = 0;
        constraints.gridy = 6;
        add(buttonArcLabel, constraints);
        constraints.gridx = 1;
        add(buttonArcButton, constraints);
        constraints.gridx = 2;
        add(buttonArcComboBox, constraints);

        // panel color customization
        addColorCustomization("Background Color:", "Edit Background Color", "backgroundColor", backgroundColor, new Color(60, 63, 65), constraints, 7);
        // text color customization
        addColorCustomization("Text Color:", "Edit Text Color", "textColor", textColor, new Color(187, 187, 187), constraints, 8);

        // Font Customization
        JLabel textFontLabel = new JLabel("text Font: ");
        String[] textFontChoices = {"Arial", "Verdana", "Tahoma", "Helvetica" ,"Times New Roman", "Georgia", "Consolas"};
        textFontComboBox = new JComboBox<String>(textFontChoices);
        Integer[] textFontSizeChoices = {12,14,16,18,20,22,24,26,28};
        textSizeComboBox = new JComboBox<Integer>(textFontSizeChoices);
        String[] textFontStyleChoices = {"Normal", "Bold", "Italic"};
        textStyleComboBox = new JComboBox<String>(textFontStyleChoices);
        JLabel testLabel = new JLabel("Test");
        // create a JPanel to hold the 3 comboBoxes so it doesn't look ugly
        JPanel fontPanel = new JPanel();
        fontPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        //add the elements to the panel
        fontPanel.add(textSizeComboBox);
        fontPanel.add(textFontComboBox);
        fontPanel.add(textStyleComboBox);

        //set it in the layout
        constraints.gridx = 0;
        constraints.gridy = 9;
        add(textFontLabel, constraints);
        constraints.gridx = 1;
        add(testLabel, constraints);
        constraints.gridx = 2;
        add(fontPanel, constraints);

        // Save Customization Button
        JButton saveConfigButton = new JButton("Save Customizations");
        saveConfigButton.addActionListener(e -> saveToProperties(parent));
        constraints.gridx = 1;
        constraints.gridy = 10;
        add(saveConfigButton, constraints);

        // Reset Customization Button
        JButton resetConfigButton = new JButton("Reset Customizations");
        resetConfigButton.addActionListener(e -> resetProperties(parent));
        constraints.gridx = 2;
        constraints.gridy = 10;
        add(resetConfigButton, constraints);

        // make the window visable
        setVisible(true);
    }

    /**
     * Adds a color customization option to the frame.
     *
     * @param labelText The label for the color option.
     * @param buttonText The button text to trigger the color picker.
     * @param colorKey The key to identify which color is being customized.
     * @param initialColor The initial color for the button.
     * @param defaultColor The default color to use if no color is selected.
     * @param constraints Layout constraints for the component.
     * @param gridY Grid position for this customization.
     */
    private void addColorCustomization(String labelText, String buttonText, String colorKey, Color initialColor, Color defaultColor, GridBagConstraints constraints, int gridY) {
        // Create label, button, and color preview panel for customization
        JLabel label = new JLabel(labelText);
        JButton button = new JButton(buttonText);
        JPanel colorBox = createPreviewColorBox(initialColor, defaultColor);

        // ActionListener for the button to open color picker
        button.addActionListener(e -> {
            Color selected = JColorChooser.showDialog(this, "Choose a color for " + labelText.toLowerCase(), initialColor);
            if (selected != null) {
                // Update the color based on user selection
                switch (colorKey) {
                    case "buttonColor" -> buttonColor = selected;
                    case "buttonHoverColor" -> buttonHoverColor = selected;
                    case "buttonPressedColor" -> buttonPressedColor = selected;
                    case "buttonTextColor" -> buttonTextColor = selected;
                    case "buttonBorderColor" -> buttonBorderColor = selected;
                    case "backgroundColor" -> backgroundColor = selected;
                    case "textColor" -> textColor = selected;
                }
                colorBox.setBackground(selected);
            }
        });

        // Add components to the frame using GridBagLayout
        constraints.gridy = gridY;
        constraints.gridx = 0;
        add(label, constraints);
        constraints.gridx = 1;
        add(colorBox, constraints);
        constraints.gridx = 2;
        add(button, constraints);
    }


    /**
     * saves all the customizations to the properties file.
     *
     * @param parent used to refresh the parent UI to apply changes.
     */
    public void saveToProperties(JFrame parent) {
        System.out.println("Starting Up Saving Customizations...");

        // open properties file
        File userConfig = new File("src/main/resources/themes/FlatDarkLaf.properties");

        // string to hold all the additions to the properties file
        StringBuilder customizations = new StringBuilder();

        // Adding customizations to the string
        appendIfNotNull(customizations, "Button.background", buttonColor);
        appendIfNotNull(customizations, "Button.hoverBackground", buttonHoverColor);
        appendIfNotNull(customizations, "Button.pressedBackground", buttonPressedColor);
        appendIfNotNull(customizations, "Button.foreground", buttonTextColor);
        appendIfNotNull(customizations, "Button.borderColor", buttonBorderColor);
        appendIfNotNull(customizations, "Button.borderWidth", buttonBorderWidthComboBox.getSelectedItem());
        appendIfNotNull(customizations, "Button.arc", buttonArcComboBox.getSelectedItem());
        appendIfNotNull(customizations, "@background", backgroundColor);

        // set textFont to the format: size "font" style. example: 20 "Consolas" Bold
        if (textSizeComboBox != null && textStyleComboBox != null && textFontComboBox != null) {
            customizations.append("defaultFont=").append(Objects.requireNonNull(textSizeComboBox.getSelectedItem()).toString()).append(" ").append(textFontComboBox.getSelectedItem()).append(" ").append(textStyleComboBox.getSelectedItem()).append("\n");
        }
        if (textColor != null){
            customizations.append("Text.foreground=").append(convertColorToHex(textColor)).append("\n");
        }

        // try writing to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userConfig))) {
            writer.write(customizations.toString());
            System.out.println("Customizations saved!");
        } catch (IOException e) {
            System.out.println("Error saving Customizations...");
            e.printStackTrace();
        }
        // Apply customizations to UIManager
        applyCustomizationsToUIManager();
        // Refresh the UI to apply changes
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            throw new RuntimeException(ex);
        }
        SwingUtilities.updateComponentTreeUI(parent);
        // show a message that the file has been saved
        JOptionPane.showMessageDialog(this, "Customizations saved!");

        // Close the customization window
        dispose();
    }

    private void applyCustomizationsToUIManager() {
        // Button background color
        if (buttonColor != null) {
            UIManager.put("Button.background", buttonColor);
        }
        // Button hover background color
        if (buttonHoverColor != null) {
            UIManager.put("Button.hoverBackground", buttonHoverColor);
        }
        // Button pressed background color
        if (buttonPressedColor != null) {
            UIManager.put("Button.pressedBackground", buttonPressedColor);
        }
        // Button text color
        if (buttonTextColor != null) {
            UIManager.put("Button.foreground", buttonTextColor);
        }
        // Button border color
        if (buttonBorderColor != null) {
            UIManager.put("Button.borderColor", buttonBorderColor);
        }
        // Button border width
        Integer borderWidth = (Integer) buttonBorderWidthComboBox.getSelectedItem();
        if (borderWidth != null) {
            UIManager.put("Button.borderWidth", borderWidth);
        }
        // Button arc (roundness)
        Integer buttonArc = (Integer) buttonArcComboBox.getSelectedItem();
        if (buttonArc != null) {
            UIManager.put("Button.arc", buttonArc);
        }
        // Background color
        if (backgroundColor != null) {
            UIManager.put("Panel.background", backgroundColor);
        }
        // Text color
        if (textColor != null) {
            UIManager.put("Text.foreground", textColor);
        }
        // Font customization
        if (textSizeComboBox != null && textStyleComboBox != null && textFontComboBox != null) {
            // Get the selected font size, style, and font family
            int fontSize = (Integer) textSizeComboBox.getSelectedItem();
            String fontStyle = (String) textStyleComboBox.getSelectedItem();
            String fontFamily = (String) textFontComboBox.getSelectedItem();

            // Convert font style to appropriate Font constant
            int style = Font.PLAIN;
            if ("Bold".equals(fontStyle)) {
                style = Font.BOLD;
            } else if ("Italic".equals(fontStyle)) {
                style = Font.ITALIC;
            }
            // Create a Font object with the selected values
            Font customFont = new Font(fontFamily, style, fontSize);
            // Apply the custom font to the default font
            UIManager.put("defaultfont", customFont);
        }
    }

    // helper method for the save function
    private void appendIfNotNull(StringBuilder sb, String key, Object value) {
        if (value != null) {
            sb.append(key).append("=").append(value instanceof Color ? convertColorToHex((Color) value) : value).append("\n");
        }
    }

    /**
     * resets all the customizations to the properties file.
     */
    public void resetProperties(JFrame parent){
        // clear the properties
        File userConfig = new File("src/main/resources/themes/FlatDarkLaf.properties");

        // write an empty string to properties file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(userConfig))) {
            writer.write("");
            System.out.println("Properties file has been cleared!");
            // make sure to reset the font
            writer.write("defaultFont=16 Veranda Bold\n");
        } catch (IOException e) {
            System.out.println("Error clearing properties file...");
            e.printStackTrace();
        }
        // Refresh the UI to apply changes
        try {
            UIManager.setLookAndFeel(new FlatDarkLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            throw new RuntimeException(ex);
        }
        SwingUtilities.updateComponentTreeUI(parent);
        // show a message that the file has been cleared
        JOptionPane.showMessageDialog(this, "Properties file has been cleared.");
        dispose();
    }

    // Convert color to hex format for saving
    public String convertColorToHex(Color color) {
        String hex = String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
        return hex;
    }

    // Create a small panel to show the selected color
    public JPanel createPreviewColorBox(Color variable, Color defaultColor) {
        JPanel box = new JPanel();
        // a nice small size for the preview color
        box.setMinimumSize(new Dimension(20, 20));
        // add a border around it
        box.setBackground(variable != null ? variable : defaultColor);
        box.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        return box;
    }
}