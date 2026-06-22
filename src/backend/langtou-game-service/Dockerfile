FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build
COPY pom.xml .
COPY langtou-common/pom.xml langtou-common/
COPY langtou-game-service/pom.xml langtou-game-service/
COPY langtou-common/src langtou-common/src
COPY langtou-game-service/src langtou-game-service/src
RUN mvn -pl langtou-common,langtou-game-service -am clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/langtou-game-service/target/langtou-game-service-*.jar app.jar

EXPOSE 8088

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8088/actuator/health || exit 1

ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -Djava.security.egd=file:/dev/./urandom -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=512m -Xss256k -XX:+UseStringDeduplication -XX:-UseBiasedLocking"

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
