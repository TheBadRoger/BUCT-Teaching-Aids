FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build
COPY API/JavaAPI/pom.xml /build/pom.xml
COPY API/JavaAPI/src /build/src
RUN mvn -B -DskipTests -Pdocker clean package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=builder /build/target/japi-*.jar /app/japi.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app/japi.jar"]
