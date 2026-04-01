FROM maven:3.9-eclipse-temurin-25 AS builder

WORKDIR /build
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates && update-ca-certificates && rm -rf /var/lib/apt/lists/*
COPY API/JavaAPI/pom.xml /build/pom.xml
COPY API/JavaAPI/src /build/src
RUN mvn -B -DskipTests -Pdocker clean package

FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=builder /build/target/japi-*.jar /app/japi.jar
EXPOSE 80
ENTRYPOINT ["java", "-jar", "/app/japi.jar"]
