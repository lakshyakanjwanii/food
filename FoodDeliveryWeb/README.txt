# SwiftBite — Food Delivery Web App (Java + JDBC + MySQL)

## HOW TO RUN (Mac)

### Step 1 — Import the database
  mysql -u root -p -e "DROP DATABASE IF EXISTS food_delivery; CREATE DATABASE food_delivery;"
  mysql -u root -p food_delivery < schema.sql

### Step 2 — Put mysql-connector.jar in this folder
  (Same jar you used for the desktop app — rename to mysql-connector.jar)

### Step 3 — Run
  chmod +x run.sh
  ./run.sh

### Step 4 — Open browser
  Go to: http://localhost:8080

## FEATURES
- Dashboard with live counts
- Customers  — Add / View / Update / Delete
- Restaurants — Add / View / Update / Delete
- Orders     — Add / View / Update / Delete (with status badge)
- Partners   — Add / View / Update / Delete

## TECH
- Pure Java (no Spring, no Maven)
- Java built-in HTTP server (com.sun.net.httpserver)
- JDBC + MySQL
- HTML/CSS served directly from Java
