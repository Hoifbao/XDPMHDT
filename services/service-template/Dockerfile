# Build từ THƯ MỤC GỐC repo (để có pom cha + module common):
#   docker build -f services/service-template/Dockerfile -t smart-heritage/service-template .
# Hoặc: docker compose -f Infra/docker-compose.yml --profile core up -d --build
# Khi copy cho service mới: thay "service-template" và cổng 8099.

# Tầng 1: build jar bằng Maven
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY . .
RUN mvn -q -B -pl services/service-template -am -DskipTests package

# Tầng 2: chỉ chứa JRE + jar (image nhỏ)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/services/service-template/target/service-template-*.jar app.jar
EXPOSE 8099
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
