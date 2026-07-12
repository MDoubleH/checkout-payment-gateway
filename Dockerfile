FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre-jammy

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system gateway \
    && useradd --system --gid gateway gateway
WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER gateway
EXPOSE 8090

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
