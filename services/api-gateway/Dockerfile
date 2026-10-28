# Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/api-gateway/Dockerfile -t smart-heritage/api-gateway .
# Hoặc: docker compose -f Infra/docker-compose.yml --profile core up -d --build
# Khi copy cho service mới: thay "api-gateway" và cổng 8080.

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/api-gateway -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
