package Drawboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class DrawBoardGUI extends JFrame {
    private DrawBoardClient client;
    private JPanel drawingPanel;
    private BufferedImage drawingImage;
    private Graphics2D graphics;
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;
    private Point lastPoint;
    private JButton eraserButton;
    private boolean isEraserActive = false;
    private JPanel colorIndicator;

    /**
     * create the GUI for the drawing board.
     *
     * @param client DrawBoardClient instance for server communication
     */
    public DrawBoardGUI(DrawBoardClient client) {
        this.client = client;
        initializeUI();
    }

    /**
     * initializes the drawing canvas with specified dimensions then creates a blank image and sets up the drawing graphics context.
     * @param width The width of the canvas
     * @param height The height of the canvas
     */
    public void initializeCanvas(int width, int height) {
        drawingImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics = drawingImage.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        clearDrawing();
        drawingPanel.repaint();
    }

    /**
     * starts the user interface components including Drawing panel with mouse listeners and Control panel with color picker, eraser, clear buttons, and brush size slider
     */
    private void initializeUI() {
        setTitle("Drawing Board");
        setSize(800, 600);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);
        setResizable(false);

        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (drawingImage != null) {
                    g.drawImage(drawingImage, 0, 0, null);
                }
            }
        };
        drawingPanel.setBackground(Color.WHITE);

        drawingPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastPoint = e.getPoint();
                draw(lastPoint.x, lastPoint.y, lastPoint.x, lastPoint.y);
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point currentPoint = e.getPoint();
                draw(lastPoint.x, lastPoint.y, currentPoint.x, currentPoint.y);
                lastPoint = currentPoint;
            }
        });

        JPanel controlPanel = new JPanel();
        JButton colorButton = createStandardButton("Choose Color");

        // Create color indicator panel
        colorIndicator = new JPanel();
        colorIndicator.setPreferredSize(new Dimension(20, 20));
        colorIndicator.setBackground(currentColor);
        colorIndicator.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        eraserButton = createStandardButton("Eraser");
        JButton clearButton = createStandardButton("Clear");
        JSlider sizeSlider = new JSlider(1, 20, brushSize);

        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose a color", currentColor);
            if (newColor != null) {
                currentColor = newColor;
                colorIndicator.setBackground(currentColor);
                isEraserActive = false;
                updateEraserButtonState();
            }
        });

        eraserButton.addActionListener(e -> {
            toggleEraser();
        });

        clearButton.addActionListener(e -> {
            clearDrawing();
            try {
                client.sendClearCommand();
            } catch (IOException ex) {
                showError("Error sending clear command: " + ex.getMessage());
            }
        });

        sizeSlider.addChangeListener(e -> {
            brushSize = sizeSlider.getValue();
        });

        // Add components to control panel with color indicator
        controlPanel.add(colorButton);
        controlPanel.add(colorIndicator);
        controlPanel.add(eraserButton);
        controlPanel.add(clearButton);
        controlPanel.add(new JLabel("Brush Size:"));
        controlPanel.add(sizeSlider);

        add(drawingPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        updateEraserButtonState();
    }

    /**
     * creates a good-looking JButton with consistent styling.
     *
     * @param text button text
     * @return configured JButton
     */
    private JButton createStandardButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setMargin(new Insets(2, 10, 2, 10));
        button.setForeground(Color.BLACK);
        button.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        return button;
    }

    /**
     * toggles eraser mode on/off.
     * when active, sets current color to white (eraser color).
     * when inactive, resets to black or previously selected color.
     */
    private void toggleEraser() {
        isEraserActive = !isEraserActive;
        currentColor = isEraserActive ? Color.WHITE : Color.BLACK;
        colorIndicator.setBackground(currentColor);
        updateEraserButtonState();
    }

    /**
     * updates the visuals of the eraser button to indicate active/inactive status such that when active, eraser is highlighted with a red border.
     */
    private void updateEraserButtonState() {
        if (isEraserActive) {
            eraserButton.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        } else {
            eraserButton.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }
    }

    /**
     * clears the drawing canvas by filling it with white and then repaints the panel to show the cleared state.
     */
    private void clearDrawing() {
        if (graphics != null) {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, drawingImage.getWidth(), drawingImage.getHeight());
            drawingPanel.repaint();
        }
    }

    /**
     * draws a line between two points on the canvas and sends the drawing data to server.
     *
     * @param x1 first x-coordinate
     * @param y1 first y-coordinate
     * @param x2 last x-coordinate
     * @param y2 last y-coordinate
     */
    public void draw(int x1, int y1, int x2, int y2) {
        if (graphics == null) return;

        graphics.setColor(currentColor);
        graphics.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x1, y1, x2, y2);
        drawingPanel.repaint();

        try {
            client.sendDrawingData(x1, y1, x2, y2, currentColor.getRGB(), brushSize);
        } catch (IOException e) {
            showError("Error sending drawing data: " + e.getMessage());
        }
    }

    /**
     * updates the drawing canvas with received drawing data from server.
     *
     * @param x1 first x-coordinate
     * @param y1 first y-coordinate
     * @param x2 last x-coordinate
     * @param y2 last y-coordinate
     * @param colorRGB color
     * @param size brush size
     */
    public void updateDrawing(int x1, int y1, int x2, int y2, int colorRGB, int size) {
        if (graphics == null) return;

        graphics.setColor(new Color(colorRGB));
        graphics.setStroke(new BasicStroke(size, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        graphics.drawLine(x1, y1, x2, y2);
        drawingPanel.repaint();
    }

    /**
     * clears the entire drawing canvas
     */
    public void clearAll() {
        clearDrawing();
    }

    /**
     * displays an error message dialog to the user.
     *
     * @param message The error message to display
     */
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}