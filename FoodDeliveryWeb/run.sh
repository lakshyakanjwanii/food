#!/bin/bash
echo "Starting SwiftBite Food Delivery Web App..."
echo ""
echo "Compiling..."
javac -cp mysql-connector.jar FoodDeliveryServer.java
if [ $? -ne 0 ]; then
  echo "❌ Compilation failed!"
  exit 1
fi
echo "✅ Compiled!"
echo ""
echo "🌐 Open your browser and go to: http://localhost:8080"
echo "   Press Ctrl+C to stop the server"
echo ""
java -cp .:mysql-connector.jar FoodDeliveryServer
