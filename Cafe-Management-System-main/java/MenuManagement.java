import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class MenuManagement {
    private Connection conn;
    private JFrame frame;

    // Constructor
    public MenuManagement(Connection conn, JFrame frame) {
        this.conn = conn;
        this.frame = frame;
    }

    // Create menu management panel
    public JPanel createMenuManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTextField nameField = new JTextField(10);
        JTextField priceField = new JTextField(10);
        JComboBox<String> categoryCombo = new JComboBox<>(new String[]{"Snacks", "Beverages", "Desserts"});
        JCheckBox availableCheck = new JCheckBox("Available", true);
        JButton addButton = new JButton("Add Item");
        JButton updateButton = new JButton("Update Item");
        DefaultListModel<String> menuListModel = new DefaultListModel<>();
        JList<String> menuList = new JList<>(menuListModel);
        JButton deleteButton = new JButton("Delete Item");
        JButton loadCsvButton = new JButton("Load from CSV");

// Load menu items
        refreshMenuList(menuListModel);

// Add menu item
        addButton.addActionListener(e -> {
            String name = nameField.getText();
            String priceText = priceField.getText();
            String category = (String) categoryCombo.getSelectedItem();
            boolean available = availableCheck.isSelected();
            try {
                double price = Double.parseDouble(priceText);
                String query = "INSERT INTO menu_items (name, category, price, available) VALUES (?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, name);
                ps.setString(2, category);
                ps.setDouble(3, price);
                ps.setBoolean(4, available);
                ps.executeUpdate();
                refreshMenuList(menuListModel);
                nameField.setText("");
                priceField.setText("");
                JOptionPane.showMessageDialog(frame, "Menu item added");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid price format");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error adding item: " + ex.getMessage());
            }
        });

// Update menu item
        updateButton.addActionListener(e -> {
            String selected = menuList.getSelectedValue();
            if (selected != null) {
                int id = Integer.parseInt(selected.split(":")[0]);
                String name = nameField.getText();
                String priceText = priceField.getText();
                String category = (String) categoryCombo.getSelectedItem();
                boolean available = availableCheck.isSelected();
                try {
                    double price = Double.parseDouble(priceText);
                    String query = "UPDATE menu_items SET name = ?, category = ?, price = ?, available = ? WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setString(1, name);
                    ps.setString(2, category);
                    ps.setDouble(3, price);
                    ps.setBoolean(4, available);
                    ps.setInt(5, id);
                    ps.executeUpdate();
                    refreshMenuList(menuListModel);
                    nameField.setText("");
                    priceField.setText("");
                    JOptionPane.showMessageDialog(frame, "Menu item updated");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid price format");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error updating item: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(frame, "Select an item to update");
            }
        });

// Delete menu item
        deleteButton.addActionListener(e -> {
            String selected = menuList.getSelectedValue();
            if (selected != null) {
                int id = Integer.parseInt(selected.split(":")[0]);
                try {
                    String query = "DELETE FROM menu_items WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    refreshMenuList(menuListModel);
                    JOptionPane.showMessageDialog(frame, "Menu item deleted");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(frame, "Error deleting item: " + ex.getMessage());
                }
            }
        });

// Load menu from CSV
        loadCsvButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
                try (BufferedReader br = new BufferedReader(new FileReader(fileChooser.getSelectedFile()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] data = line.split(",");
                        if (data.length == 4) {
                            String name = data[0];
                            String category = data[1];
                            double price = Double.parseDouble(data[2]);
                            boolean available = Boolean.parseBoolean(data[3]);
                            String query = "INSERT INTO menu_items (name, category, price, available) VALUES (?, ?, ?, ?)";
                            PreparedStatement ps = conn.prepareStatement(query);
                            ps.setString(1, name);
                            ps.setString(2, category);
                            ps.setDouble(3, price);
                            ps.setBoolean(4, available);
                            ps.executeUpdate();
                        }
                    }
                    refreshMenuList(menuListModel);
                    JOptionPane.showMessageDialog(frame, "Menu loaded from CSV");
                } catch (IOException | SQLException | NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Error loading CSV: " + ex.getMessage());
                }
            }
        });

// Select item to populate fields
        menuList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = menuList.getSelectedValue();
                if (selected != null) {
                    try {
                        int id = Integer.parseInt(selected.split(":")[0]);
                        String query = "SELECT name, category, price, available FROM menu_items WHERE id = ?";
                        PreparedStatement ps = conn.prepareStatement(query);
                        ps.setInt(1, id);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            nameField.setText(rs.getString("name"));
                            categoryCombo.setSelectedItem(rs.getString("category"));
                            priceField.setText(String.valueOf(rs.getDouble("price")));
                            availableCheck.setSelected(rs.getBoolean("available"));
                        }
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(frame, "Error loading item: " + ex.getMessage());
                    }
                }
            }
        });

        JPanel inputPanel = new JPanel(new GridLayout(6, 2));
        inputPanel.add(new JLabel("Item Name:"));
        inputPanel.add(nameField);
        inputPanel.add(new JLabel("Price:"));
        inputPanel.add(priceField);
        inputPanel.add(new JLabel("Category:"));
        inputPanel.add(categoryCombo);
        inputPanel.add(new JLabel("Available:"));
        inputPanel.add(availableCheck);
        inputPanel.add(addButton);
        inputPanel.add(updateButton);
        inputPanel.add(deleteButton);
        inputPanel.add(loadCsvButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(menuList), BorderLayout.CENTER);
        return panel;
    }

    // Refresh menu list
    private void refreshMenuList(DefaultListModel<String> menuListModel) {
        menuListModel.clear();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, category, price, available FROM menu_items");
            while (rs.next()) {
                menuListModel.addElement(rs.getInt("id") + ": " + rs.getString("name") + " (" + rs.getString("category") + ") - Rs." + rs.getDouble("price") + (rs.getBoolean("available") ? "" : " [Unavailable]"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading menu: " + e.getMessage());
        }
    }
}