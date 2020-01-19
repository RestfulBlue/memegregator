# Base Alpine Linux based image with OpenJDK JRE only
FROM openjdk:8-jre-alpine

# copy application WAR (with libraries inside)
COPY target/meme-query-server-0.0.1.jar /meme-query-server.jar
COPY target/logback-spring.xml /logback-spring.xml
# specify default command
CMD ["/usr/bin/java", "-Xmx512m","-jar", "-Dspring.profiles.active=test", "/meme-query-server.jar" ,"--logging.config=/logback-spring.xml"]