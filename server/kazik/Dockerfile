FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle gradle
COPY kazik/build.gradle.kts kazik/
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :kazik:help

COPY kazik/src kazik/src
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew --no-daemon :kazik:bootJar \
    && cp kazik/build/libs/*.jar /workspace/service.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 kazik
COPY --from=build /workspace/service.jar /app/service.jar
USER kazik
ENTRYPOINT ["java", "-jar", "/app/service.jar"]
