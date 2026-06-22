# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY langtou-common/pom.xml langtou-common/
COPY langtou-ai-service/pom.xml langtou-ai-service/
COPY langtou-common/src langtou-common/src
COPY langtou-ai-service/src langtou-ai-service/src
RUN mvn -pl langtou-common,langtou-ai-service -am clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/langtou-ai-service/target/langtou-ai-service-*.jar app.jar

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8087/actuator/health || exit 1

# ============ JVM Options ============
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Djava.security.egd=file:/dev/./urandom -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -Xss256k -XX:+UseStringDeduplication -XX:-UseBiasedLocking"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]