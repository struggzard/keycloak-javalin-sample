FROM openjdk:11

COPY target/keycloak-javalin-sample-1.0-SNAPSHOT.jar /usr/bin/sampleApp.jar
WORKDIR /usr/bin

CMD nohup java -jar /usr/bin/sampleApp.jar