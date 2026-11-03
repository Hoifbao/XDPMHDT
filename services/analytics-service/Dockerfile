# Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/analytics-service/Dockerfile -t smart-heritage/analytics-service .
# Hoặc: docker compose -f Infra/docker-compose.yml --profile core up -d --build

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/analytics-service -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/analytics-service/target/analytics-service-*.jar app.jar
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
