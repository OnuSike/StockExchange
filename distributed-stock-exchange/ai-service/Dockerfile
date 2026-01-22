FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . /workspace
RUN mvn -q -pl ai-service -am -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/ai-service/target/*.jar /app/ai-service.jar
EXPOSE 8081
ENTRYPOINT ["java","-jar","/app/ai-service.jar"]
