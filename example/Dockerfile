# -buster is required to have apt available
FROM openjdk:17-jdk-slim

# by default curl is not available, so install it
RUN apt update && apt install curl -y

WORKDIR /app

# Copy jar
COPY ./build/libs/dataspace-connector.jar /app

ENV EDC_FS_CONFIG=$EDC_FS_CONFIG
ENV JAVA_OPTS=""

ENTRYPOINT ["java $JAVA_OPTS", "-jar", "dataspace-connector.jar"]
