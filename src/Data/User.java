package Data;

public class User {
    private String usbSerialNumber, userName, password, role;
    public User(String usbSerialNumber, String userName, String password, String role) {
        this.usbSerialNumber = usbSerialNumber;
        this.userName = userName;
        this.password = password;
        this.role = role;
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
    public String getRole() {
        return role;
    }
    public void setRole(String role) {
        this.role = role;
    }

    public void setUsbSerialNumber(String usbSerialNumber) {
        this.usbSerialNumber = usbSerialNumber;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
