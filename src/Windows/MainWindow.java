package Windows;

import Data.Computer;
import Data.Device;
import Data.Switch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
        devicePanel = new JPanel();
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

                    Computer computer = new Computer(name, ip, linkedDevice);
                    // Add device to devices list
                    devices.add(computer);
                    // Create icon for the device and add it
                    addComputerIcon(name);
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
    // Method to add icon to the main window(panel)
    private void addComputerIcon(String name) {
        // Create a label with computer icon
        JLabel deviceLabel = new JLabel(name);

        // Load the image from the specified path
        ImageIcon originalIcon = new ImageIcon("D:\\eclipse\\computerIcon.png");

        // Optional: Scale the image to a reasonable size (e.g., 64x64 pixels)
        Image scaledImage = originalIcon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        ImageIcon deviceIcon = new ImageIcon(scaledImage);

        // Set the icon and configure label
        deviceLabel.setIcon(deviceIcon);
        deviceLabel.setHorizontalTextPosition(JLabel.CENTER);
        deviceLabel.setVerticalTextPosition(JLabel.BOTTOM);

        // Position the icon (simple incremental positioning)
        int x = (devices.size() - 1) * 100 + 20;
        int y = 20;
        deviceLabel.setBounds(x, y, 80, 80);

        // Make the label clickable by adding a MouseListener
        deviceLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Action to perform when the icon is clicked
                String currentName = deviceLabel.getText();
                onComputerMouseClicked(currentName);
                // You can replace this with any action, e.g., opening a new dialog, showing device details, etc.
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Optional: Change cursor to hand when hovering over the icon
                deviceLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Optional: Reset cursor when leaving the icon
                deviceLabel.setCursor(Cursor.getDefaultCursor());
            }
        });

        devicePanel.add(deviceLabel);
        devicePanel.revalidate();
        devicePanel.repaint();
    }


    private void onComputerMouseClicked(String name) {
        // Find the selected device
        System.out.println(name);
        Device selectedDevice = devices.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);
        // Check if the device exists and is a Computer

        Computer computer;
        if (selectedDevice instanceof Computer) {
            computer = (Computer) selectedDevice;
        } else {
            computer = null;
        }

        // Create the dialog
        JDialog dialog = new JDialog(this, "Device Information", true);
        dialog.setSize(300, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Content panel for device details
        JPanel contentPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add device details
        contentPanel.add(new JLabel("Name:"));
        contentPanel.add(new JLabel(computer.getName()));
        contentPanel.add(new JLabel("IP Address:"));
        contentPanel.add(new JLabel(computer.getIp()));
        contentPanel.add(new JLabel("Linked Device:"));
        contentPanel.add(new JLabel(computer.getLinkedDevice()));

        // Button panel (only show buttons if a Computer is selected)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (computer != null) {
            JButton deleteButton = new JButton("Delete");
            JButton editButton = new JButton("Edit");

            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);

            // Delete button action
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to delete " + computer.getName() + "?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    devices.remove(computer);
                    // Remove the corresponding icon from devicePanel
                    for (Component comp : devicePanel.getComponents()) {
                        if (comp instanceof JLabel && ((JLabel) comp).getText().equals(name)) {
                            devicePanel.remove(comp);
                            break;
                        }
                    }
                    devicePanel.revalidate();
                    devicePanel.repaint();
                    dialog.dispose();
                }
            });

            // Edit button action
            editButton.addActionListener(e -> {
                // Create an edit dialog
                JDialog editDialog = new JDialog(dialog, "Edit Device", true);
                editDialog.setSize(300, 200);
                editDialog.setLocationRelativeTo(dialog);
                editDialog.setLayout(new BorderLayout(10, 10));

                // Input fields pre-filled with current values
                JTextField nameField = new JTextField(computer.getName());
                JTextField ipField = new JTextField(computer.getIp());

                JComboBox<String> linkedDeviceCombo = new JComboBox<>();
                boolean isLinked = !computer.getLinkedDevice().equals("None");
                if(isLinked){
                    linkedDeviceCombo.addItem(computer.getLinkedDevice());
                    for (Device d : devices) {
                        if(!d.getName().equals(computer.getLinkedDevice())) {
                            if(!d.getName().equals(computer.getName())) {
                                linkedDeviceCombo.addItem(d.getName());
                            }
                        }
                    }
                    linkedDeviceCombo.addItem("None");
                }else{
                    linkedDeviceCombo.addItem("None");
                    for (Device d : devices) {
                        if(!d.getName().equals(computer.getName())) {
                            linkedDeviceCombo.addItem(d.getName());
                        }
                    }
                }


                JPanel editPanel = new JPanel(new GridLayout(3, 3, 10, 10));
                editPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                editPanel.add(new JLabel("Name:"));
                editPanel.add(nameField);
                editPanel.add(new JLabel("IP Address:"));
                editPanel.add(ipField);
                editPanel.add(new JLabel("Linked Device:"));
                editPanel.add(linkedDeviceCombo);

                JButton saveButton = new JButton("Save");
                JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                editButtonPanel.add(saveButton);

                saveButton.addActionListener(saveEvent -> {
                    String newName = nameField.getText();
                    String newIp = ipField.getText();
                    String newLinkedDevice = linkedDeviceCombo.getSelectedItem().toString();

                    if (!newName.isEmpty() && !newIp.isEmpty()) {
                        computer.setName(newName);
                        computer.setIp(newIp);
                        computer.setLinkedDevice(newLinkedDevice);
                        // Update the icon text if the name changes
                        for (Component comp : devicePanel.getComponents()) {
                            if (comp instanceof JLabel && ((JLabel) comp).getText().equals(name)) {
                                ((JLabel) comp).setText(newName);
                                break;
                            }
                        }
                        devicePanel.revalidate();
                        devicePanel.repaint();
                        editDialog.dispose();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(editDialog, "Name and IP cannot be empty!");
                    }
                });

                editDialog.add(editPanel, BorderLayout.CENTER);
                editDialog.add(editButtonPanel, BorderLayout.SOUTH);
                editDialog.setVisible(true);
            });
        } else {
            // If not a Computer, show a message instead of buttons
            buttonPanel.add(new JLabel("Not a Computer device"));
        }

        // Add panels to dialog
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
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

                // Create and add the new Switch
                Switch newSwitch = new Switch(name, ip);
                devices.add(newSwitch);
                addSwitchIcon(name); // Rename to addSwitchIcon if appropriate

                dialog.dispose(); // Close dialog on success
            }
        });

        dialog.setVisible(true);
    }

    // Method to add icon to the main window(panel)
    private void addSwitchIcon(String name) {
        // Create a label with computer icon
        JLabel deviceLabel = new JLabel(name);

        // Load the image from the specified path
        ImageIcon originalIcon = new ImageIcon("D:\\eclipse\\switchIcon.png");

        // Optional: Scale the image to a reasonable size (e.g., 64x64 pixels)
        Image scaledImage = originalIcon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
        ImageIcon deviceIcon = new ImageIcon(scaledImage);

        // Set the icon and configure label
        deviceLabel.setIcon(deviceIcon);
        deviceLabel.setHorizontalTextPosition(JLabel.CENTER);
        deviceLabel.setVerticalTextPosition(JLabel.BOTTOM);

        // Position the icon (simple incremental positioning)
        int x = (devices.size() - 1) * 100 + 20;
        int y = 20;
        deviceLabel.setBounds(x, y, 80, 80);

        // Make the label clickable by adding a MouseListener
        deviceLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Action to perform when the icon is clicked
                String currentName = deviceLabel.getText();
                onSwitchMouseClicked(currentName);
                // You can replace this with any action, e.g., opening a new dialog, showing device details, etc.
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                // Optional: Change cursor to hand when hovering over the icon
                deviceLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // Optional: Reset cursor when leaving the icon
                deviceLabel.setCursor(Cursor.getDefaultCursor());
            }
        });

        devicePanel.add(deviceLabel);
        devicePanel.revalidate();
        devicePanel.repaint();
    }
//
//
    private void onSwitchMouseClicked(String name) {
        // Find the selected device
        Device selectedDevice = devices.stream()
                .filter(d -> d.getName().equals(name))
                .findFirst()
                .orElse(null);

        // Check if the device exists and is a Switch
        Switch selectedSwitch;
        if (selectedDevice instanceof Switch) {
            selectedSwitch = (Switch) selectedDevice;
        } else {
            selectedSwitch = null;
        }

        // Create the dialog
        JDialog dialog = new JDialog(this, "Device Information", true);
        dialog.setSize(300, 180);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // Content panel for device details
        JPanel contentPanel = new JPanel(new GridLayout(3, 3, 10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add device details
        contentPanel.add(new JLabel("Name:"));
        contentPanel.add(new JLabel(selectedSwitch.getName()));
        contentPanel.add(new JLabel("IP Address:"));
        contentPanel.add(new JLabel(selectedSwitch.getIp()));
        contentPanel.add(new JLabel("Linked Device:"));
        List<Device> linkedDevices = selectedSwitch.getLinkedDevices();
        String deviceNames = linkedDevices.stream()
                .map(Device::getName) // Assuming Device has a getName() method
                .collect(Collectors.joining(", "));
        contentPanel.add(new JLabel(deviceNames));


        // Button panel (only show buttons if a Computer is selected)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        if (selectedSwitch != null) {
            JButton deleteButton = new JButton("Delete");
            JButton editButton = new JButton("Edit");

            buttonPanel.add(editButton);
            buttonPanel.add(deleteButton);

            // Delete button action
            deleteButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(dialog,
                        "Are you sure you want to delete " + selectedSwitch.getName() + "?",
                        "Confirm Deletion", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    devices.remove(selectedSwitch);
                    // Remove the corresponding icon from devicePanel
                    for (Component comp : devicePanel.getComponents()) {
                        if (comp instanceof JLabel && ((JLabel) comp).getText().equals(name)) {
                            devicePanel.remove(comp);
                            break;
                        }
                    }
                    devicePanel.revalidate();
                    devicePanel.repaint();
                    dialog.dispose();
                }
            });

            // Edit button action
            editButton.addActionListener(e -> {
                // Create an edit dialog
                JDialog editDialog = new JDialog(dialog, "Edit Device", true);
                editDialog.setSize(300, 200);
                editDialog.setLocationRelativeTo(dialog);
                editDialog.setLayout(new BorderLayout(10, 10));

                // Input fields pre-filled with current values
                JTextField nameField = new JTextField(selectedSwitch.getName());
                JTextField ipField = new JTextField(selectedSwitch.getIp());


                JPanel editPanel = new JPanel(new GridLayout(3, 3, 10, 10));
                editPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                editPanel.add(new JLabel("Name:"));
                editPanel.add(nameField);
                editPanel.add(new JLabel("IP Address:"));
                editPanel.add(ipField);
                editPanel.add(new JLabel("Linked Device:"));
                editPanel.add(new JLabel(""));

                JButton saveButton = new JButton("Save");
                JPanel editButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
                editButtonPanel.add(saveButton);

                saveButton.addActionListener(saveEvent -> {
                    String newName = nameField.getText();
                    String newIp = ipField.getText();

                    if (!newName.isEmpty() && !newIp.isEmpty()) {
                        selectedSwitch.setName(newName);
                        selectedSwitch.setIp(newIp);
                        // Update the icon text if the name changes
                        for (Component comp : devicePanel.getComponents()) {
                            if (comp instanceof JLabel && ((JLabel) comp).getText().equals(name)) {
                                ((JLabel) comp).setText(newName);
                                break;
                            }
                        }
                        devicePanel.revalidate();
                        devicePanel.repaint();
                        editDialog.dispose();
                        dialog.dispose();
                    } else {
                        JOptionPane.showMessageDialog(editDialog, "Name and IP cannot be empty!");
                    }
                });

                editDialog.add(editPanel, BorderLayout.CENTER);
                editDialog.add(editButtonPanel, BorderLayout.SOUTH);
                editDialog.setVisible(true);
            });
        } else {
            // If not a Computer, show a message instead of buttons
            buttonPanel.add(new JLabel("Not a Computer device"));
        }

        // Add panels to dialog
        dialog.add(contentPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

}
