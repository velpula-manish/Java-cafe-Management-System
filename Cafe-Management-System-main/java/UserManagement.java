import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class UserManagement {
    private Connection conn;
    private String currentUserRole;
    private JFrame frame;

    // Constructor
    public UserManagement(Connection conn, JFrame frame) {
        this.conn = conn;
        this.frame = frame;
    }

    // Show login panel
    public JPanel showLoginPanel(Runnable onLoginSuccess) {
        JPanel loginPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        JButton loginButton = new JButton("Login");

        loginPanel.add(new JLabel("Username:"));
        loginPanel.add(usernameField);
        loginPanel.add(new JLabel("Password:"));
        loginPanel.add(passwordField);
        loginPanel.add(new JLabel(""));
        loginPanel.add(loginButton);

        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                String query = "SELECT role FROM users WHERE username = ? AND password = ?";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, username);
                ps.setString(2, hashPassword(password));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentUserRole = rs.getString("role");
                    onLoginSuccess.run();
                } else {
                    JOptionPane.showMessageDialog(frame, "Invalid credentials");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Login error: " + ex.getMessage());
            }
        });

        return loginPanel;
    }

    // Create user management panel (Admin only)
    public JPanel createUserManagementPanel() {
        if (!"Admin".equals(currentUserRole)) {
            return new JPanel();
        }

        JPanel panel = new JPanel(new BorderLayout());
        JTextField usernameField = new JTextField(10);
        JPasswordField passwordField = new JPasswordField(10);
        JComboBox<String> roleCombo = new JComboBox<>(new String[]{"Admin", "Staff"});
        JButton addButton = new JButton("Add User");
        DefaultListModel<String> userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        JButton deleteButton = new JButton("Delete User");

        // Load users
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT username, role FROM users");
            while (rs.next()) {
                userListModel.addElement(rs.getString("username") + " (" + rs.getString("role") + ")");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading users: " + e.getMessage());
        }

        // Add user
        addButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String role = (String) roleCombo.getSelectedItem();
            try {
                String query = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, username);
                ps.setString(2, hashPassword(password));
                ps.setString(3, role);
                ps.executeUpdate();
                userListModel.addElement(username + " (" + role + ")");
                usernameField.setText("");
                passwordField.setText("");
                JOptionPane.showMessageDialog(frame, "User added");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error adding user: " + ex.getMessage());
            }
        });

        // Delete user
        deleteButton.addActionListener(e -> {
            String selected = userList.getSelectedValue();
            if (selected != null) {
                String username = selected.split(" ")[0];
                try {
                    String query = "DELETE FROM users WHERE username = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, username);
                    ps.executeUpdate();
                    userListModel.removeElement(selected);
                    JOptionPane.showMessageDialog(frame, "User deleted");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error deleting user: " + ex.getMessage());
                }
            }
        });

        JPanel inputPanel = new JPanel(new GridLayout(4, 2));
        inputPanel.add(new JLabel("Username:"));
        inputPanel.add(usernameField);
        inputPanel.add(new JLabel("Password:"));
        inputPanel.add(passwordField);
        inputPanel.add(new JLabel("Role:"));
        inputPanel.add(roleCombo);
        inputPanel.add(addButton);
        inputPanel.add(deleteButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(userList), BorderLayout.CENTER);
        return panel;
    }

    // Hash password using SHA-256
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing error: " + e.getMessage());
        }
    }

    // Get current user role
    public String getCurrentUserRole() {
        return currentUserRole;
    }
}
