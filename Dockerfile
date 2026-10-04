FROM maven:3.9.16-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# Tests are a pre-deploy gate, run separately with the existing browser tooling.
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN groupadd --system finbridge && useradd --system --gid finbridge --home /app finbridge
WORKDIR /app
COPY --from=build --chown=finbridge:finbridge /build/target/personal-finance-cross-border-agent-1.0.0.jar /app/finbridge.jar
USER finbridge
EXPOSE 8080
# Secrets are runtime environment variables, never build arguments or image contents.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "/app/finbridge.jar", "--spring.profiles.active=hosting"]
