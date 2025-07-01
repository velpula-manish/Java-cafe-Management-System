import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class MainApp {
    private JFrame frame;
    private Connection conn;
    private UserManagement userManagement;
    private MenuManagement menuManagement;
    private OrderBilling orderBilling;
    private InventoryTracking inventoryTracking;
    private ReportAnalysis reportAnalysis;

    public MainApp() {
        initializeDatabase();
        initializeUI();
    }

    // Initialize MySQL database connection
    private void initializeDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/cafe_db?useSSL=false";
            String user = "root";
            String password = "Manishsql@9989"; // Replace with your MySQL password
            conn = DriverManager.getConnection(url, user, password);
            createTables();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Database connection failed: " + e.getMessage());
            System.exit(1);
        }
    }

    // Create necessary tables
    private void createTables() throws SQLException {
        String userTable = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) UNIQUE, password VARCHAR(64), role VARCHAR(20))";
        String menuTable = "CREATE TABLE IF NOT EXISTS menu_items (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100), category VARCHAR(50), price DECIMAL(10,2), available BOOLEAN, stock_quantity INT DEFAULT 100)";
        String orderTable = "CREATE TABLE IF NOT EXISTS orders (id INT AUTO_INCREMENT PRIMARY KEY, order_type VARCHAR(20), table_number VARCHAR(10), subtotal DECIMAL(10,2), gst DECIMAL(10,2), discount DECIMAL(10,2), total DECIMAL(10,2), order_date DATETIME)";
        String orderItemTable = "CREATE TABLE IF NOT EXISTS order_items (id INT AUTO_INCREMENT PRIMARY KEY, order_id INT, item_id INT, quantity INT, FOREIGN KEY (order_id) REFERENCES orders(id), FOREIGN KEY (item_id) REFERENCES menu_items(id))";
        String inventoryTransactionTable = "CREATE TABLE IF NOT EXISTS inventory_transactions (id INT AUTO_INCREMENT PRIMARY KEY, item_id INT, quantity INT, transaction_type VARCHAR(20), transaction_date DATETIME, FOREIGN KEY (item_id) REFERENCES menu_items(id))";
        String settingsTable = "CREATE TABLE IF NOT EXISTS settings (id INT PRIMARY KEY, cafe_name VARCHAR(100), gst_rate DECIMAL(5,2), currency VARCHAR(10))";
        Statement stmt = conn.createStatement();
        stmt.execute(userTable);
        stmt.execute(menuTable);
        stmt.execute(orderTable);
        stmt.execute(orderItemTable);
        stmt.execute(inventoryTransactionTable);
        stmt.execute(settingsTable);

        // Insert default admin user
        String adminQuery = "INSERT IGNORE INTO users (username, password, role) VALUES (?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(adminQuery);
        ps.setString(1, "admin");
        ps.setString(2, hashPassword("admin123"));
        ps.setString(3, "Admin");
        ps.executeUpdate();

        // Insert default settings
        String settingsQuery = "INSERT IGNORE INTO settings (id, cafe_name, gst_rate, currency) VALUES (1, ?, ?, ?)";
        PreparedStatement settingsPs = conn.prepareStatement(settingsQuery);
        settingsPs.setString(1, "Cafe Management System");
        settingsPs.setDouble(2, 0.05);
        settingsPs.setString(3, "Rs.");
        settingsPs.executeUpdate();
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing error: " + e.getMessage());
        }
    }

    // Initialize UI
    private void initializeUI() {
        frame = new JFrame("Cafe Management System");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        userManagement = new UserManagement(conn, frame);
        menuManagement = new MenuManagement(conn, frame);
        orderBilling = new OrderBilling(conn, frame);
        inventoryTracking = new InventoryTracking(conn, frame);
        reportAnalysis = new ReportAnalysis(conn, frame);
        JPanel loginPanel = userManagement.showLoginPanel(this::showMainInterface);
        frame.add(loginPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    // Show main interface after login
    private void showMainInterface() {
        frame.getContentPane().removeAll();
        JTabbedPane tabbedPane = new JTabbedPane();
        if ("Admin".equals(userManagement.getCurrentUserRole())) {
            tabbedPane.addTab("User Management", userManagement.createUserManagementPanel());
            tabbedPane.addTab("Inventory Tracking", inventoryTracking.createInventoryTrackingPanel());
            tabbedPane.addTab("Report Analysis", reportAnalysis.createReportAnalysisPanel());
        }
        tabbedPane.addTab("Menu Management", menuManagement.createMenuManagementPanel());
        tabbedPane.addTab("Order & Billing", orderBilling.createOrderBillingPanel());
        frame.add(tabbedPane, BorderLayout.CENTER);
        frame.revalidate();
        frame.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MainApp::new);
    }
}