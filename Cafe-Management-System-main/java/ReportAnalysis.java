import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.*;
import com.opencsv.CSVWriter;

public class ReportAnalysis {
    private Connection conn;
    private JFrame frame;
    private String cafeName = "Cafe Management System";
    private double gstRate = 0.05;
    private String currency = "Rs.";

    // Constructor
    public ReportAnalysis(Connection conn, JFrame frame) {
        this.conn = conn;
        this.frame = frame;
        loadSettings();
    }

    // Create report analysis panel
    public JPanel createReportAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input panel with GridBagLayout for better alignment
        JPanel inputPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Padding between components
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Sales report components
        JLabel reportTypeLabel = new JLabel("Report Type:");
        JComboBox<String> reportTypeCombo = new JComboBox<>(new String[]{"Daily", "Weekly"});
        JButton viewReportButton = new JButton("View Report");
        JButton exportCsvButton = new JButton("Export to CSV");
        JTextArea reportArea = new JTextArea(20, 50);
        reportArea.setEditable(false);

        // Cafe settings components
        JLabel cafeNameLabel = new JLabel("Cafe Name:");
        JTextField cafeNameField = new JTextField(cafeName, 15);
        JLabel gstLabel = new JLabel("GST Rate (%):");
        JTextField gstField = new JTextField(String.valueOf(gstRate * 100), 5);
        JLabel currencyLabel = new JLabel("Currency:");
        JTextField currencyField = new JTextField(currency, 5);
        JButton updateSettingsButton = new JButton("Update Settings");

        // View report
        viewReportButton.addActionListener(e -> {
            String reportType = (String) reportTypeCombo.getSelectedItem();
            generateSalesReport(reportType, reportArea);
        });

        // Export to CSV
        exportCsvButton.addActionListener(e -> {
            String reportType = (String) reportTypeCombo.getSelectedItem();
            exportToCSV(reportType);
        });

        // Update settings
        updateSettingsButton.addActionListener(e -> {
            try {
                cafeName = cafeNameField.getText();
                gstRate = Double.parseDouble(gstField.getText()) / 100.0;
                currency = currencyField.getText();

                String query = "UPDATE settings SET cafe_name = ?, gst_rate = ?, currency = ? WHERE id = 1";
                PreparedStatement ps = conn.prepareStatement(query);
                ps.setString(1, cafeName);
                ps.setDouble(2, gstRate);
                ps.setString(3, currency);
                int rows = ps.executeUpdate();
                if (rows == 0) {
                    String insertQuery = "INSERT INTO settings (id, cafe_name, gst_rate, currency) VALUES (1, ?, ?, ?)";
                    PreparedStatement insertPs = conn.prepareStatement(insertQuery);
                    insertPs.setString(1, cafeName);
                    insertPs.setDouble(2, gstRate);
                    insertPs.setString(3, currency);
                    insertPs.executeUpdate();
                }
                JOptionPane.showMessageDialog(frame, "Settings updated successfully");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(frame, "Invalid GST rate");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(frame, "Error updating settings: " + ex.getMessage());
            }
        });

        // Add components to input panel with GridBagLayout
        // Row 0: Report Type
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        inputPanel.add(reportTypeLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(reportTypeCombo, gbc);

        // Row 1: View Report button
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2; // Span both columns
        gbc.weightx = 0.0;
        inputPanel.add(viewReportButton, gbc);
        gbc.gridwidth = 1; // Reset gridwidth

        // Row 2: Export to CSV button
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        inputPanel.add(exportCsvButton, gbc);
        gbc.gridwidth = 1;

        // Row 3: Cafe Name
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0.0;
        inputPanel.add(cafeNameLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(cafeNameField, gbc);

        // Row 4: GST Rate
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0.0;
        inputPanel.add(gstLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(gstField, gbc);

        // Row 5: Currency
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0.0;
        inputPanel.add(currencyLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        inputPanel.add(currencyField, gbc);

        // Row 6: Update Settings button
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.weightx = 0.0;
        inputPanel.add(updateSettingsButton, gbc);

        // Add panels to main panel
        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        return panel;
    }

    // Load settings from database
    private void loadSettings() {
        try {
            String query = "SELECT cafe_name, gst_rate, currency FROM settings WHERE id = 1";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                cafeName = rs.getString("cafe_name");
                gstRate = rs.getDouble("gst_rate");
                currency = rs.getString("currency");
            }
        } catch (SQLException e) {
            // Ignore if table doesn't exist yet
        }
    }

    // Generate sales report
    private void generateSalesReport(String reportType, JTextArea reportArea) {
        try {
            String dateCondition = reportType.equals("Daily") ?
                    "DATE(order_date) = CURDATE()" :
                    "order_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
            String query = "SELECT o.id, o.order_type, o.table_number, o.subtotal, o.gst, o.discount, o.total, o.order_date, " +
                    "GROUP_CONCAT(oi.quantity, ' x ', m.name) as items " +
                    "FROM orders o " +
                    "JOIN order_items oi ON o.id = oi.order_id " +
                    "JOIN menu_items m ON oi.item_id = m.id " +
                    "WHERE " + dateCondition + " " +
                    "GROUP BY o.id";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            StringBuilder report = new StringBuilder();
            report.append("=== ").append(reportType).append(" Sales Report ===\n");
            report.append("Cafe: ").append(cafeName).append("\n");
            report.append("Generated on: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");

            double totalSales = 0.0;
            int orderCount = 0;

            while (rs.next()) {
                orderCount++;
                double total = rs.getDouble("total");
                totalSales += total;
                report.append("Order ID: ").append(rs.getInt("id")).append("\n");
                report.append("Type: ").append(rs.getString("order_type")).append("\n");
                report.append("Table: ").append(rs.getString("table_number")).append("\n");
                report.append("Items: ").append(rs.getString("items")).append("\n");
                report.append("Subtotal: ").append(currency).append(String.format("%.2f", rs.getDouble("subtotal"))).append("\n");
                report.append("GST: ").append(currency).append(String.format("%.2f", rs.getDouble("gst"))).append("\n");
                report.append("Discount: ").append(currency).append(String.format("%.2f", rs.getDouble("discount"))).append("\n");
                report.append("Total: ").append(currency).append(String.format("%.2f", total)).append("\n");
                report.append("Date: ").append(rs.getTimestamp("order_date")).append("\n\n");
            }

            report.append("Summary:\n");
            report.append("Total Orders: ").append(orderCount).append("\n");
            report.append("Total Sales: ").append(currency).append(String.format("%.2f", totalSales)).append("\n");

            reportArea.setText(report.toString());
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error generating report: " + e.getMessage());
        }
    }

    // Export report to CSV
    private void exportToCSV(String reportType) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(reportType.toLowerCase() + "_report_" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + ".csv"));
        if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(fileChooser.getSelectedFile()))) {
                // Write header
                csvWriter.writeNext(new String[]{"ID", "Type", "Table", "Items", "Subtotal", "GST", "Discount", "Total", "Date"});

                // Fetch data
                String dateCondition = reportType.equals("Daily") ?
                        "DATE(order_date) = CURDATE()" :
                        "order_date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
                String query = "SELECT o.id, o.order_type, o.table_number, o.subtotal, o.gst, o.discount, o.total, o.order_date, " +
                        "GROUP_CONCAT(oi.quantity, ' x ', m.name) as items " +
                        "FROM orders o " +
                        "JOIN order_items oi ON o.id = oi.order_id " +
                        "JOIN menu_items m ON oi.item_id = m.id " +
                        "WHERE " + dateCondition + " " +
                        "GROUP BY o.id";
                PreparedStatement ps = conn.prepareStatement(query);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    csvWriter.writeNext(new String[]{
                            String.valueOf(rs.getInt("id")),
                            rs.getString("order_type"),
                            rs.getString("table_number"),
                            rs.getString("items"),
                            currency + String.format("%.2f", rs.getDouble("subtotal")),
                            currency + String.format("%.2f", rs.getDouble("gst")),
                            currency + String.format("%.2f", rs.getDouble("discount")),
                            currency + String.format("%.2f", rs.getDouble("total")),
                            rs.getTimestamp("order_date").toString()
                    });
                }

                JOptionPane.showMessageDialog(frame, "CSV exported successfully");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Error exporting CSV: " + e.getMessage());
            }
        }
    }

    // Getter for GST rate
    public double getGstRate() {
        return gstRate;
    }
}