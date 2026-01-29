FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
COPY itercraft_api/pom.xml .
RUN mvn dependency:go-offline --batch-mode
COPY itercraft_api/src ./src
RUN mvn clean package -DskipTests --batch-mode

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/itercraft-api-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
