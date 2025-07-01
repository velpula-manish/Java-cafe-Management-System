CREATE DATABASE IF NOT EXISTS cafe_db;
SHOW DATABASES;
USE cafe_db;
SHOW TABLES;
SELECT * FROM cafe_db.users;
SELECT * FROM cafe_db.menu_items;
SELECT * FROM users; -- Should include 'admin'
SELECT * FROM menu_items; -- Should include items like 'Coffee'
SELECT * FROM orders;
SELECT * FROM order_items;
ALTER TABLE menu_items ADD COLUMN stock_quantity INT DEFAULT 100;
DESCRIBE menu_items;
CREATE TABLE IF NOT EXISTS inventory_transactions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT,
    quantity INT,
    transaction_type VARCHAR(20),
    transaction_date DATETIME,
    FOREIGN KEY (item_id) REFERENCES menu_items(id)
);
SHOW TABLES;
UPDATE menu_items SET stock_quantity = 100 WHERE stock_quantity IS NULL;

SELECT * FROM menu_items; -- Check stock_quantity
SELECT * FROM inventory_transactions; -- Check transactions