FROM openjdk:8-jdk as BUILD

COPY . /ktor-full-app
WORKDIR /ktor-full-app

FROM openjdk:8-jre

COPY --from=BUILD /ktor-full-app/build/libs/com.qohat.ktor-full-app-all.jar /bin/runner/run.jar
WORKDIR /bin/runner

CMD ["java","-jar","run.jar"]