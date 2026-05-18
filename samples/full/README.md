# Example Use Case

The example use case starts two connectors with the edc4aas extension and the client extension. The first connector
provides an AAS model and the second connector is an example consumer who wants to retrieve data from the provider.

## Requirements

Running this example requires an installation of JDK, a docker engine and docker compose.

The material for this example can be found here:

- `samples/config`: contains configuration files for different connectors (needed here: `provider.properties`,
  `consumer.properties`)
- `samples/resources`: contains two example AAS files ('aas_v3_example_full_aas4j.json' and 'aas_model_v3.aasx') and an
  example config file ('exampleConfig.json') for the used AAS service (FA³ST). Additionally, there is a bruno
  collection which can be used for requesting the consumer connector under `misc/http`. Lastly, there is a JSON with
  accepted contract offers for the client connector and a keystore for HTTPS use-cases
- `samples/full/docker-compose.yml`: Docker compose file for running the example

## Getting Started

The starting location of all commands is the base repository directory.

You can directly start a provider and consumer EDC with the AAS extension built-in with docker-compose:

1. Build the project's docker images: `./gradlew dockerize`
2. Go to the full-example directory: `cd samples/full`
3. `docker compose up`

### HTTP Requests

There is a **Bruno** collection for HTTP requests in `misc/http`.

When using Docker, the pre-defined variables in the Collection for the provider should be changed from `localhost` to
the name of the containers, i.e. http://provider:8282/dsp instead of http://localhost:8282/dsp whenever the consumer
container wants to access the provider container. When a docker container wants to access AAS Services on the
host, the URL "host.docker.internal" should be used instead of the IP or localhost.

Additionally, new AASX or JSON model files should be placed in the resources folder beforehand, since the docker
container does not have access to your local files.

## Configuration

The EDC and its extensions can be configured with a `.properties` file. `samples/config` contains an example
configuration file for each launcher.

## Usage of the client's automated contract negotiation and data transfer interfaces

Build the EDC with the extensions.

```sh
./gradlew shadowJar
```

Start the provider connector:

```sh
java -Dedc.fs.config=./samples/config/provider-https.properties -jar ./launchers/provider/build/libs/provider.jar
```

Start the consumer connector:

```sh
java -Dedc.fs.config=./samples/config/consumer.properties -jar ./launchers/consumer/build/libs/consumer.jar
```

### Starting the data transfer from provider to consumer

Using the bruno collection in `misc/http`, complete the following steps:

1. Call the provider's self-description (bruno: `AAS Extension/discovery/SelfDescription` or
   `http://localhost:8281/api/selfDescription` in your browser) and choose an element you want to transfer. Put its
   EDC Asset Identifier (see below) as a variable in the bruno collection's variables section ("asset-id").
   Below is an example of a registered submodel within a self-description.

```json
{
  "submodels": [
    {
      "modelType": "Submodel",
      "extensions": [
        {
          "name": "https://w3id.org/edc/v0.0.1/ns/id",
          "value": "-585713211"
          <--
          EDC
          Asset
          ID
        }
      ],
      "id": "https://example.com/ids/sm/5213_1120_8022_9305",
      "idShort": "ControlComponentInstance"
    }
  ]
}
```

### Option a: Fully automated

__Important__:

- If the (consumer's) config value `edc.client.acceptAllProviderOffers` is set to `true`: This command will fetch
  a provider policy for the selected asset and accept it as is. If no policies exist for this asset, no negotiation will
  take place.

- If the (consumer's) config value `edc.client.acceptAllProviderOffers` is set to `false` (default): This command
  will request the provider's offered policy for the asset and check it against its own accepted policyDefinitions by
  comparing the permissions, prohibitions and obligations of both the provider's policyDefinition and the ones in the
  policyDefinitionStore. The assetID or other IDs must not be equal for the policyDefinitions to match. Initially, this
  store is empty and can be filled up by the request `Client/Add accepted policyDefinition` (tip: with the
  request `EDC API/GET catalog`, policyDefinitions for all assets of the provider can be viewed and added to the
  consumer connector).

1. Execute the request `Client/automated/Negotiation+Transfer`. The consumer connector will now try to negotiate a
   contract with
   the provider to get the data of the selected asset. Optionally, you can provide a target address via the request
   body, where the data should be sent. If you just want to view the data, change the request body to "none". This must
   be a JSON representation of an EDC DataAddress like for example an AasDataAddress:
    ```json
    {
      "type": "AasData",
      "properties": {
        "https://w3id.org/edc/v0.0.1/ns/baseUrl": "https://localhost:8080/api/v3.0",
        "https://admin-shell.io/aas/3/0/reference": "[ModelRef](Submodel)https://example.com/ids/sm/5213_1120_8022_9305",
        "https://w3id.org/edc/v0.0.1/ns/method": "PUT",
        "header:x-api-key": "password"
      }
    }
    ```
   The above request body will update the submodel with the provider's data.
2. If everything went right, the response should already be the data behind the `asset id` you selected (in case of
   DataAddress, the data should be at the target address).

### Option b: Separate requests

1. Go to the bruno folder `Client/manual`. It contains the necessary requests to manually transfer data.

2. Execute the request `1. Get Offer for Asset`
   This will return the provider offer for this asset. It will be used in the next request automatically.

3. Execute the request `2. Initiate Negotiation using ContractOffer`.
   This will negotiate a contract agreement with the provider EDC and return the agreement id used in the next step.

4. Execute request `3. Get Data`. The client extension will then get the AAS data behind the EDC Asset. It will be
   returned as the HTTP response.
