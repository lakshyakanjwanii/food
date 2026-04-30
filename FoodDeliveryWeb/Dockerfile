FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac -cp .:mysql-connector.jar FoodDeliveryServer.java

CMD ["java", "-cp", ".:mysql-connector.jar", "FoodDeliveryServer"]