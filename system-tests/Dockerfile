FROM eclipse-temurin:17-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    jq \
    gettext-base \
    curl \
    ca-certificates \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /work
