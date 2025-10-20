# Command for building this image:
# $ docker build -t <launcher-name>:<version> launchers --build-arg PROJECT=<launcher-name>
# Example:
# $ docker build -t consumer:latest launchers --build-arg PROJECT=consumer
FROM openjdk:17-alpine

ARG PROJECT

ENV EDC_FS_CONFIG=$EDC_FS_CONFIG

COPY ./build/libs/${PROJECT}.jar app.jar

ENTRYPOINT ["java", \
     "-Djava.security.egd=file:/dev/urandom", \
     "-jar", \
     "app.jar"]
