package Data;

import javax.swing.*;

public abstract class Device {
    protected String name;
    protected String ip;
    public JLabel label;

    public Device(String name, String ipAddress, JLabel label) {
        this.name = name;
        this.ip = ipAddress;
        this.label = label;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ipAddress) {
        this.ip = ipAddress;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", IP: " + ip;
    }
}