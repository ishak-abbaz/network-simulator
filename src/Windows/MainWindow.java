package Windows;

import Data.Computer;
import Data.Device;
import Data.Switch;
import Data.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class MainWindow extends JFrame implements ActionListener {

    private JButton addComputerButton, addSwitchButton;
    private JPanel devicePanel;
    private ArrayList<Device> devices;
    private User user;
    private AdministratorWindow adminWindow;
    private static final String DATA_DIR = "users_data/";

    public void actionPerformed(ActionEvent ae) {
        if (addComputerButton.equals(ae.getSource())) {
            showAddComputerDialog();
        } else if (addSwitchButton.equals(ae.getSource())) {
            showAddSwitchDialog();
        }
    }

    public MainWindow(User user) {
        this.user = user;
        setTitle("Network Simulator - " + user.getUserName());
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        devices = new ArrayList<>();

        createDataDirectory();

        devicePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawConnections(g);
            }
        };
        devicePanel.setLayout(null);
        devicePanel.setPreferredSize(new Dimension(800, 600)); // Ensure initial size
        JScrollPane scrollPane = new JScrollPane(devicePanel);
        add(scrollPane, BorderLayout.CENTER);

        addComputerButton = new JButton("Add Computer");
        addSwitchButton = new JButton("Add Switch");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addComputerButton);
        buttonPanel.add(addSwitchButton);
        add(buttonPanel, BorderLayout.SOUTH);

        addComputerButton.addActionListener(this);
        addSwitchButton.addActionListener(this);

        JMenuBar menuBar = new JMenuBar();
        JMenu userMenu = new JMenu("User");
        JMenuItem userInfoItem = new JMenuItem("View User Info");
        userInfoItem.addActionListener(e -> showUserInfoDialog());
        userMenu.add(userInfoItem);
        if (user.getRole().equalsIgnoreCase("admin")) {
            JMenuItem adminItem = new JMenuItem("Open Admin Panel");
            adminItem.addActionListener(e -> openAdminPanel());
            userMenu.add(adminItem);
        }
        menuBar.add(userMenu);
        setJMenuBar(menuBar);

        // Load configuration after UI setup but before setVisible
        loadUserConfiguration();

        setVisible(true);

        // Force layout update after window is visible
        SwingUtilities.invokeLater(() -> {
            devicePanel.revalidate();
            devicePanel.repaint();
            scrollPane.revalidate();
            scrollPane.repaint();
        });
    }

    private void createDataDirectory() {
        File dir = new File(DATA_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private String getUserFilePath(String username) {
        return DATA_DIR + username + ".txt";
    }

    private void saveUserConfiguration() {
        String filePath = getUserFilePath(user.getUserName());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Device device : devices) {
                int x = device.label.getX();
                int y = device.label.getY();
                if (device instanceof Computer) {
                    Computer computer = (Computer) device;
                    String linkedName = computer.getLinkedDevice() != null ? computer.getLinkedDevice().getName() : "None";
                    writer.write(String.format("Computer,%s,%s,%s,%d,%d\n", computer.getName(), computer.getIp(), linkedName, x, y));
                } else if (device instanceof Switch) {
                    Switch switchDevice = (Switch) device;
                    String linkedNames = switchDevice.getLinkedDevices().stream()
                            .map(Device::getName)
                            .collect(Collectors.joining(";"));
                    writer.write(String.format("Switch,%s,%s,%s,%d,%d\n", switchDevice.getName(), switchDevice.getIp(), linkedNames, x, y));
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving configuration: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUserConfiguration() {
        String filePath = getUserFilePath(user.getUserName());
        File file = new File(filePath);

        if (!file.exists()) {
            try {
                file.createNewFile();
                saveUserConfiguration();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error creating user file: " + e.getMessage(), "File Creation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // First pass: Create all devices
        List<Device> tempDevices = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 6);
                if (parts.length < 6) {
                    System.err.println("Skipping invalid line: " + line);
                    continue;
                }

                String type = parts[0];
                String name = parts[1];
                String ip = parts[2];
                int x, y;
                try {
                    x = Integer.parseInt(parts[4]);
                    y = Integer.parseInt(parts[5]);
                    // Delay clamping until after window is visible
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid coordinates in file for device: " + name, "Load Error", JOptionPane.WARNING_MESSAGE);
                    x = 50;
                    y = 50;
                    placeDeviceWithoutOverlap(createDeviceLabel(name, ip, null, type.equals("Computer") ? "D:\\eclipse\\computerIcon.png" : "D:\\eclipse\\switchIcon.png", type.equals("Computer")));
                    continue;
                }

                JLabel label = createDeviceLabel(name, ip, null, type.equals("Computer") ? "D:\\eclipse\\computerIcon.png" : "D:\\eclipse\\switchIcon.png", type.equals("Computer"));
                label.setBounds(x, y, 80, 80);

                Device device;
                if (type.equals("Computer")) {
                    device = new Computer(name, ip, label, null);
                } else {
                    device = new Switch(name, ip, label);
                }
                tempDevices.add(device);
                devicePanel.add(label);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading configuration: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        devices = new ArrayList<>(tempDevices);

        // Second pass: Collect linking information
        Map<String, String> computerLinks = new HashMap<>();
        Map<String, List<String>> switchLinks = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 6);
                if (parts.length < 6) continue;

                String type = parts[0];
                String name = parts[1];
                String linkedInfo = parts[3];

                if (type.equals("Computer") && !linkedInfo.equals("None")) {
                    computerLinks.put(name, linkedInfo);
                } else if (type.equals("Switch") && !linkedInfo.isEmpty()) {
                    switchLinks.put(name, new ArrayList<>(Arrays.asList(linkedInfo.split(";"))));
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading links: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Apply linking using a copy of devices
        List<Device> devicesCopy = new ArrayList<>(devices);
        for (Map.Entry<String, String> entry : computerLinks.entrySet()) {
            String name = entry.getKey();
            String linkedName = entry.getValue();
            Device device = findDeviceByName(name, devicesCopy);
            Device linkedDevice = findDeviceByName(linkedName, devicesCopy);
            if (device instanceof Computer && linkedDevice != null) {
                ((Computer) device).setLinkedDevice(linkedDevice);
            }
        }

        for (Map.Entry<String, List<String>> entry : switchLinks.entrySet()) {
            String name = entry.getKey();
            List<String> linkedNames = entry.getValue();
            Device device = findDeviceByName(name, devicesCopy);
            if (device instanceof Switch) {
                List<Device> linkedDevices = new ArrayList<>();
                for (String linkedName : linkedNames) {
                    Device linkedDevice = findDeviceByName(linkedName, devicesCopy);
                    if (linkedDevice != null) {
                        linkedDevices.add(linkedDevice);
                    }
                }
                ((Switch) device).setLinkedDevices(linkedDevices);
            }
        }

        // Clamp positions after window is visible
        SwingUtilities.invokeLater(() -> {
            for (Device device : devices) {
                int x = device.label.getX();
                int y = device.label.getY();
                x = Math.max(0, Math.min(x, devicePanel.getWidth() - 80));
                y = Math.max(0, Math.min(y, devicePanel.getHeight() - 80));
                device.label.setBounds(x, y, 80, 80);
            }
            devicePanel.revalidate();
            devicePanel.repaint();
        });
    }

    private Device findDeviceByName(String name, List<Device> deviceList) {
        return deviceList.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public void onUserNameChanged(String oldUserName, String newUserName) {
        String oldFilePath = getUserFilePath(oldUserName);
        String newFilePath = getUserFilePath(newUserName);
        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);

        if (oldFile.exists()) {
            try {
                Files.move(oldFile.toPath(), newFile.toPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error renaming user file: " + e.getMessage(), "Rename Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            try {
                newFile.createNewFile();
                saveUserConfiguration();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error creating new user file: " + e.getMessage(), "File Creation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        user.setUserName(newUserName);
        setTitle("Network Simulator - " + newUserName);
    }

    private void showUserInfoDialog() {
        JDialog dialog = new JDialog(this, "User Information", true);
        dialog.setSize(300, 180);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(new JLabel("Username:"));
        contentPanel.add(new JLabel(user.getUserName()));
        contentPanel.add(new JLabel("Role:"));
        contentPanel.add(new JLabel(user.getRole()));
        contentPanel.add(new JLabel("USB Serial:"));
        contentPanel.add(new JLabel(user.getUsbSerialNumber()));

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.dispose());

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(createButtonPanel(closeButton), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void openAdminPanel() {
        if (adminWindow == null || !adminWindow.isVisible()) {
            adminWindow = new AdministratorWindow();
            adminWindow.setVisible(true);
        } else {
            adminWindow.toFront();
        }
    }

    private void drawConnections(Graphics g) {
        g.setColor(Color.BLACK);
        for (Device device : devices) {
            if (device instanceof Computer) {
                Computer computer = (Computer) device;
                Device linkedDevice = computer.getLinkedDevice();
                if (linkedDevice != null) {
                    drawLineBetweenLabels(g, computer.label, linkedDevice.label);
                }
            } else if (device instanceof Switch) {
                Switch switchDevice = (Switch) device;
                for (Device linkedDevice : switchDevice.getLinkedDevices()) {
                    if (linkedDevice != null) {
                        drawLineBetweenLabels(g, switchDevice.label, linkedDevice.label);
                    }
                }
            }
        }
    }

    private Device findDeviceByName(String name) {
        return findDeviceByName(name, devices);
    }

    private void drawLineBetweenLabels(Graphics g, JLabel label1, JLabel label2) {
        Point p1 = getLabelCenter(label1);
        Point p2 = getLabelCenter(label2);
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
    }

    private Point getLabelCenter(JLabel label) {
        int x = label.getX() + label.getWidth() / 2;
        int y = label.getY() + label.getHeight() / 2;
        return new Point(x, y);
    }

    private void showAddComputerDialog() {
        JDialog dialog = createDeviceDialog("Add Computer");
        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();
        JComboBox<String> linkedDeviceCombo = new JComboBox<>();
        linkedDeviceCombo.addItem("None");
        for (Device d : devices) {
            if (!(d instanceof Computer) || ((Computer) d).getLinkedDevice() == null) {
                linkedDeviceCombo.addItem(d.getName());
            }
        }

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("IP Address:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Linked Device:"));
        inputPanel.add(linkedDeviceCombo);

        JButton okButton = new JButton("OK");
        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(createButtonPanel(okButton), BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = validateAndFormatIP(ipField.getText(), dialog);
            String linkedDeviceName = (String) linkedDeviceCombo.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty!");
                return;
            }
            if (ip == null) {
                return;
            }

            if (devices.stream().anyMatch(d -> d.getName().equals(name))) {
                JOptionPane.showMessageDialog(dialog, "Device name already exists!");
                return;
            }

            Device linkedDevice = linkedDeviceName.equals("None") ? null : findDeviceByName(linkedDeviceName);
            JLabel label = createDeviceLabel(name, ip, linkedDevice, "D:\\eclipse\\computerIcon.png", true);
            placeDeviceWithoutOverlap(label);

            Computer computer = new Computer(name, ip, label, linkedDevice);
            if (linkedDevice != null) {
                computer.setLinkedDevice(linkedDevice);
            }
            devices.add(computer);
            devicePanel.add(label);
            devicePanel.revalidate();
            devicePanel.repaint();
            saveUserConfiguration();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private void showAddSwitchDialog() {
        List<Device> linkedDevices = new ArrayList<>();
        JDialog dialog = createDeviceDialog("Add Switch");
        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();
        JLabel linkedLabel = new JLabel("0");
        JButton linkButton = new JButton("Link") {{
            setPreferredSize(new Dimension(80, 30));
        }};

        linkButton.addActionListener(e -> {
            JDialog linkDialog = new JDialog(dialog, "Select Devices to Link", true);
            linkDialog.setSize(200, 250);
            linkDialog.setLayout(new BorderLayout());
            linkDialog.setLocationRelativeTo(dialog);

            JPanel checkBoxPanel = new JPanel();
            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
            List<JCheckBox> checkBoxes = new ArrayList<>();
            for (Device d : devices) {
                if (!(d instanceof Computer) || ((Computer) d).getLinkedDevice() == null) {
                    JCheckBox checkBox = new JCheckBox(d.getName());
                    checkBox.setSelected(linkedDevices.contains(d));
                    checkBoxes.add(checkBox);
                    checkBoxPanel.add(checkBox);
                }
            }

            JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
            linkDialog.add(scrollPane, BorderLayout.CENTER);

            JButton confirmButton = new JButton("Confirm");
            confirmButton.addActionListener(e1 -> {
                linkedDevices.clear();
                for (JCheckBox checkBox : checkBoxes) {
                    if (checkBox.isSelected()) {
                        Device d = findDeviceByName(checkBox.getText());
                        if (d != null) {
                            linkedDevices.add(d);
                        }
                    }
                }
                linkedLabel.setText(String.valueOf(linkedDevices.size()));
                linkDialog.dispose();
            });

            linkDialog.add(createButtonPanel(confirmButton, new JButton("Cancel") {{
                addActionListener(e1 -> linkDialog.dispose());
            }}), BorderLayout.SOUTH);
            linkDialog.setVisible(true);
        });

        JPanel linkedDevicesPanel = new JPanel();
        linkedDevicesPanel.add(linkedLabel);
        linkedDevicesPanel.add(linkButton);

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("IP Address:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Linked Devices:"));
        inputPanel.add(linkedDevicesPanel);

        JButton okButton = new JButton("OK");
        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(createButtonPanel(okButton), BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = validateAndFormatIP(ipField.getText(), dialog);

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty!");
                return;
            }
            if (ip == null) {
                return;
            }

            if (devices.stream().anyMatch(d -> d.getName().equals(name))) {
                JOptionPane.showMessageDialog(dialog, "Device name already exists!");
                return;
            }

            JLabel label = createDeviceLabel(name, ip, linkedDevices, "D:\\eclipse\\switchIcon.png", false);
            placeDeviceWithoutOverlap(label);

            Switch newSwitch = new Switch(name, ip, label);
            newSwitch.setLinkedDevices(new ArrayList<>(linkedDevices));
            devices.add(newSwitch);
            devicePanel.add(label);
            devicePanel.revalidate();
            devicePanel.repaint();
            saveUserConfiguration();
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private JDialog createDeviceDialog(String title) {
        JDialog dialog = new JDialog(this, title, true);
        dialog.setSize(300, 200);
        dialog.setLayout(new BorderLayout(4, 4));
        dialog.setLocationRelativeTo(this);
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        dialog.setContentPane(contentPanel);
        return dialog;
    }

    private JPanel createButtonPanel(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        for (JButton button : buttons) {
            panel.add(button);
        }
        return panel;
    }

    private JLabel createDeviceLabel(String name, String ip, Object linked, String iconPath, boolean isComputer) {
        JLabel label = new JLabel(name);
        String linkedText = isComputer ? (linked != null ? ((Device) linked).getName() : "None") :
                linked == null || ((List<Device>) linked).isEmpty() ? "None" :
                        ((List<Device>) linked).stream().map(Device::getName).collect(Collectors.joining(", "));
        label.setToolTipText("<html>Name: " + name + "<br>IP: " + ip + "<br>Linked: " + linkedText + "</html>");

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Device device = devices.stream()
                        .filter(d -> d.label == label)
                        .findFirst()
                        .orElse(null);
                if (device instanceof Computer) {
                    Computer computer = (Computer) device;
                    label.setToolTipText("<html>Name: " + computer.getName() +
                            "<br>IP: " + computer.getIp() +
                            "<br>Linked: " + computer.getLinkedDeviceName() + "</html>");
                } else if (device instanceof Switch) {
                    Switch switchDevice = (Switch) device;
                    label.setToolTipText("<html>Name: " + switchDevice.getName() +
                            "<br>IP: " + switchDevice.getIp() +
                            "<br>Linked: " + switchDevice.getLinkedDevicesNames() + "</html>");
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                String currentName = label.getText();
                if (isComputer) {
                    onComputerMouseClicked(currentName);
                } else {
                    onSwitchMouseClicked(currentName);
                }
            }
        });

        ImageIcon icon = null;
        try {
            icon = new ImageIcon(new ImageIcon(iconPath).getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading icon: " + iconPath, "Icon Error", JOptionPane.WARNING_MESSAGE);
        }
        if (icon != null) {
            label.setIcon(icon);
        }
        label.setHorizontalTextPosition(JLabel.CENTER);
        label.setVerticalTextPosition(JLabel.BOTTOM);
        label.setSize(80, 80);

        Point[] offset = {null};
        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = new Point(e.getX(), e.getY());
            }
        });

        label.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = label.getX() + e.getX() - offset[0].x;
                int y = label.getY() + e.getY() - offset[0].y;

                int minX = 0;
                int minY = 0;
                int maxX = devicePanel.getWidth() - label.getWidth();
                int maxY = devicePanel.getHeight() - label.getHeight();

                x = Math.max(minX, Math.min(x, maxX));
                y = Math.max(minY, Math.min(y, maxY));

                label.setLocation(x, y);
                devicePanel.repaint();
                saveUserConfiguration();
            }
        });

        return label;
    }

    private void placeDeviceWithoutOverlap(JLabel label) {
        if (devicePanel == null) {
            label.setBounds(50, 50, 80, 80);
            return;
        }

        int padding = 100;
        boolean placed = false;

        for (int x = 50; x < devicePanel.getWidth() - 80; x += padding) {
            for (int y = 50; y < devicePanel.getHeight() - 80; y += padding) {
                Rectangle newBounds = new Rectangle(x, y, 80, 80);
                boolean overlap = false;

                for (Device d : devices) {
                    if (d.label.getBounds().intersects(newBounds)) {
                        overlap = true;
                        break;
                    }
                }

                if (!overlap) {
                    label.setBounds(newBounds);
                    placed = true;
                    break;
                }
            }
            if (placed) break;
        }

        if (!placed) {
            label.setBounds(50, 50, 80, 80);
            for (Device d : devices) {
                if (d.label.getBounds().intersects(label.getBounds())) {
                    label.setLocation(label.getX() + padding, label.getY());
                }
            }
        }
    }

    private void onComputerMouseClicked(String name) {
        Computer computer = devices.stream()
                .filter(d -> d instanceof Computer && d.getName().equals(name))
                .map(d -> (Computer) d)
                .findFirst()
                .orElse(null);

        if (computer == null) {
            JOptionPane.showMessageDialog(this, "Selected device is not a Computer.");
            return;
        }

        showDeviceInfoDialog(computer);
    }

    private void onSwitchMouseClicked(String name) {
        Switch switchDevice = devices.stream()
                .filter(d -> d instanceof Switch && d.getName().equals(name))
                .map(d -> (Switch) d)
                .findFirst()
                .orElse(null);

        if (switchDevice == null) {
            JOptionPane.showMessageDialog(this, "Selected device is not a Switch.");
            return;
        }

        showDeviceInfoDialog(switchDevice);
    }

    private void showDeviceInfoDialog(Device device) {
        JDialog dialog = new JDialog(this, "Device Information", true);
        dialog.setSize(300, 180);
        dialog.setLayout(new BorderLayout(10, 10));
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(new JLabel("Name:"));
        contentPanel.add(new JLabel(device.getName()));
        contentPanel.add(new JLabel("IP Address:"));
        contentPanel.add(new JLabel(device.getIp()));
        contentPanel.add(new JLabel("Linked:"));
        contentPanel.add(new JLabel(device instanceof Computer ? ((Computer) device).getLinkedDeviceName() :
                ((Switch) device).getLinkedDevicesNames()));

        JButton deleteButton = new JButton("Delete");
        JButton editButton = new JButton("Edit");

        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Delete " + device.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (device instanceof Computer) {
                    ((Computer) device).setLinkedDevice(null);
                } else {
                    ((Switch) device).setLinkedDevices(new ArrayList<>());
                }
                devices.remove(device);
                devicePanel.remove(device.label);
                devicePanel.revalidate();
                devicePanel.repaint();
                saveUserConfiguration();
                dialog.dispose();
            }
        });

        editButton.addActionListener(e -> {
            if (device instanceof Computer) {
                computerEditDialog((Computer) device, dialog);
            } else {
                switchEditDialog((Switch) device, dialog);
            }
        });

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(createButtonPanel(editButton, deleteButton), BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void computerEditDialog(Computer computer, JDialog parentDialog) {
        JDialog editDialog = createDeviceDialog("Edit Computer");
        JTextField nameField = new JTextField(computer.getName());
        JTextField ipField = new JTextField(computer.getIp());
        JComboBox<String> linkedDeviceCombo = new JComboBox<>();
        linkedDeviceCombo.addItem("None");
        for (Device d : devices) {
            if (!d.getName().equals(computer.getName()) &&
                    (!(d instanceof Computer) || ((Computer) d).getLinkedDevice() == null)) {
                linkedDeviceCombo.addItem(d.getName());
            }
        }
        linkedDeviceCombo.setSelectedItem(computer.getLinkedDeviceName());

        JPanel editPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        editPanel.add(new JLabel("Name:"));
        editPanel.add(nameField);
        editPanel.add(new JLabel("IP Address:"));
        editPanel.add(ipField);
        editPanel.add(new JLabel("Linked Device:"));
        editPanel.add(linkedDeviceCombo);

        JButton saveButton = new JButton("Save");
        editDialog.add(editPanel, BorderLayout.CENTER);
        editDialog.add(createButtonPanel(saveButton), BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newIp = validateAndFormatIP(ipField.getText(), editDialog);
            String newLinkedName = (String) linkedDeviceCombo.getSelectedItem();

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Name cannot be empty!");
                return;
            }
            if (newIp == null) {
                return;
            }

            if (devices.stream().anyMatch(d -> d != computer && d.getName().equals(newName))) {
                JOptionPane.showMessageDialog(editDialog, "Device name already exists!");
                return;
            }

            Device newLinkedDevice = newLinkedName.equals("None") ? null : findDeviceByName(newLinkedName);
            computer.setName(newName);
            computer.setIp(newIp);
            computer.setLinkedDevice(newLinkedDevice);
            computer.label.setText(newName);
            devicePanel.repaint();
            saveUserConfiguration();
            editDialog.dispose();
            parentDialog.dispose();
        });

        editDialog.setVisible(true);
    }

    private void switchEditDialog(Switch switchDevice, JDialog parentDialog) {
        JDialog editDialog = createDeviceDialog("Edit Switch");
        JTextField nameField = new JTextField(switchDevice.getName());
        JTextField ipField = new JTextField(switchDevice.getIp());
        JLabel linkedLabel = new JLabel(String.valueOf(switchDevice.getLinkedDevices().size()));
        JButton linkButton = new JButton("Link") {{
            setPreferredSize(new Dimension(80, 30));
        }};

        linkButton.addActionListener(e -> {
            JDialog linkDialog = new JDialog(editDialog, "Select Devices to Link", true);
            linkDialog.setSize(200, 250);
            linkDialog.setLayout(new BorderLayout());
            linkDialog.setLocationRelativeTo(editDialog);

            JPanel checkBoxPanel = new JPanel();
            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));
            List<JCheckBox> checkBoxes = new ArrayList<>();
            for (Device d : devices) {
                if (!d.getName().equals(switchDevice.getName()) &&
                        (!(d instanceof Computer) || ((Computer) d).getLinkedDevice() == null)) {
                    JCheckBox checkBox = new JCheckBox(d.getName());
                    checkBox.setSelected(switchDevice.getLinkedDevices().contains(d));
                    checkBoxes.add(checkBox);
                    checkBoxPanel.add(checkBox);
                }
            }

            JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
            linkDialog.add(scrollPane, BorderLayout.CENTER);

            JButton confirmButton = new JButton("Confirm");
            confirmButton.addActionListener(e1 -> {
                List<Device> newLinkedDevices = new ArrayList<>();
                for (JCheckBox checkBox : checkBoxes) {
                    if (checkBox.isSelected()) {
                        Device d = findDeviceByName(checkBox.getText());
                        if (d != null) {
                            newLinkedDevices.add(d);
                        }
                    }
                }
                linkedLabel.setText(String.valueOf(newLinkedDevices.size()));
                switchDevice.setLinkedDevices(newLinkedDevices);
                linkDialog.dispose();
            });

            linkDialog.add(createButtonPanel(confirmButton, new JButton("Cancel") {{
                addActionListener(e1 -> linkDialog.dispose());
            }}), BorderLayout.SOUTH);
            linkDialog.setVisible(true);
        });

        JPanel linkedDevicesPanel = new JPanel();
        linkedDevicesPanel.add(linkedLabel);
        linkedDevicesPanel.add(linkButton);

        JPanel editPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        editPanel.add(new JLabel("Name:"));
        editPanel.add(nameField);
        editPanel.add(new JLabel("IP Address:"));
        editPanel.add(ipField);
        editPanel.add(new JLabel("Linked Devices:"));
        editPanel.add(linkedDevicesPanel);

        JButton saveButton = new JButton("Save");
        editDialog.add(editPanel, BorderLayout.CENTER);
        editDialog.add(createButtonPanel(saveButton), BorderLayout.SOUTH);

        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newIp = validateAndFormatIP(ipField.getText(), editDialog);

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Name cannot be empty!");
                return;
            }
            if (newIp == null) {
                return;
            }

            if (devices.stream().anyMatch(d -> d != switchDevice && d.getName().equals(newName))) {
                JOptionPane.showMessageDialog(editDialog, "Device name already exists!");
                return;
            }

            switchDevice.setName(newName);
            switchDevice.setIp(newIp);
            switchDevice.label.setText(newName);
            devicePanel.repaint();
            saveUserConfiguration();
            editDialog.dispose();
            parentDialog.dispose();
        });

        editDialog.setVisible(true);
    }

    private String validateAndFormatIP(String ip, JDialog dialog) {
        if (ip == null || ip.trim().isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "IP Address cannot be empty!");
            return null;
        }

        String[] segments = ip.trim().split("\\.");
        if (segments.length != 4) {
            JOptionPane.showMessageDialog(dialog, "IP Address must have exactly four segments (e.g., 192.168.1.100)!");
            return null;
        }

        int[] values = new int[4];
        for (int i = 0; i < 4; i++) {
            try {
                values[i] = Integer.parseInt(segments[i].trim());
                if (values[i] < 0 || values[i] > 255) {
                    JOptionPane.showMessageDialog(dialog, "Each IP segment must be between 0 and 255!");
                    return null;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(dialog, "IP segments must be valid numbers!");
                return null;
            }
        }

        return String.format("%03d.%03d.%03d.%03d", values[0], values[1], values[2], values[3]);
    }
}