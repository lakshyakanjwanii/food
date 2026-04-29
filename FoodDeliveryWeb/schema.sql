CREATE DATABASE IF NOT EXISTS food_delivery;
USE food_delivery;

CREATE TABLE IF NOT EXISTS User (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(15) NOT NULL UNIQUE,
    role ENUM('customer', 'delivery_partner') NOT NULL
);

CREATE TABLE IF NOT EXISTS Customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    address VARCHAR(255) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Delivery_Partner (
    partner_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    vehicle_type VARCHAR(50) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES User(user_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS Restaurant (
    restaurant_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    location VARCHAR(255) NOT NULL,
    rating DECIMAL(2,1) DEFAULT 0.0
);

CREATE TABLE IF NOT EXISTS `Order` (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    customer_id INT NOT NULL,
    restaurant_id INT NOT NULL,
    partner_id INT,
    address VARCHAR(255) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    FOREIGN KEY (customer_id) REFERENCES Customer(customer_id),
    FOREIGN KEY (restaurant_id) REFERENCES Restaurant(restaurant_id),
    FOREIGN KEY (partner_id) REFERENCES Delivery_Partner(partner_id)
);

CREATE TABLE IF NOT EXISTS Order_Item (
    item_no INT AUTO_INCREMENT PRIMARY KEY,
    order_id INT NOT NULL,
    qty INT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES `Order`(order_id) ON DELETE CASCADE
);

INSERT INTO Restaurant (name, location, rating) VALUES
('Spice Garden', 'Andheri West', 4.5),
('The Burger Lab', 'Bandra', 4.2),
('Sushi Station', 'Juhu', 4.7);
