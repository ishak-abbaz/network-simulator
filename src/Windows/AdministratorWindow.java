package Windows;

import Data.User;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AdministratorWindow extends JFrame {
    private List<User> users = new ArrayList<>();
    private JPanel userPanel;
    private JComboBox<String> roleFilterCombo;

    public AdministratorWindow() {
        setTitle("Administrator Window");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        loadUsersFromDB();
        // Top panel for filtering and adding users
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roleFilterCombo = new JComboBox<>(new String[]{"All", "Admin", "User"});
        JButton addUserButton = new JButton("Add User");
        topPanel.add(new JLabel("Filter by Role:"));
        topPanel.add(roleFilterCombo);
        topPanel.add(addUserButton);

        // User panel to display users
        userPanel = new JPanel();
        userPanel.setLayout(new BoxLayout(userPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(userPanel);

        // Add components to frame
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Populate initial user list
        updateUserList();

        // Add user button action
        addUserButton.addActionListener(e -> showAddUserDialog());

        // Role filter action
        roleFilterCombo.addActionListener(e -> updateUserList());
        setVisible(true);
    }

    private void updateUserList() {
        userPanel.removeAll();
        String selectedRole = (String) roleFilterCombo.getSelectedItem();

        for (User user : users) {
            if (selectedRole.equals("All") || user.getRole().equalsIgnoreCase(selectedRole)) {
                JPanel userEntry = createUserEntry(user);
                userPanel.add(userEntry);
            }
        }
        userPanel.revalidate();
        userPanel.repaint();
    }

    private JPanel createUserEntry(User user) {
        JPanel entry = new JPanel(new BorderLayout(5, 5));
        entry.setBorder(BorderFactory.createEtchedBorder());
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel userLabel = new JLabel(user.getUserName() + " (" + user.getRole() + ")");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        entry.add(userLabel, BorderLayout.WEST);
        entry.add(buttonPanel, BorderLayout.EAST);

        // Edit action
        editButton.addActionListener(e -> showEditUserDialog(user));

        // Delete action
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Delete user " + user.getUserName() + "?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (user.getRole().equalsIgnoreCase("admin") && countAdmins() <= 1) {
                    JOptionPane.showMessageDialog(this, "Cannot delete the last admin user!", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    deleteUserFromDB(user.getUserName());
                    users.remove(user);
                    updateUserList();
                }
            }
        });

        // Make the entry clickable for details (optional)
        entry.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showUserDetails(user);
            }
        });
        return entry;
    }

    private void showAddUserDialog() {
        JDialog dialog = new JDialog(this, "Add User", true);
        dialog.setSize(300, 250);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        // USB detection components
        DefaultComboBoxModel<String> usbComboModel = new DefaultComboBoxModel<>();
        JComboBox<String> usbCombo = new JComboBox<>(usbComboModel);
        JLabel statusLabel = new JLabel("Detecting USB...", SwingConstants.CENTER);

        // Input fields
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"user", "admin"});

        // Input panel
        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel("USB Device:"));
        inputPanel.add(usbCombo);
        inputPanel.add(new JLabel(""));
        inputPanel.add(statusLabel);
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);
        inputPanel.add(new JLabel("Role:"));
        inputPanel.add(roleCombo);

        // Button panel
        JButton saveButton = new JButton("Save");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(saveButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // USB detection logic
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Map<String, String> usbDevices = new HashMap<>();

        Runnable usbDetector = () -> {
            while (!Thread.currentThread().isInterrupted()) {
                Map<String, String> newUsbDevices = detectUSBDrives();
                SwingUtilities.invokeLater(() -> {
                    usbDevices.clear();
                    usbDevices.putAll(newUsbDevices);
                    usbComboModel.removeAllElements();
                    if (!newUsbDevices.isEmpty()) {
                        newUsbDevices.forEach((name, serial) -> usbComboModel.addElement(name));
                        statusLabel.setText("USB detected");
                        saveButton.setEnabled(true);
                    } else {
                        usbComboModel.addElement("No USB Detected");
                        statusLabel.setText("Please insert a USB...");
                        saveButton.setEnabled(false);
                    }
                });
                try {
                    Thread.sleep(2000); // Poll every 2 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        };

        // Start USB detection
        Map<String, String> initialUsbDevices = detectUSBDrives();
        if (!initialUsbDevices.isEmpty()) {
            usbDevices.putAll(initialUsbDevices);
            initialUsbDevices.forEach((name, serial) -> usbComboModel.addElement(name));
            statusLabel.setText("USB detected");
            saveButton.setEnabled(true);
        } else {
            usbComboModel.addElement("No USB Detected");
            statusLabel.setText("Please insert a USB...");
            saveButton.setEnabled(false);
            executor.submit(usbDetector);
        }

        // Save button action
        saveButton.addActionListener(e -> {
            String selectedUsbName = (String) usbCombo.getSelectedItem();
            String usbSerial = usbDevices.getOrDefault(selectedUsbName, "");
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();

            if (!selectedUsbName.equals("No USB Detected") && !username.isEmpty() && !password.isEmpty()) {
                if (users.stream().noneMatch(u -> u.getUserName().equals(username))) {
                    User newUser = new User(usbSerial, username, password, role);
                    users.add(newUser);

                    addUserToDB(newUser);
                    updateUserList();
                    executor.shutdownNow();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Username already exists!");
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "All fields are required! Please plug in a USB device.");
            }
        });

        // Cleanup on dialog close
        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                executor.shutdownNow();
            }
        });

        dialog.setVisible(true);
    }

    private Map<String, String> detectUSBDrives() {
        Map<String, String> usbDevices = new HashMap<>();
        File[] roots = File.listRoots();

        for (File root : roots) {
            try {
                FileStore store = Files.getFileStore(root.toPath());
                // Only include removable drives
                if ((Boolean) store.getAttribute("volume:isRemovable")) {
                    Object serialObj = store.getAttribute("volume:vsn");
                    String serialHex;
                    if (serialObj instanceof Integer) {
                        serialHex = Integer.toHexString((Integer) serialObj).toUpperCase();
                    } else if (serialObj instanceof Long) {
                        serialHex = Long.toHexString((Long) serialObj).toUpperCase();
                    } else {
                        serialHex = "UNKNOWN";
                    }
                    // Check if the USB serial is already registered
                    if (isUserExists(serialHex) == null) {
                        String driveName = root.getAbsolutePath();
                        usbDevices.put(driveName, serialHex);
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }
        return usbDevices;
    }

    private User isUserExists(String serialHex) {
        return users.stream()
                .filter(u -> u.getUsbSerialNumber().equals(serialHex))
                .findFirst()
                .orElse(null);
    }

    private void showEditUserDialog(User user) {
        JDialog dialog = new JDialog(this, "Edit User", true);
        dialog.setSize(300, 250);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JTextField usbField = new JTextField(user.getUsbSerialNumber());
        JTextField usernameField = new JTextField(user.getUserName());
        JPasswordField passwordField = new JPasswordField(user.getPassword());
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"admin", "user"});
        roleCombo.setSelectedItem(user.getRole());

        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(new JLabel("USB Serial Number:"));
        inputPanel.add(usbField);
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);
        inputPanel.add(new JLabel("Role:"));
        inputPanel.add(roleCombo);

        JButton saveButton = new JButton("Save");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(saveButton);

        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            String usbSerial = usbField.getText();
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();

            if (!usbSerial.isEmpty() && !username.isEmpty() && !password.isEmpty()) {
                if (user.getRole().equalsIgnoreCase("admin") && countAdmins() <= 1 && !role.equalsIgnoreCase("admin")) {
                    JOptionPane.showMessageDialog(dialog, "Cannot change the role of the last admin!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (users.stream().noneMatch(u -> u != user && u.getUserName().equals(username))) {
                    user.setUsbSerialNumber(usbSerial);
                    user.setUserName(username);
                    user.setPassword(password);
                    user.setRole(role);
                    editUserFromDB(usbSerial, user);
                    updateUserList();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Username already exists!");
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "All fields are required!");
            }
        });

        dialog.setVisible(true);
    }

    private void showUserDetails(User user) {
        JDialog dialog = new JDialog(this, "User Details", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JPanel detailsPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailsPanel.add(new JLabel("USB Serial Number:"));
        detailsPanel.add(new JLabel(user.getUsbSerialNumber()));
        detailsPanel.add(new JLabel("Username:"));
        detailsPanel.add(new JLabel(user.getUserName()));
        detailsPanel.add(new JLabel("Role:"));
        detailsPanel.add(new JLabel(user.getRole()));

        dialog.add(detailsPanel, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection("jdbc:mysql://localhost:3306/networksimulatorusers", "root", "root");
        } catch (ClassNotFoundException e) {
            System.out.println("Driver not found brotha");
        } catch (SQLException e) {
            System.out.println("SQL Error Brotha");
        }
        return null;
    }

    private void loadUsersFromDB() {
        try {
            Connection dbConnection = getConnection();
            assert dbConnection != null;
            Statement dbStatement = dbConnection.createStatement();
            ResultSet resultSet = dbStatement.executeQuery("select * from users");
            while (resultSet.next()) {
                String serialNumber = resultSet.getString("usbSerialNum");
                String userName = resultSet.getString("userName");
                String password = resultSet.getString("password");
                String role = resultSet.getString("role");
                users.add(new User(serialNumber, userName, password, role));
            }
            dbConnection.close();
        } catch (SQLException e) {
            System.out.println("SQL Error Brotha");
        }
    }

    private void addUserToDB(User user) {
        try {
            Connection dbConnection = getConnection();
            PreparedStatement stmt = dbConnection.prepareStatement("insert into users values (?, ?, ?, ?)");
            stmt.setString(1, user.getUsbSerialNumber());
            stmt.setString(2, user.getUserName());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole());
            stmt.executeUpdate();
            dbConnection.close();
        } catch (SQLException e) {
            System.out.println("SQL Error Brotha");
        }
    }

    private int countAdmins() {
        return (int) users.stream().filter(u -> u.getRole().equalsIgnoreCase("admin")).count();
    }

    private void editUserFromDB(String usbSerialNum, User user) {
        try {
            Connection dbConnection = getConnection();
            PreparedStatement stmt = dbConnection.prepareStatement("UPDATE users SET usbSerialNum = ?, userName = ?, password = ?, role = ? WHERE usbSerialNum = ?");
            stmt.setString(1, user.getUsbSerialNumber());
            stmt.setString(2, user.getUserName());
            stmt.setString(3, user.getPassword());
            stmt.setString(4, user.getRole());
            stmt.setString(5, usbSerialNum);
            stmt.executeUpdate();
            dbConnection.close();
        } catch (SQLException e) {
            System.out.println("SQL Error Brotha");
        }
    }

    private void deleteUserFromDB(String userName) {
        try {
            Connection dbConnection = getConnection();
            User user = users.stream().filter(u -> u.getUserName().equals(userName)).findFirst().orElse(null);
            if (user != null && user.getRole().equalsIgnoreCase("admin") && countAdmins() <= 1) {
                JOptionPane.showMessageDialog(this, "Cannot delete the last admin user!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            PreparedStatement stmt = dbConnection.prepareStatement("DELETE FROM users WHERE userName = ?");
            stmt.setString(1, userName);
            stmt.executeUpdate();
            dbConnection.close();
        } catch (SQLException e) {
            System.out.println("SQL Error Brotha");
        }
    }
}