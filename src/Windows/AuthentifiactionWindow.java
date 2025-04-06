package Windows;

import Data.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.sql.*;
import java.util.Timer;
import java.util.TimerTask;

public class AuthentifiactionWindow extends JFrame {

    private final JPasswordField passwordField;
    private final JButton submitButton;
    private final JLabel welcomeLabel;
    private static final String USB_DRIVE_PATH = "E:"; // Adjust this to your USB drive letter
    private Timer usbCheckTimer;
    private boolean isWindowShown = false;
    private boolean isClosedWindow = false;
    private User user;

    public AuthentifiactionWindow() {
        super("Authentication");

        welcomeLabel = new JLabel("Welcome User") {{
            setFont(new Font("Segoe UI", Font.BOLD, 16));
            setHorizontalAlignment(JLabel.CENTER);
            setVisible(false);
        }};

        // Initialize components with modern styling
        passwordField = new JPasswordField(20) {{
            setFont(new Font("Segoe UI", Font.PLAIN, 14));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200)),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            setVisible(false);
        }};

        submitButton = new JButton("Login") {{
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBackground(new Color(0, 123, 255));
            setForeground(Color.WHITE);
            setFocusPainted(false);
            setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addActionListener(e -> handleSubmit());
            setVisible(false);
        }};

        // Configure window
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 250);
        setMinimumSize(new Dimension(350, 200));
        setLocationRelativeTo(null);

        // Create and style main panel
        JPanel mainPanel = new JPanel() {{
            setLayout(new GridBagLayout());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        }};

        // Setup GridBagConstraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;


        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(welcomeLabel, gbc);

        gbc.gridy = 1;
        mainPanel.add(passwordField, gbc);

        // Add submit button
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        mainPanel.add(submitButton, gbc);

        // Add panel to frame
        add(mainPanel);

        // Add modern features
        configureKeyboardShortcuts();
        // Start USB detection
        startUsbDetection();
    }

    private void startUsbDetection() {
        usbCheckTimer = new Timer();
        usbCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File usbDrive = new File(USB_DRIVE_PATH);
                boolean usbPresent = usbDrive.exists() && usbDrive.canRead();
                SwingUtilities.invokeLater(() -> {
                    if (usbPresent && !isWindowShown && !isClosedWindow) {
                        if(isUsbRecognized(usbDrive)) {
                            showAuthenticationWindow(user.getUserName());
                        }
                    } else if (!usbPresent && isWindowShown) {
                        hideAuthenticationWindow();
                    }
                });
            }
        }, 0, 1000); // Start immediately, check every 1000ms (1 second)
    }

    private boolean isUsbRecognized(File usbDrive){
        try{
            FileStore store = Files.getFileStore(usbDrive.toPath()); // Get usb information
            // Safely handle the volume serial number as Number
            Object serialObj = store.getAttribute("volume:vsn"); // Get usb serial number as an object
            String serialHex;
            if (serialObj instanceof Integer) { // Check object type (it could be long/integer)
                serialHex = Integer.toHexString((Integer) serialObj).toUpperCase();
            } else if (serialObj instanceof Long) {
                serialHex = Long.toHexString((Long) serialObj).toUpperCase();
            } else {
                serialHex = "UNKNOWN";
            }
            user = isUserExists(serialHex);
            if(user != null) {
                return true;
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Error reading USB: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            });
        }
        return false;
    }

    private void showAuthenticationWindow(String userName) {
        welcomeLabel.setText("Welcome " + userName);
        welcomeLabel.setVisible(true);
        passwordField.setVisible(true);
        submitButton.setVisible(true);
        setVisible(true);
        isWindowShown = true;
        passwordField.requestFocus();
    }

    private void hideAuthenticationWindow() {
        setVisible(false);
        welcomeLabel.setVisible(false);
        passwordField.setVisible(false);
        submitButton.setVisible(false);
        isWindowShown = false;
    }

    private void showUSBNotRecognizedDialog() {
        JOptionPane.showMessageDialog(
                null,
                "USB not recognized. Create an account or use another usb",
                "USB Detection Failed",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void configureKeyboardShortcuts() {
        // Enter key submits form
        getRootPane().setDefaultButton(submitButton);
        passwordField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        passwordField.getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSubmit();
            }
        });
    }

    private void handleSubmit() {
        char[] password = passwordField.getPassword();

        // Add your authentication logic here

        if(user.getPassword().equals(new String(password))) {
            // Clear password field after submission
            passwordField.setText("");
            hideAuthenticationWindow();
            isClosedWindow = true;
        }else{
            handleIncorrectPassword();
        }
    }

    private void handleIncorrectPassword() {
        // Indicate that the password is incorrect
        JOptionPane.showMessageDialog(null, "Incorrect password. Please try again.",
                "Authentication Failed", JOptionPane.ERROR_MESSAGE);
        // Optionally, clear the password field or keep it for re-entry
        passwordField.setText("");
        passwordField.requestFocus(); // Put cursor back in the password field
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public JButton getSubmitButton() {
        return submitButton;
    }

    public User isUserExists(String serialNumber) {
        try{
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/networksimulatorusers", "root", "root");
            Statement dbStatement = dbConnection.createStatement();
            ResultSet resultSet = dbStatement.executeQuery("select * from users");
            while (resultSet.next()) {
                if(resultSet.getString("usbSerialNum").equals(serialNumber)) {
                    return new User(serialNumber, resultSet.getString("userName"), resultSet.getString("password"));
                }
            }
        } catch (ClassNotFoundException e){
            System.out.println("Driver not found");
        } catch (SQLException e){
            System.out.println("SQL error");
        }
        return null;
    }


}
