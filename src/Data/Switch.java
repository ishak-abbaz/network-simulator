package Data;

import java.util.ArrayList;
import java.util.List;

public class Switch extends Device {

    List<Device> linkedDevices;

    public Switch(String name, String ipAddress) {
        super(name, ipAddress);
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