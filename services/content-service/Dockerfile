# Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/content-service/Dockerfile -t smart-heritage/content-service .
# Hoặc: docker compose -f Infra/docker-compose.yml --profile core up -d --build
# content-service (Xuân Anh), cổng 8083.

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/content-service -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/content-service/target/content-service-*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
