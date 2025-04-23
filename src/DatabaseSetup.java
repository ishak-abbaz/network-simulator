import java.sql.*;

public class DatabaseSetup {
    public void setupAndInsert(String password) {
        String url = "jdbc:mysql://localhost:3306/";
        String user = "root";
        String dbName = "networksimulatorusers";

        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {

            // Create database
            String createDatabase = "CREATE DATABASE IF NOT EXISTS " + dbName;
            stmt.executeUpdate(createDatabase);

            // Use the new database
            stmt.executeUpdate("USE " + dbName);

            // Create users table
            String createTable = "CREATE TABLE IF NOT EXISTS users (" +
                    "usbSerialNum CHAR(8), " +
                    "userName VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(50), " +
                    "role VARCHAR(50) DEFAULT 'user')";
            stmt.executeUpdate(createTable);

            // Insert tuple
            String insertSQL = "INSERT INTO users (usbSerialNum, userName, password, role) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {
                pstmt.setString(1, "1681C75D");
                pstmt.setString(2, "ishak");
                pstmt.setString(3, "root");
                pstmt.setString(4, "admin");
                pstmt.executeUpdate();
            }

            System.out.println("Database setup and user insertion completed successfully");

        } catch (SQLException e) {
            System.err.println("Operation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}