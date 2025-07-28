FROM openjdk:17-jdk-slim

# by default curl is not available  so install it
RUN apt update && apt install curl -y

WORKDIR /app

# Copy jar
COPY build/libs/dataspace-connector.jar /app

ENV EDC_FS_CONFIG=/config/config.properties

ENTRYPOINT ["java", "-jar", "dataspace-connector.jar"]
