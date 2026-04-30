FROM openjdk:17

WORKDIR /app

COPY . .

RUN javac -cp .:mysql-connector.jar FoodDeliveryServer.java

CMD ["sh", "run.sh"]