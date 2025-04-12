package Data;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Switch extends Device {
    private List<Device> linkedDevices;

    public Switch(String name, String ipAddress, JLabel label) {
        super(name, ipAddress, label);
        this.linkedDevices = new ArrayList<>();
    }

    public List<Device> getLinkedDevices() {
        return linkedDevices;
    }

    public void setLinkedDevices(List<Device> newLinkedDevices) {
        // Remove this Switch from the old linked devices
        for (Device oldLinked : linkedDevices) {
            if (oldLinked instanceof Computer) {
                Computer comp = (Computer) oldLinked;
                if (comp.getLinkedDevice() == this) {
                    comp.setLinkedDevice(null);
                }
            } else if (oldLinked instanceof Switch) {
                ((Switch) oldLinked).getLinkedDevices().remove(this);
            }
        }

        linkedDevices.clear();
        linkedDevices.addAll(newLinkedDevices);

        // Update new linked devices
        for (Device newLinked : newLinkedDevices) {
            if (newLinked instanceof Computer) {
                Computer comp = (Computer) newLinked;
                if (comp.getLinkedDevice() != this) {
                    comp.setLinkedDevice(this);
                }
            } else if (newLinked instanceof Switch) {
                List<Device> otherLinkedDevices = ((Switch) newLinked).getLinkedDevices();
                if (!otherLinkedDevices.contains(this)) {
                    otherLinkedDevices.add(this);
                }
            }
        }
    }

    public String getLinkedDevicesNames() {
        return linkedDevices.isEmpty() ? "None" : linkedDevices.stream()
                .map(Device::getName)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return super.toString() + ", Linked: " + getLinkedDevicesNames();
    }
}