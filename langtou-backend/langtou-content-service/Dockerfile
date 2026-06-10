FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY target/langtou-content-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
