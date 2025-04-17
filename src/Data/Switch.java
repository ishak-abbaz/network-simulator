package Data;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Switch extends Device {
    private List<Device> linkedDevices;

    public Switch(String name, String ip, JLabel label) {
        super(name, ip, label);
        this.linkedDevices = new ArrayList<>();
    }

    public List<Device> getLinkedDevices() {
        return new ArrayList<>(linkedDevices); // Return a copy to prevent external modification
    }

    public void setLinkedDevices(List<Device> newLinkedDevices) {
        // Safely replace the linkedDevices list without iteration
        this.linkedDevices = new ArrayList<>(newLinkedDevices != null ? newLinkedDevices : new ArrayList<>());
    }

    public String getLinkedDevicesNames() {
        return linkedDevices.stream()
                .map(Device::getName)
                .collect(Collectors.joining(", "));
    }
}