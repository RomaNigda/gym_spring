FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mvn -B -q dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /build/target/lib ./lib
COPY --from=build /build/target/gym_spring-*.jar ./app.jar

ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "io.github.romanigda.gym_spring.GymSpringApplication"]
