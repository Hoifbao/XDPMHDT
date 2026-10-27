# Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/auth-service/Dockerfile -t smart-heritage/auth-service .
# Hoặc: docker compose -f Infra/docker-compose.yml --profile core up -d --build
# Khi copy cho service mới: thay "auth-service" và cổng 8081.

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/auth-service -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/auth-service/target/auth-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
