# feedback-service (Tường - Sprint 4). Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/feedback-service/Dockerfile -t smart-heritage/feedback-service .

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/feedback-service -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/feedback-service/target/feedback-service-*.jar app.jar
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
