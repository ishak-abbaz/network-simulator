package Data;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Switch extends Device {

    List<Device> linkedDevices;

    public Switch(String name, String ipAddress, JLabel label) {
        super(name, ipAddress, label);
        this.linkedDevices = new ArrayList<Device>();
    }



    public List<Device> getLinkedDevices() {
        return linkedDevices;
    }

    public void setLinkedDevices(List<Device> linkedDevices) {
        this.linkedDevices = linkedDevices;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}