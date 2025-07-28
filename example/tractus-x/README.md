# Configuration files for the tractus-x EDC fork

### Important: For data transfer to work, the connectors need to be correctly configured w.r.t. vault and oauth2-service. This example only demonstrates compatibility of the EDC-Extension-for-AAS with tractus-x EDC. 

## Structure:

- `configurations`: Configuration files for the consumer and provider connectors from the example.
    - When deploying data- and control-plane separately, a configuration file is shared across both deployments.
    - Identity and access management is mocked throughout this example.
- `control-plane`: extension + tractus-x EDC control-plane build file + dockerfile.
- `data-plane`: data-plane + aas-data-plane build file + dockerfile.

## Build the connectors:

From the repository's root directory:

```sh
./gradlew clean example:tractus-x:control-plane:build example:tractus-x:data-plane:build -x test
```

Build the docker image and run the docker-compose file:

```sh
cd example/tractus-x/control-plane
docker build -t tractus-x-control-plane .

cd ../data-plane
docker build -t tractus-x-data-plane .

cd ..
docker compose up -d
```

The self-description can be found at:
```shell
curl -i http://localhost:8281/api/selfDescription
```