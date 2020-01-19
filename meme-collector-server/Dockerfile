# Base Alpine Linux based image with OpenJDK JRE only
FROM openjdk:8-jre-alpine
# copy application WAR (with libraries inside)
COPY target/meme-collector-server-0.0.1.jar /meme-collector-server.jar
# specify default command
CMD ["/usr/bin/java", "-Xmx512m", "-jar", "-Dspring.profiles.active=test", "/meme-collector-server.jar"]