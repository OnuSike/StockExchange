FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . /workspace
RUN mvn -q -pl exchange-server -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/exchange-server/target/*.jar /app/exchange-server.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/exchange-server.jar"]
