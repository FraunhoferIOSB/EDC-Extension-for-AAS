FROM eclipse-temurin:21-jdk

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    jq \
    gettext-base \
    curl \
    ca-certificates \
  && rm -rf /var/lib/apt/lists/*

RUN . /etc/os-release \
  && install -m 0755 -d /etc/apt/keyrings \
  && curl -fsSL "https://download.docker.com/linux/${ID}/gpg" \
       -o /etc/apt/keyrings/docker.asc \
  && chmod a+r /etc/apt/keyrings/docker.asc \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/${ID} ${VERSION_CODENAME} stable" \
       > /etc/apt/sources.list.d/docker.list \
  && apt-get update \
  && apt-get install -y --no-install-recommends \
       docker-ce-cli \
       docker-compose-plugin \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /work
