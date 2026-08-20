FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S ems && adduser -S ems -G ems
COPY --from=build /build/target/employee-management-system.jar app.jar
USER ems
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
