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

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JButton submitButton;
    private Timer usbCheckTimer;
    private boolean isWindowShown = false;
    private boolean isClosedWindow = false;
    private User user;

    public AuthentifiactionWindow() {
        super("Authentication");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(300, 250);
        setMinimumSize(new Dimension(300, 250));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // Initialize components
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        submitButton = new JButton("Login") {{
            addActionListener(e -> handleSubmit());
        }};

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(submitButton);

        // Add components to frame
        add(inputPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Configure keyboard shortcuts
        configureKeyboardShortcuts();

        // Initially hide window
        setVisible(false);

        // Start USB detection
        startUsbDetection();
    }

    private void startUsbDetection() {
        usbCheckTimer = new Timer();
        usbCheckTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                File usbDrive = detectUsbDrive();
                boolean usbPresent = usbDrive != null;
                SwingUtilities.invokeLater(() -> {
                    if (usbPresent && !isWindowShown && !isClosedWindow) {
                        if (isUsbRecognized(usbDrive)) {
                            showAuthenticationWindow();
                        } else {
                            showUSBNotRecognizedDialog();
                        }
                    } else if (!usbPresent && isWindowShown) {
                        hideAuthenticationWindow();
                    }
                });
            }
        }, 0, 1000); // Check every 1000ms
    }

    private File detectUsbDrive() {
        File[] roots = File.listRoots();
        for (File root : roots) {
            try {
                FileStore store = Files.getFileStore(root.toPath());
                if ((Boolean) store.getAttribute("volume:isRemovable") && root.exists() && root.canRead()) {
                    return root;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return null;
    }

    private boolean isUsbRecognized(File usbDrive) {
        try {
            FileStore store = Files.getFileStore(usbDrive.toPath());
            Object serialObj = store.getAttribute("volume:vsn");
            String serialHex;
            if (serialObj instanceof Integer) {
                serialHex = Integer.toHexString((Integer) serialObj).toUpperCase();
            } else if (serialObj instanceof Long) {
                serialHex = Long.toHexString((Long) serialObj).toUpperCase();
            } else {
                serialHex = "UNKNOWN";
            }
            user = isUserExists(serialHex);
            return user != null;
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(null,
                        "Error reading USB: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            });
            return false;
        }
    }

    private void showAuthenticationWindow() {
        setVisible(true);
        isWindowShown = true;
        usernameField.requestFocus();
    }

    private void hideAuthenticationWindow() {
        setVisible(false);
        usernameField.setText("");
        passwordField.setText("");
        isWindowShown = false;
    }

    private void showUSBNotRecognizedDialog() {
        JOptionPane.showMessageDialog(null,
                "USB not recognized. Create an account or use another USB.",
                "USB Detection Failed",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showPlugInUsbDialog() {
        JOptionPane.showMessageDialog(null,
                "Please plug in a USB device first.",
                "USB Required",
                JOptionPane.WARNING_MESSAGE);
    }

    private void configureKeyboardShortcuts() {
        getRootPane().setDefaultButton(submitButton);
        usernameField.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "submit");
        usernameField.getActionMap().put("submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleSubmit();
            }
        });
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
        String username = usernameField.getText();
        char[] password = passwordField.getPassword();

        if (username.isEmpty() || password.length == 0) {
            JOptionPane.showMessageDialog(this,
                    "Username and password are required.",
                    "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (user != null && user.getUserName().equals(username) && user.getPassword().equals(new String(password))) {
            usernameField.setText("");
            passwordField.setText("");
            hideAuthenticationWindow();
            isClosedWindow = true;
            // Open MainWindow for all users
            SwingUtilities.invokeLater(() -> {
                MainWindow mainWindow = new MainWindow(user);
                mainWindow.setVisible(true);
            });
        } else {
            handleIncorrectCredentials();
        }
    }

    private void handleIncorrectCredentials() {
        JOptionPane.showMessageDialog(this,
                "Incorrect username or password. Please try again.",
                "Authentication Failed",
                JOptionPane.ERROR_MESSAGE);
        usernameField.setText("");
        passwordField.setText("");
        usernameField.requestFocus();
    }

    public String getPassword() {
        return new String(passwordField.getPassword());
    }

    public JButton getSubmitButton() {
        return submitButton;
    }

    public User isUserExists(String serialNumber) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection dbConnection = DriverManager.getConnection("jdbc:mysql://localhost:3306/networksimulatorusers", "root", "root");
            PreparedStatement stmt = dbConnection.prepareStatement("SELECT * FROM users WHERE usbSerialNum = ?");
            stmt.setString(1, serialNumber);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                User user = new User(
                        resultSet.getString("usbSerialNum"),
                        resultSet.getString("userName"),
                        resultSet.getString("password"),
                        resultSet.getString("role")
                );
                dbConnection.close();
                return user;
            }
            dbConnection.close();
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found");
        } catch (SQLException e) {
            System.out.println("SQL error: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            File usbDrive = detectUsbDrive();
            if (usbDrive == null) {
                showPlugInUsbDialog();
                return;
            }
            if (!isUsbRecognized(usbDrive)) {
                showUSBNotRecognizedDialog();
                return;
            }
        }
        super.setVisible(visible);
    }
}