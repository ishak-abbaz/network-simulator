package Data;

public class Computer extends Device {
    private String linkedDevice;
    public Computer(String name, String ipAddress, String linkedDevice) {
        super(name, ipAddress);
        this.linkedDevice = linkedDevice;
    }

    public String getLinkedDevice() {
        return linkedDevice;
    }

    public void setLinkedDevice(String linkedDevice) {
        this.linkedDevice = linkedDevice;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
