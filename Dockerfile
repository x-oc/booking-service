FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /app/build/libs/app.jar ./app.jar

EXPOSE 28800
ENTRYPOINT ["java", "-jar", "app.jar"]
