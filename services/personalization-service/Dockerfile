# Build từ thư mục gốc repo:
# docker build -f services/personalization-service/Dockerfile -t smart-heritage/personalization-service .
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/personalization-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/personalization-service/target/personalization-service-*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
