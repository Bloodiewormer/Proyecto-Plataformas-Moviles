FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle/wrapper/ gradle/wrapper/
COPY src/main/ src/main/
RUN sed -i 's/\r$//' gradlew \
    && cp src/main/resources/application-cloud.properties src/main/resources/application.properties \
    && sh ./gradlew --no-daemon bootJar \
    && cp "$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar')" /tmp/glifo.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app
COPY --from=build /tmp/glifo.jar /app/glifo.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/glifo.jar", "--spring.profiles.active=cloud"]
