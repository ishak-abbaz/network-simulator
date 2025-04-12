package Windows;

import Data.Computer;
import Data.Device;
import Data.Switch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainWindow extends JFrame implements ActionListener {

    private JButton addComputerButton, addSwitchButton;
    private JPanel devicePanel;
    private ArrayList<Device> devices;

    public void actionPerformed(ActionEvent ae) {
        if (addComputerButton.equals(ae.getSource())) {
            showAddComputerDialog();
        } else {
            showAddSwitchDialog();
        }
    }

    public MainWindow() {
        setTitle("Network Simulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        devices = new ArrayList<>();

        devicePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawConnections(g);
            }
        };
        devicePanel.setLayout(null);
        add(new JScrollPane(devicePanel), BorderLayout.CENTER);

        addComputerButton = new JButton("Add Computer");
        addSwitchButton = new JButton("Add Switch");

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addComputerButton);
        buttonPanel.add(addSwitchButton);
        add(buttonPanel, BorderLayout.SOUTH);

        addComputerButton.addActionListener(this);
        addSwitchButton.addActionListener(this);

        setVisible(true);
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
        return devices.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
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
        JDialog dialog = new JDialog(this, "Add Computer", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new BorderLayout(4, 4));
        dialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

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
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("IP Address:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Linked Device:"));
        inputPanel.add(linkedDeviceCombo);

        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);
        dialog.setContentPane(contentPanel);
        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = validateAndFormatIP(ipField.getText(), dialog);
            String linkedDeviceName = (String) linkedDeviceCombo.getSelectedItem();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Name cannot be empty!");
                return;
            }
            if (ip == null) {
                return; // Validation failed, error already shown
            }

            boolean found = false;
            for (Device d : devices) {
                if (d.getName().equals(name)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                Device linkedDevice = linkedDeviceName.equals("None") ? null : findDeviceByName(linkedDeviceName);
                JLabel label = createComputerLabel(name, ip, linkedDevice, "D:\\eclipse\\computerIcon.png");
                placeDeviceWithoutOverlap(label);

                Computer computer = new Computer(name, ip, label, linkedDevice);
                if (linkedDevice != null) {
                    computer.setLinkedDevice(linkedDevice); // Updates bidirectional link
                }
                devices.add(computer);
                devicePanel.add(label);
                devicePanel.repaint();
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Device name already exists, please choose a different name.");
            }
        });

        dialog.setVisible(true);
    }

    private JLabel createComputerLabel(String name, String ip, Device linkedDevice, String iconPath) {
        JLabel label = new JLabel(name);
        label.setToolTipText("<html>Name: " + name + "<br>IP: " + ip + "<br>Linked: " + (linkedDevice != null ? linkedDevice.getName() : "None") + "</html>");

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Find the device associated with this label
                Device device = devices.stream()
                        .filter(d -> d.label == label)
                        .findFirst()
                        .orElse(null);
                if (device instanceof Computer) {
                    Computer computer = (Computer) device;
                    label.setToolTipText("<html>Name: " + computer.getName() +
                            "<br>IP: " + computer.getIp() +
                            "<br>Linked: " + computer.getLinkedDeviceName() + "</html>");
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                String currentName = label.getText();
                onComputerMouseClicked(currentName);
            }
        });

        ImageIcon icon = new ImageIcon(new ImageIcon(iconPath).getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH));
        label.setIcon(icon);
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
            }
        });

        return label;
    }

    private void placeDeviceWithoutOverlap(JLabel label) {
        int padding = 100;
        boolean placed = false;

        for (int x = 20; x < devicePanel.getWidth() - 80; x += padding) {
            for (int y = 20; y < devicePanel.getHeight() - 80; y += padding) {
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

        if (!placed) label.setBounds(20, 20, 80, 80);
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

        JDialog dialog = new JDialog(this, "Device Information", true);
        dialog.setSize(300, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(new JLabel("Name:"));
        contentPanel.add(new JLabel(computer.getName()));
        contentPanel.add(new JLabel("IP Address:"));
        contentPanel.add(new JLabel(computer.getIp()));
        contentPanel.add(new JLabel("Linked Device:"));
        contentPanel.add(new JLabel(computer.getLinkedDeviceName()));

        JButton deleteButton = new JButton("Delete");
        JButton editButton = new JButton("Edit");

        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Delete " + computer.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                // Unlink from any linked device
                computer.setLinkedDevice(null);
                devices.remove(computer);
                devicePanel.remove(computer.label);
                devicePanel.revalidate();
                devicePanel.repaint();
                dialog.dispose();
            }
        });

        editButton.addActionListener(e -> computerEditDialog(computer, dialog));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void computerEditDialog(Computer computer, JDialog parentDialog) {
        JDialog editDialog = new JDialog(parentDialog, "Edit Device", true);
        editDialog.setSize(300, 200);
        editDialog.setLocationRelativeTo(parentDialog);
        editDialog.setLayout(new BorderLayout(10, 10));

        JTextField nameField = new JTextField(computer.getName());
        JTextField ipField = new JTextField(computer.getIp());
        JComboBox<String> linkedDeviceCombo = new JComboBox<>();
        linkedDeviceCombo.addItem("None");
        for (Device d : devices) {
            if (!d.getName().equals(computer.getName())) {
                if (!(d instanceof Computer) || ((Computer) d).getLinkedDevice() == null) {
                    linkedDeviceCombo.addItem(d.getName());
                }
            }
        }
        linkedDeviceCombo.setSelectedItem(computer.getLinkedDeviceName());

        JPanel editPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        editPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editPanel.add(new JLabel("Name:"));
        editPanel.add(nameField);
        editPanel.add(new JLabel("IP Address:"));
        editPanel.add(ipField);
        editPanel.add(new JLabel("Linked Device:"));
        editPanel.add(linkedDeviceCombo);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newIp = validateAndFormatIP(ipField.getText(), editDialog);
            String newLinkedName = (String) linkedDeviceCombo.getSelectedItem();

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Name cannot be empty!");
                return;
            }
            if (newIp == null) {
                return; // Validation failed, error already shown
            }

            // Check for duplicate name
            boolean nameExists = devices.stream()
                    .anyMatch(d -> d != computer && d.getName().equals(newName));
            if (nameExists) {
                JOptionPane.showMessageDialog(editDialog, "Device name already exists, please choose a different name.");
                return;
            }

            Device newLinkedDevice = newLinkedName.equals("None") ? null : findDeviceByName(newLinkedName);
            computer.setName(newName);
            computer.setIp(newIp);
            computer.setLinkedDevice(newLinkedDevice); // Updates bidirectional link
            computer.label.setText(newName);
            devicePanel.repaint();
            editDialog.dispose();
            parentDialog.dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);

        editDialog.add(editPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }

    private void showAddSwitchDialog() {
        List<Device> linkedDevices = new ArrayList<>();

        JDialog addSwitchDialog = new JDialog(this, "Add Switch", true);
        addSwitchDialog.setSize(300, 200);
        addSwitchDialog.setLayout(new BorderLayout(4, 4));
        addSwitchDialog.setLocationRelativeTo(this);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();

        JPanel linkedDevicesPanel = new JPanel();
        JLabel linkedLabel = new JLabel(String.valueOf(linkedDevices.size()));
        JButton linkButton = new JButton("Link");
        linkButton.setPreferredSize(new Dimension(80, 30));
        linkedDevicesPanel.add(linkedLabel);
        linkedDevicesPanel.add(linkButton);

        linkButton.addActionListener(e -> {
            JDialog dialog = new JDialog(addSwitchDialog, "Select Devices to Link", true);
            dialog.setSize(200, 250);
            dialog.setLayout(new BorderLayout());

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
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton confirmButton = new JButton("Confirm");
            JButton cancelButton = new JButton("Cancel");

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
                dialog.dispose();
            });

            cancelButton.addActionListener(e1 -> dialog.dispose());

            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setLocationRelativeTo(addSwitchDialog);
            dialog.setVisible(true);
        });

        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("IP Address:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Linked Devices:"));
        inputPanel.add(linkedDevicesPanel);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);

        contentPanel.add(inputPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        addSwitchDialog.setContentPane(contentPanel);

        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = validateAndFormatIP(ipField.getText(), addSwitchDialog);

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(addSwitchDialog, "Name cannot be empty!");
                return;
            }
            if (ip == null) {
                return; // Validation failed, error already shown
            }

            boolean found = false;
            for (Device d : devices) {
                if (d.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                JOptionPane.showMessageDialog(addSwitchDialog, "Device name already exists, please choose a different name.");
            } else {
                JLabel label = createSwitchLabel(name, ip, linkedDevices, "D:\\eclipse\\switchIcon.png");
                placeDeviceWithoutOverlap(label);

                Switch newSwitch = new Switch(name, ip, label);
                newSwitch.setLinkedDevices(new ArrayList<>(linkedDevices)); // Updates bidirectional links
                devices.add(newSwitch);
                devicePanel.add(label);
                devicePanel.repaint();
                addSwitchDialog.dispose();
            }
        });

        addSwitchDialog.setVisible(true);
    }

    private JLabel createSwitchLabel(String name, String ip, List<Device> linkedDevices, String iconPath) {
        JLabel label = new JLabel(name);
        String linkedNames = linkedDevices.isEmpty() ? "None" : linkedDevices.stream()
                .map(Device::getName)
                .collect(Collectors.joining(", "));
        label.setToolTipText("<html>Name: " + name + "<br>IP: " + ip + "<br>Linked: " + linkedNames + "</html>");

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                Device device = devices.stream()
                        .filter(d -> d.label == label)
                        .findFirst()
                        .orElse(null);
                if (device instanceof Switch) {
                    Switch switchDevice = (Switch) device;
                    label.setToolTipText("<html>Name: " + switchDevice.getName() +
                            "<br>IP: " + switchDevice.getIp() +
                            "<br>Linked: " + switchDevice.getLinkedDevicesNames() + "</html>");
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                String currentName = label.getText();
                onSwitchMouseClicked(currentName);
            }
        });

        ImageIcon icon = new ImageIcon(new ImageIcon(iconPath).getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH));
        label.setIcon(icon);
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
            }
        });

        return label;
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

        JDialog dialog = new JDialog(this, "Device Information", true);
        dialog.setSize(300, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel contentPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        contentPanel.add(new JLabel("Name:"));
        contentPanel.add(new JLabel(switchDevice.getName()));
        contentPanel.add(new JLabel("IP Address:"));
        contentPanel.add(new JLabel(switchDevice.getIp()));
        contentPanel.add(new JLabel("Linked Devices:"));
        contentPanel.add(new JLabel(switchDevice.getLinkedDevicesNames()));

        JButton deleteButton = new JButton("Delete");
        JButton editButton = new JButton("Edit");

        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Delete " + switchDevice.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                switchDevice.setLinkedDevices(new ArrayList<>()); // Clear links
                devices.remove(switchDevice);
                devicePanel.remove(switchDevice.label);
                devicePanel.revalidate();
                devicePanel.repaint();
                dialog.dispose();
            }
        });

        editButton.addActionListener(e -> switchEditDialog(switchDevice, dialog));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void switchEditDialog(Switch switchDevice, JDialog parentDialog) {
        JDialog editDialog = new JDialog(parentDialog, "Edit Switch", true);
        editDialog.setSize(300, 200);
        editDialog.setLocationRelativeTo(parentDialog);
        editDialog.setLayout(new BorderLayout(10, 10));

        JTextField nameField = new JTextField(switchDevice.getName());
        JTextField ipField = new JTextField(switchDevice.getIp());

        JPanel linkedDevicesPanel = new JPanel();
        JLabel linkedLabel = new JLabel(String.valueOf(switchDevice.getLinkedDevices().size()));
        JButton linkButton = new JButton("Link");
        linkButton.setPreferredSize(new Dimension(80, 30));
        linkedDevicesPanel.add(linkedLabel);
        linkedDevicesPanel.add(linkButton);

        linkButton.addActionListener(e -> {
            JDialog dialog = new JDialog(this, "Select Devices to Link", true);
            dialog.setSize(200, 250);
            dialog.setLayout(new BorderLayout());

            JPanel checkBoxPanel = new JPanel();
            checkBoxPanel.setLayout(new BoxLayout(checkBoxPanel, BoxLayout.Y_AXIS));

            List<JCheckBox> checkBoxes = new ArrayList<>();
            for (Device d : devices) {
                if (!d.getName().equals(switchDevice.getName())) {
                    if (!(d instanceof Computer) || ((Computer) d).getLinkedDevice() == null) {
                        JCheckBox checkBox = new JCheckBox(d.getName());
                        checkBox.setSelected(switchDevice.getLinkedDevices().contains(d));
                        checkBoxes.add(checkBox);
                        checkBoxPanel.add(checkBox);
                    }
                }
            }

            JScrollPane scrollPane = new JScrollPane(checkBoxPanel);
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel();
            JButton confirmButton = new JButton("Confirm");
            JButton cancelButton = new JButton("Cancel");

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
                switchDevice.setLinkedDevices(newLinkedDevices); // Updates bidirectional links
                dialog.dispose();
            });

            cancelButton.addActionListener(e1 -> dialog.dispose());

            buttonPanel.add(confirmButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setLocationRelativeTo(this);
            dialog.setVisible(true);
        });

        JPanel editPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        editPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editPanel.add(new JLabel("Name:"));
        editPanel.add(nameField);
        editPanel.add(new JLabel("IP Address:"));
        editPanel.add(ipField);
        editPanel.add(new JLabel("Linked Devices:"));
        editPanel.add(linkedDevicesPanel);

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(e -> {
            String newName = nameField.getText().trim();
            String newIp = validateAndFormatIP(ipField.getText(), editDialog);

            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(editDialog, "Name cannot be empty!");
                return;
            }
            if (newIp == null) {
                return; // Validation failed, error already shown
            }

            // Check for duplicate name
            boolean nameExists = devices.stream()
                    .anyMatch(d -> d != switchDevice && d.getName().equals(newName));
            if (nameExists) {
                JOptionPane.showMessageDialog(editDialog, "Device name already exists, please choose a different name.");
                return;
            }

            switchDevice.setName(newName);
            switchDevice.setIp(newIp);
            switchDevice.label.setText(newName);
            devicePanel.repaint();
            editDialog.dispose();
            parentDialog.dispose();
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);

        editDialog.add(editPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
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

        // Format each segment to three digits with leading zeros
        return String.format("%03d.%03d.%03d.%03d", values[0], values[1], values[2], values[3]);
    }

}