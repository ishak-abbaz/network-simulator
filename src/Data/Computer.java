package Data;

import javax.swing.*;
import java.util.List;

public class Computer extends Device {
    private Device linkedDevice; // Store Device reference instead of String

    public Computer(String name, String ipAddress, JLabel label, Device linkedDevice) {
        super(name, ipAddress, label);
        this.linkedDevice = linkedDevice;
    }

    public Device getLinkedDevice() {
        return linkedDevice;
    }

    public void setLinkedDevice(Device newLinkedDevice) {
        // Remove this Computer from the old linked device's linkedDevices (if it’s a Switch)
        if (this.linkedDevice != null && this.linkedDevice instanceof Switch) {
            ((Switch) this.linkedDevice).getLinkedDevices().remove(this);
        }

        this.linkedDevice = newLinkedDevice;

        // Add this Computer to the new linked device's linkedDevices (if it’s a Switch)
        if (newLinkedDevice != null && newLinkedDevice instanceof Switch) {
            List<Device> linkedDevices = ((Switch) newLinkedDevice).getLinkedDevices();
            if (!linkedDevices.contains(this)) {
                linkedDevices.add(this);
            }
        }
    }

    public String getLinkedDeviceName() {
        return linkedDevice != null ? linkedDevice.getName() : "None";
    }

    @Override
    public String toString() {
        return super.toString() + ", Linked: " + getLinkedDeviceName();
    }
}