FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY target/*.jar app.jar

EXPOSE 8081

ENV TZ=Asia/Shanghai
ENTRYPOINT ["java", "-jar", "app.jar"]
