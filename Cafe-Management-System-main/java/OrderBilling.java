import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrderBilling {
    private Connection conn;
    private JFrame frame;

    // Constructor
    public OrderBilling(Connection conn, JFrame frame) {
        this.conn = conn;
        this.frame = frame;
    }

    // Create order and billing panel
    public JPanel createOrderBillingPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JComboBox<String> orderTypeCombo = new JComboBox<>(new String[]{"Table", "Takeaway"});
        JTextField tableNumberField = new JTextField(5);
        JComboBox<String> itemCombo = new JComboBox<>();
        JTextField quantityField = new JTextField(5);
        JButton addItemButton = new JButton("Add Item to Order");
        DefaultListModel<String> orderListModel = new DefaultListModel<>();
        JList<String> orderList = new JList<>(orderListModel);
        JTextField discountField = new JTextField(5);
        JButton generateBillButton = new JButton("Generate Bill");
        JTextArea billArea = new JTextArea(10, 30);
        billArea.setEditable(false);

        // Load menu items into combo box
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, name, price FROM menu_items WHERE available = TRUE");
            while (rs.next()) {
                itemCombo.addItem(rs.getInt("id") + ": " + rs.getString("name") + " (Rs." + rs.getDouble("price") + ")");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading menu items: " + e.getMessage());
        }

        // Add item to order
        addItemButton.addActionListener(e -> {
            String selectedItem = (String) itemCombo.getSelectedItem();
            String quantityText = quantityField.getText();
            try {
                int quantity = Integer.parseInt(quantityText);
                if (quantity > 0) {
                    int itemId = Integer.parseInt(selectedItem.split(":")[0]);
                    // Check stock
                    String stockQuery = "SELECT stock_quantity FROM menu_items WHERE id = ?";
                    PreparedStatement stockPs = conn.prepareStatement(stockQuery);
                    stockPs.setInt(1, itemId);
                    ResultSet stockRs = stockPs.executeQuery();
                    if (stockRs.next()) {
                        int stock = stockRs.getInt("stock_quantity");
                        if (stock < quantity) {
                            JOptionPane.showMessageDialog(frame, "Insufficient stock for item ID " + itemId + ": " + stock + " units available");
                            return;
                        }
                    }
                    String query = "SELECT name, price FROM menu_items WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setInt(1, itemId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String name = rs.getString("name");
                        double price = rs.getDouble("price");
                        orderListModel.addElement(itemId + ": " + name + " x" + quantity + " (Rs." + (price * quantity) + ")");
                        quantityField.setText("");
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Quantity must be positive");
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid quantity");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error adding item: " + ex.getMessage());
            }
        });

        // Generate bill
        generateBillButton.addActionListener(e -> {
            String orderType = (String) orderTypeCombo.getSelectedItem();
            String tableNumber = orderType.equals("Table") ? tableNumberField.getText() : "N/A";
            String discountText = discountField.getText();
            try {
                double discount = discountText.isEmpty() ? 0.0 : Double.parseDouble(discountText);
                double subtotal = 0.0;
                List<String> orderItems = new ArrayList<>();

                // Calculate subtotal and check stock
                for (int i = 0; i < orderListModel.size(); i++) {
                    String item = orderListModel.get(i);
                    int itemId = Integer.parseInt(item.split(":")[0]);
                    int quantity = Integer.parseInt(item.split("x")[1].split(" ")[0]);
                    String query = "SELECT price, stock_quantity FROM menu_items WHERE id = ?";
                    PreparedStatement ps = conn.prepareStatement(query);
                    ps.setInt(1, itemId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        double price = rs.getDouble("price");
                        int stock = rs.getInt("stock_quantity");
                        if (stock < quantity) {
                            JOptionPane.showMessageDialog(frame, "Insufficient stock for item ID " + itemId + ": " + stock + " units available");
                            return;
                        }
                        subtotal += price * quantity;
                        orderItems.add(item);
                    }
                }

                // Calculate GST (5%) and total
                double gst = subtotal * 0.05;
                double total = subtotal + gst - discount;

                // Save order to database
                String orderQuery = "INSERT INTO orders (order_type, table_number, subtotal, gst, discount, total, order_date) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(orderQuery, Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, orderType);
                ps.setString(2, tableNumber);
                ps.setDouble(3, subtotal);
                ps.setDouble(4, gst);
                ps.setDouble(5, discount);
                ps.setDouble(6, total);
                ps.setTimestamp(7, new Timestamp(new Date().getTime()));
                ps.executeUpdate();

                // Get order ID
                ResultSet rs = ps.getGeneratedKeys();
                int orderId = rs.next() ? rs.getInt(1) : -1;

                // Save order items and update stock
                for (String item : orderItems) {
                    int itemId = Integer.parseInt(item.split(":")[0]);
                    int quantity = Integer.parseInt(item.split("x")[1].split(" ")[0]);
                    String itemQuery = "INSERT INTO order_items (order_id, item_id, quantity) VALUES (?, ?, ?)";
                    PreparedStatement itemPs = conn.prepareStatement(itemQuery);
                    itemPs.setInt(1, orderId);
                    itemPs.setInt(2, itemId);
                    itemPs.setInt(3, quantity);
                    itemPs.executeUpdate();

                    // Update stock
                    String stockQuery = "UPDATE menu_items SET stock_quantity = stock_quantity - ? WHERE id = ?";
                    PreparedStatement stockPs = conn.prepareStatement(stockQuery);
                    stockPs.setInt(1, quantity);
                    stockPs.setInt(2, itemId);
                    stockPs.executeUpdate();

                    // Log inventory transaction
                    String transactionQuery = "INSERT INTO inventory_transactions (item_id, quantity, transaction_type, transaction_date) VALUES (?, ?, ?, ?)";
                    PreparedStatement transactionPs = conn.prepareStatement(transactionQuery);
                    transactionPs.setInt(1, itemId);
                    transactionPs.setInt(2, quantity);
                    transactionPs.setString(3, "Order Deduction");
                    transactionPs.setTimestamp(4, new Timestamp(new Date().getTime()));
                    transactionPs.executeUpdate();
                }

                // Display bill
                StringBuilder bill = new StringBuilder();
                bill.append("=== Cafe Bill ===\n");
                bill.append("Order ID: ").append(orderId).append("\n");
                bill.append("Date: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n");
                bill.append("Type: ").append(orderType).append("\n");
                if (orderType.equals("Table")) {
                    bill.append("Table: ").append(tableNumber).append("\n");
                }
                bill.append("\nItems:\n");
                for (String item : orderItems) {
                    bill.append(item).append("\n");
                }
                bill.append("\nSubtotal: Rs.").append(String.format("%.2f", subtotal)).append("\n");
                bill.append("GST (5%): Rs.").append(String.format("%.2f", gst)).append("\n");
                bill.append("Discount: Rs.").append(String.format("%.2f", discount)).append("\n");
                bill.append("Total: Rs.").append(String.format("%.2f", total)).append("\n");
                billArea.setText(bill.toString());

                // Clear order
                orderListModel.clear();
                discountField.setText("");
                tableNumberField.setText("");

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid discount");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error generating bill: " + ex.getMessage());
            }
        });

        JPanel inputPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        inputPanel.add(new JLabel("Order Type:"));
        inputPanel.add(orderTypeCombo);
        inputPanel.add(new JLabel("Table Number:"));
        inputPanel.add(tableNumberField);
        inputPanel.add(new JLabel("Select Item:"));
        inputPanel.add(itemCombo);
        inputPanel.add(new JLabel("Quantity:"));
        inputPanel.add(quantityField);
        inputPanel.add(new JLabel("Discount (Rs.):"));
        inputPanel.add(discountField);
        inputPanel.add(addItemButton);
        inputPanel.add(generateBillButton);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(orderList), BorderLayout.CENTER);
        panel.add(new JScrollPane(billArea), BorderLayout.SOUTH);
        return panel;
    }
}