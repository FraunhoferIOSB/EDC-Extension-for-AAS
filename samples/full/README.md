# Example Use Case

The example use case starts two connectors with the edc4aas extension and the client extension. The first connector
provides an AAS model and the second connector is an example consumer who wants to retrieve data from the provider.

## Requirements

Running this example requires an installation of JDK, a docker engine and docker compose.

The material for this example can be found here:

- `samples/configurations`: contains configuration files for the provider and consumer connector (`provider.properties`, `consumer.properties`)
- `samples/resources`: contains two example AAS files ('aas_v3_example_full_aas4j.json' and 'aas_model_v3.aasx') and an
  example config file ('exampleConfig.json') for the used AAS service (FA³ST). Additionally, there is a postman collection which
  can be used for requesting the consumer connector. Lastly, there is a JSON with accepted contract offers for the
  client connector and a keystore for HTTPS use-cases
- `samples/full/docker-compose.yml`: Running the example

## Getting Started

The starting location of all commands is the base repository directory.

You can directly start a provider and consumer EDC with the AAS extension built-in with docker-compose:

1. Go to the full-example directory: `cd samples/full`
2. `docker compose up`

### HTTP Requests

We support **Bruno** and Postman as collections for HTTP requests. Both collections can be found in `misc/http`

When using Docker, the pre-defined variables in the Collection for the provider should be changed from `localhost` to
the name of the containers, i.e. http://provider:8282/dsp instead of http://localhost:8282/dsp whenever the consumer
container wants to access the provider container. This is already reflected in the consumer call "Docker Automated
Negotation", which should work out of the box in our example docker compose setup. Docker occasionally renames the
containers to "example-provider-1" and "example-consumer-1". When a docker container wants to access AAS Services on the
host, the URL "host.docker.internal" should be used instead of the IP or localhost.

Additionally, new AASX or JSON model files should be placed in the resources folder beforehand, since the docker
container does not have access to your local files.

## Configuration

The EDC and its extensions can be configured with a `.properties` file. `samples/configurations` contains an example
configuration file for each launcher.


## Usage of the client's automated contract negotiation and data transfer interfaces

Build the EDC with the extensions.

```sh
cd /EDC-Extension-for-AAS
./gradlew shadowJar
```

Start the provider connector:

```sh
java -Dedc.fs.config=./example/configurations/provider-https.properties -jar ./example/build/libs/dataspace-connector.jar
```

Start the consumer connector:

```sh
java -Dedc.fs.config=./example/configurations/consumer.properties -jar ./example/build/libs/dataspace-connector.jar
```

### Starting the data transfer from provider to consumer

There is a `postman collection` located in `/misc/http/postman` that contains the necessary http requests. Complete the
following steps:

1. Call the provider's self-description (postman: EDC4AAS/Self Description or
   `http://localhost:8281/api/selfDescription` in your browser) and choose an element you want to transfer. Put its `id`
   as a variable in the postman collection's variables section ("current value"). Below is an example
   of a submodel element within a self-description (details omitted).

```json
{
  "id": "1987065423",
  "properties": {
    "some": "properties"
  },
  "idShort": "ExampleProperty",
  "description": [
    {
      "some": "descriptions"
    }
  ],
  "https://w3id.org/edc/v0.0.1/ns/id": "1987065423",
  "https://w3id.org/edc/v0.0.1/ns/contenttype": "application/json"
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

1. Execute the request `Client/Automated Negotiation`. The consumer connector will now try to negotiate a contract with
   the provider to get the data of the selected asset. Optionally, you can provide a target address via the request
   body, where the data should be sent. If you just want to view the data, change the request body to "none". This must
   be a JSON representation of an EDC DataAddress like for example an AasDataAddress:
    ```json
    {
       "type": "AasData",
       "properties": {
           "aasProvider": {
              "aasAccessUrl": "https://my-aas:1234/api/v3.0"
           },
           "path": "submodels/xyz/submodel-elements/a.b.c",
           "method": "PATCH",
           "header:x-api-key": "password"
       }
    }
    ```
   The above request body will PATCH the data into a running AAS at the consumer side.
2. If everything went right, the response should already be the data behind the `asset id` you selected (in case of
   DataAddress, the data should be at the target address).

### Option b: Separate requests

1. Execute the request `Client/1. Get dataset for asset`
   This will return the provider offer for this asset. Copy the full response for the next step.

2. Paste the response inside of request `Client/2. Initiate negotiation with contractOffer`'s body ("contractOffer"
   field, as can be seen in the screenshot) and
   execute said request.
   <img src="resources/tutorial-images/step-2">

3. If everything succeeded, request `2` returns an agreementID. Update the postman collection's "agreement-id" variable
   using the response value.

4. Execute request `3. Get data for agreement id and asset id`. If again everything went right, the response should be
   the data behind the previously selected asset. Here, you can also provide a DataAddress like in step 1 of the fully
   automated tutorial.
