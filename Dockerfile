# Stage 1: Build
FROM docker.m.daocloud.io/library/maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY src ./src

RUN mvn package -DskipTests -q

# Stage 2: Runtime
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

RUN addgroup -g 1000 appgroup && adduser -u 1000 -G appgroup -D appuser \
    && mkdir -p /app/logs /app/uploads && chown -R appuser:appgroup /app

EXPOSE 8081

ENV TZ=Asia/Shanghai
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -Xlog:gc*:file=/app/logs/gc.log:time,level,tags:filecount=10,filesize=10M"

USER appuser
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
