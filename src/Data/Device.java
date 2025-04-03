package Data;

public class Device {
    private String name;
    private String ip;

    // Constructor
    public Device(String name, String ipAddress) {
        this.name = name;
        this.ip = ipAddress;
    }

    // Getters and setters
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

    // toString method for easy display
    @Override
    public String toString() {
        return "Name: " + name + ", IP: " + ip;
    }
}
