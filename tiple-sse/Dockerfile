FROM openjdk:21-jdk-slim

WORKDIR /app

# 직접 JAR 파일을 복사하는 방식 (사전 빌드된 경우)
COPY build/libs/*.jar app.jar

# 또는 Maven/Gradle로 빌드하는 방식
# COPY . .
# RUN ./gradlew build --no-daemon -x test

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8084

# JVM optimization options
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+UseContainerSupport"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]