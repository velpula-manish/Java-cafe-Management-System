import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Date;

public class InventoryTracking {
    private Connection conn;
    private JFrame frame;

    // Constructor
    public InventoryTracking(Connection conn, JFrame frame) {
        this.conn = conn;
        this.frame = frame;
    }

    // Create inventory tracking panel
    public JPanel createInventoryTrackingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<String> itemCombo = new JComboBox<>();
        JTextField quantityField = new JTextField(5);
        JComboBox<String> transactionTypeCombo = new JComboBox<>(new String[]{"Add Stock", "Remove Stock"});
        JButton updateStockButton = new JButton("Update Stock");
        DefaultListModel<String> inventoryListModel = new DefaultListModel<>();
        JList<String> inventoryList = new JList<>(inventoryListModel);
        DefaultListModel<String> transactionListModel = new DefaultListModel<>();
        JList<String> transactionList = new JList<>(transactionListModel);

        // Load menu items into combo box
        refreshInventoryList(inventoryListModel, itemCombo);

        // Update stock
        updateStockButton.addActionListener(e -> {
            String selectedItem = (String) itemCombo.getSelectedItem();
            String quantityText = quantityField.getText();
            String transactionType = (String) transactionTypeCombo.getSelectedItem();
            try {
                int quantity = Integer.parseInt(quantityText);
                if (quantity <= 0) {
                    JOptionPane.showMessageDialog(frame, "Quantity must be positive");
                    return;
                }
                int itemId = Integer.parseInt(selectedItem.split(":")[0]);
                int quantityChange = transactionType.equals("Add Stock") ? quantity : -quantity;

                // Update stock in menu_items
                String updateQuery = "UPDATE menu_items SET stock_quantity = stock_quantity + ? WHERE id = ?";
                PreparedStatement ps = conn.prepareStatement(updateQuery);
                ps.setInt(1, quantityChange);
                ps.setInt(2, itemId);
                ps.executeUpdate();

                // Log transaction
                String transactionQuery = "INSERT INTO inventory_transactions (item_id, quantity, transaction_type, transaction_date) VALUES (?, ?, ?, ?)";
                PreparedStatement transactionPs = conn.prepareStatement(transactionQuery);
                transactionPs.setInt(1, itemId);
                transactionPs.setInt(2, quantity);
                transactionPs.setString(3, transactionType);
                transactionPs.setTimestamp(4, new Timestamp(new Date().getTime()));
                transactionPs.executeUpdate();

                // Refresh lists
                refreshInventoryList(inventoryListModel, itemCombo);
                refreshTransactionList(transactionListModel);
                quantityField.setText("");

                // Check for low stock
                String checkQuery = "SELECT stock_quantity FROM menu_items WHERE id = ?";
                PreparedStatement checkPs = conn.prepareStatement(checkQuery);
                checkPs.setInt(1, itemId);
                ResultSet rs = checkPs.executeQuery();
                if (rs.next() && rs.getInt("stock_quantity") < 10) {
                    JOptionPane.showMessageDialog(frame, "Low stock alert for item ID " + itemId + ": " + rs.getInt("stock_quantity") + " units remaining");
                }

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid quantity");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error updating stock: " + ex.getMessage());
            }
        });

        // Layout
        JPanel inputPanel = new JPanel(new GridLayout(4, 2, 5, 5));
        inputPanel.add(new JLabel("Select Item:"));
        inputPanel.add(itemCombo);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Transaction Type:"));
        inputPanel.add(transactionTypeCombo);
        inputPanel.add(new JLabel(""));
        inputPanel.add(updateStockButton);

        JPanel listPanel = new JPanel(new GridLayout(1, 2));
        listPanel.add(new JScrollPane(inventoryList));
        listPanel.add(new JScrollPane(transactionList));

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(listPanel, BorderLayout.CENTER);
        return panel;
    }

    // Refresh inventory list and combo box
    private void refreshInventoryList(DefaultListModel<String> inventoryListModel, JComboBox<String> itemCombo) {
        inventoryListModel.clear();
        itemCombo.removeAllItems();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, stock_quantity FROM menu_items");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int stock = rs.getInt("stock_quantity");
                String item = id + ": " + name + " (Stock: " + stock + ")";
                inventoryListModel.addElement(item);
                itemCombo.addItem(item);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading inventory: " + e.getMessage());
        }
    }

    // Refresh transaction list
    private void refreshTransactionList(DefaultListModel<String> transactionListModel) {
        transactionListModel.clear();
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT t.id, t.item_id, t.quantity, t.transaction_type, t.transaction_date, m.name " +
                    "FROM inventory_transactions t JOIN menu_items m ON t.item_id = m.id " +
                    "ORDER BY t.transaction_date DESC");
            while (rs.next()) {
                transactionListModel.addElement("ID: " + rs.getInt("id") + ", Item: " + rs.getString("name") +
                        ", Qty: " + rs.getInt("quantity") + ", Type: " + rs.getString("transaction_type") +
                        ", Date: " + rs.getTimestamp("transaction_date"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading transactions: " + e.getMessage());
        }
    }
}