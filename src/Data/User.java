package Data;

public class User {
    private String usbSerialNumber, userName, password;
    public User(String usbSerialNumber, String userName, String password) {
        this.usbSerialNumber = usbSerialNumber;
        this.userName = userName;
        this.password = password;
    }
    public String getUserName() {
        return userName;
    }
    public String getPassword() {
        return password;
    }
    public String getUsbSerialNumber() {
        return usbSerialNumber;
    }
}
