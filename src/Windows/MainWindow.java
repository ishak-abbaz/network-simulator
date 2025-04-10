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

    // Buttons
    private JButton addComputerButton, addSwitchButton;
    // Panel
    private JPanel devicePanel;
    // Devices list
    private ArrayList<Device> devices;

    // OnClick operations
    public void actionPerformed(ActionEvent ae){
        if(addComputerButton.equals(ae.getSource())){
            showAddComputerDialog();
        }else{
            showAddSwitchDialog();
        }
    }

    public MainWindow() {
        // Set up the frame
        setTitle("Network Simulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        devices = new ArrayList<>();

        // Create device display panel
        devicePanel = new JPanel(){
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawConnections(g); // Draw lines between linked devices
            }
        };
        devicePanel.setLayout(null);  // Using null layout for manual positioning
        add(new JScrollPane(devicePanel), BorderLayout.CENTER);

        // Initialize buttons
        addComputerButton = new JButton("Add Computer");
        addSwitchButton = new JButton("Add Switch");

        // Create panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        // Add buttons to the panel
        buttonPanel.add(addComputerButton);
        buttonPanel.add(addSwitchButton);
        // Add buttons panel to the frame
        add(buttonPanel, BorderLayout.SOUTH);

        addComputerButton.addActionListener(this);

        addSwitchButton.addActionListener(this);

        setVisible(true);
    }

    private void drawConnections(Graphics g) {
        g.setColor(Color.BLACK); // Line color
        for (Device device : devices) {
            if (device instanceof Computer) {
                Computer computer = (Computer) device;
                String linkedDeviceName = computer.getLinkedDevice();
                if (linkedDeviceName != null && !linkedDeviceName.equals("None")) {
                    Device linkedDevice = findDeviceByName(linkedDeviceName);
                    if (linkedDevice != null) {
                        drawLineBetweenLabels(g, computer.label, linkedDevice.label);
                    }
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

    // Helper to find a device by name
    private Device findDeviceByName(String name) {
        return devices.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    // Helper to draw a line between two JLabels
    private void drawLineBetweenLabels(Graphics g, JLabel label1, JLabel label2) {
        Point p1 = getLabelCenter(label1);
        Point p2 = getLabelCenter(label2);
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
    }

    // Helper to get the center point of a JLabel
    private Point getLabelCenter(JLabel label) {
        int x = label.getX() + label.getWidth() / 2;
        int y = label.getY() + label.getHeight() / 2;
        return new Point(x, y);
    }

    // Method to enter device information
    private void showAddComputerDialog() {
        // Create dialog
        JDialog dialog = new JDialog(this, "Add Computer", true);
        dialog.setSize(300, 200);
        dialog.setLayout(new BorderLayout(4, 4));
        dialog.setLocationRelativeTo(this);
        // Panel that contains device information text fields
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create input fields
        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();
        JComboBox<String> linkedDeviceCombo = new JComboBox<>();
        linkedDeviceCombo.addItem("None");  // Default null option
        // For each to add devices that exists to the combo box
        for (Device d : devices) {
            linkedDeviceCombo.addItem(d.getName());
        }
        // Input panel where to enter information about device
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Add components to dialog
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("IP Address:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Linked Device:"));
        inputPanel.add(linkedDeviceCombo);

        // Button and add it to the panel
        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);
        // Set the main panel of the dialog
        dialog.setContentPane(contentPanel);

        // Add created panel to the main panel(dialog)
        dialog.add(inputPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);


        // OK button action
        okButton.addActionListener(e -> {
            // Get the info user have put
            String name = nameField.getText();
            String ip = ipField.getText();
            String linkedDevice = (String) linkedDeviceCombo.getSelectedItem();
            // Condition to ensure text fields are not empty
            if (!name.isEmpty() && !ip.isEmpty()) {

                boolean found = false;

                for (Device d : devices) {
                    if(d.getName().equals(name)){
                        found = true;
                        break;
                    }
                }

                if (!found) {

                    JLabel label = createComputerLabel(name, ip, linkedDevice, "D:\\eclipse\\computerIcon.png");
                    placeDeviceWithoutOverlap(label);

                    Computer computer = new Computer(name, ip, label, linkedDevice);
                    devices.add(computer);
                    devicePanel.add(label);
                    devicePanel.repaint();
                    dialog.dispose();
                } else {
                    JOptionPane.showMessageDialog(dialog, "Device name already exists, please choose a different name.");
                }
            } else {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields");
            }
        });

        dialog.setVisible(true);
    }

    private JLabel createComputerLabel(String name, String ip, String linkedDevice, String iconPath) {
        JLabel label = new JLabel(name);
        label.setToolTipText("<html>Name: " + name + "<br>IP: " + ip + "<br>Linked: " + linkedDevice + "</html>");  // simple hover

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setBorder(null); // highlight
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setBorder(null); // remove highlight
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
        Point[] offset = {null};  // to store drag offset

        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = new Point(e.getX(), e.getY());
            }
        });

        label.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = label.getX() + e.getX() - offset[0].x;
                int y = label.getY() + e.getY() - offset[0].y;

                // Constrain x and y to stay within devicePanel bounds
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

        if (!placed) label.setBounds(20, 20, 80, 80); // fallback
    }

    private void onComputerMouseClicked(String name) {

        // Retrieve the selected device based on the name
        Computer computer = devices.stream()
                .filter(d -> d instanceof Computer && d.getName().equals(name))
                .map(d -> (Computer) d)
                .findFirst()
                .orElse(null);



        if (computer == null) {
            JOptionPane.showMessageDialog(this, "Selected device is not a Computer.");
            return;
        }

        // Create the device information dialog
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
        contentPanel.add(new JLabel(computer.getLinkedDevice()));

        JButton deleteButton = new JButton("Delete");
        JButton editButton = new JButton("Edit");

        // Delete button action
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Delete " + computer.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                devices.remove(computer);
                devicePanel.remove(computer.label);
                devicePanel.revalidate();
                devicePanel.repaint();
                dialog.dispose();
            }
        });

        // Edit button action
        editButton.addActionListener(e -> showEditDialog(computer, dialog));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showEditDialog(Computer computer, JDialog parentDialog) {
        // Create the edit dialog
        JDialog editDialog = new JDialog(parentDialog, "Edit Device", true);
        editDialog.setSize(300, 200);
        editDialog.setLocationRelativeTo(parentDialog);
        editDialog.setLayout(new BorderLayout(10, 10));

        // Pre-fill input fields with current values
        JTextField nameField = new JTextField(computer.getName());
        JTextField ipField = new JTextField(computer.getIp());

        // Set up linked device combo box
        JComboBox<String> linkedDeviceCombo = new JComboBox<>();
        linkedDeviceCombo.addItem("None");
        for (Device d : devices) {
            if (!d.getName().equals(computer.getName())) {
                linkedDeviceCombo.addItem(d.getName());
            }
        }
        linkedDeviceCombo.setSelectedItem(computer.getLinkedDevice());

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
            String newName = nameField.getText();
            String newIp = ipField.getText();
            String newLinked = (String) linkedDeviceCombo.getSelectedItem();

            if (!newName.isEmpty() && !newIp.isEmpty()) {
                // Update device fields and the label
                computer.setName(newName);
                computer.setIp(newIp);
                computer.setLinkedDevice(newLinked);
                computer.label.setText(newName); // Update the JLabel with the new name

                // Update the tooltip with new values
                computer.label.setToolTipText("<html>Name: " + newName + "<br>IP: " + newIp + "<br>Linked: " + newLinked + "</html>");

                devicePanel.repaint();  // Repaint to reflect changes
                editDialog.dispose();  // Close the edit dialog
                parentDialog.dispose(); // Close the parent dialog
            } else {
                JOptionPane.showMessageDialog(editDialog, "Name and IP cannot be empty!");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);

        editDialog.add(editPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }
    // Add switch segment
    private void showAddSwitchDialog() {
        // Create dialog
        JDialog dialog = new JDialog(this, "Add Switch", true);
        dialog.setSize(300, 200); // Adjusted size for compact layout
        dialog.setLayout(new BorderLayout(4, 4));
        dialog.setLocationRelativeTo(this);

        // Panel that contains device information
        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create input fields
        JTextField nameField = new JTextField();
        JTextField ipField = new JTextField();

        // JList with multiple selection
        DefaultListModel<String> model = new DefaultListModel<>();
        for (Device device : devices) {
            model.addElement(device.getName()); // Only store names
        }
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Add list to a scroll pane
        JScrollPane scrollPane = new JScrollPane(list);

        // Input panel with 3 labels, 2 text fields, and dropdown
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("IP Address:"));
        inputPanel.add(ipField);
        inputPanel.add(new JLabel("Linked Devices:"));
        inputPanel.add(scrollPane);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton okButton = new JButton("OK");
        buttonPanel.add(okButton);

        // Add panels to the content panel
        contentPanel.add(inputPanel, BorderLayout.CENTER);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setContentPane(contentPanel);

        // OK button action
        okButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            String ip = ipField.getText().trim();

            // Validate inputs
            if (name.isEmpty() || ip.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please fill all required fields");
                return;
            }

            // Check for duplicate name
            boolean found = false;
            for (Device d : devices) {
                if (d.getName().equals(name)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                JOptionPane.showMessageDialog(dialog, "Device name already exists, please choose a different name.");
            } else {

                JLabel label = createSwitchLabel(name, ip, "no linked devices", "D:\\eclipse\\switchIcon.png");
                placeDeviceWithoutOverlap(label);

                Switch newSwitch = new Switch(name, ip, label);
                
                devices.add(newSwitch);
                devicePanel.add(label);
                devicePanel.repaint();
                dialog.dispose();
            }
        });

        dialog.setVisible(true);
    }

    private JLabel createSwitchLabel(String name, String ip, String linkedDevices, String iconPath) {
        JLabel label = new JLabel(name);
        label.setToolTipText("<html>Name: " + name + "<br>IP: " + ip + "<br>Linked: " + linkedDevices + "</html>");  // simple hover

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setBorder(null); // highlight
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setBorder(null); // remove highlight
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
        Point[] offset = {null};  // to store drag offset

        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                offset[0] = new Point(e.getX(), e.getY());
            }
        });

        label.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int x = label.getX() + e.getX() - offset[0].x;
                int y = label.getY() + e.getY() - offset[0].y;

                // Constrain x and y to stay within devicePanel bounds
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

        // Retrieve the selected device based on the name
        Switch switchDevice = devices.stream()
                .filter(d -> d instanceof Switch && d.getName().equals(name))
                .map(d -> (Switch) d)
                .findFirst()
                .orElse(null);



        if (switchDevice == null) {
            JOptionPane.showMessageDialog(this, "Selected device is not a Computer.");
            return;
        }

        // Create the device information dialog
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
        contentPanel.add(new JLabel("Linked Device:"));
        contentPanel.add(new JLabel(switchDevice.getLinkedDevices().toString()));

        JButton deleteButton = new JButton("Delete");
        JButton editButton = new JButton("Edit");

        // Delete button action
        deleteButton.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(dialog,
                    "Delete " + switchDevice.getName() + "?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                devices.remove(switchDevice);
                devicePanel.remove(switchDevice.label);
                devicePanel.revalidate();
                devicePanel.repaint();
                dialog.dispose();
            }
        });

        // Edit button action
        editButton.addActionListener(e -> switchEditDialog(switchDevice, dialog));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void switchEditDialog(Switch switchDevice, JDialog parentDialog) {
        // Create the edit dialog
        JDialog editDialog = new JDialog(parentDialog, "Edit Device", true);
        editDialog.setSize(300, 200);
        editDialog.setLocationRelativeTo(parentDialog);
        editDialog.setLayout(new BorderLayout(10, 10));

        // Pre-fill input fields with current values
        JTextField nameField = new JTextField(switchDevice.getName());
        JTextField ipField = new JTextField(switchDevice.getIp());

        // Set up linked device combo box
        JComboBox<String> linkedDeviceCombo = new JComboBox<>();
        linkedDeviceCombo.addItem("None");
        for (Device d : devices) {
            if (!d.getName().equals(switchDevice.getName())) {
                linkedDeviceCombo.addItem(d.getName());
            }
        }
        linkedDeviceCombo.setSelectedItem(switchDevice.getLinkedDevices().toString());

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
            String newName = nameField.getText();
            String newIp = ipField.getText();

            if (!newName.isEmpty() && !newIp.isEmpty()) {
                // Update device fields and the label
                switchDevice.setName(newName);
                switchDevice.setIp(newIp);
                switchDevice.setLinkedDevices(switchDevice.getLinkedDevices());
                switchDevice.label.setText(newName); // Update the JLabel with the new name

                // Update the tooltip with new values
                switchDevice.label.setToolTipText("<html>Name: " + newName + "<br>IP: " + newIp + "<br>Linked: " + switchDevice.getLinkedDevices().toString() + "</html>");

                devicePanel.repaint();  // Repaint to reflect changes
                editDialog.dispose();  // Close the edit dialog
                parentDialog.dispose(); // Close the parent dialog
            } else {
                JOptionPane.showMessageDialog(editDialog, "Name and IP cannot be empty!");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);

        editDialog.add(editPanel, BorderLayout.CENTER);
        editDialog.add(buttonPanel, BorderLayout.SOUTH);
        editDialog.setVisible(true);
    }

}