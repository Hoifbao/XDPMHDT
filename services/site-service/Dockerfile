# Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/site-service/Dockerfile -t smart-heritage/site-service .
# Hoặc: docker compose -f Infra/docker-compose.yml --profile core up -d --build
# site-service (Ân), cổng 8082.

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/site-service -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/site-service/target/site-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
