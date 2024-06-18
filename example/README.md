# Example Use Case

The example use case starts two connectors with the edc4aas extension and the client extension. The first connector is a
provider of an AAS model and the second connector is an example consumer which wants to retrieve data from the provider.

The example has the following structure:

- `configurations`: contains configuration files for the provider and consumer connector
- `resources`: contains two example AAS files ('demoAAS.json' and 'FestoDemoAAS.json') and an example config file
  ('exampleConfig.json') for the used AAS service. Additionally, there is a postman collection which can be used for
  requesting the consumer connector.
- `build.gradle.kts`: build file for an EDC with the edc-extension4aas. Can be used as a launcher for a ready to use
  EDC. _Connectors can be started using the concept of "launchers", which are essentially compositions of Java modules
  defined as gradle build
  files._ - [EDC readme](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector#run-your-first-connector).
- `dataspaceconnector-configuration.properties`: Debugging and quick testing of changes via `./gradlew run --debug-jvm`
  command
- `docker-compose.yml`, `Dockerfile`: Docker files
- `README.md`: This README file

## Getting Started

First, the extension and the connector is built via the following command:

```sh
cd /EDC-Extension-for-AAS
./gradlew clean build
```

The following command starts an EDC connector with the _EDC AAS Extension_ with a configuration file:

```sh
java -Dedc.fs.config=./example/configurations/provider.properties -jar ./example/build/libs/dataspace-connector.jar
```

In case using **PowerShell** the `-D` parameter needs to be encapsulated with `"`:

```sh
java "-Dedc.fs.config=./example/configurations/provider.properties" -jar ./example/build/libs/dataspace-connector.jar
```

### Alternative: docker & docker-compose

After building the extension as seen above, a docker image can be built with

1. `cd ./example`
2. `docker build -t edc-aas-extension:latest .`
3. `mkdir workdir`
4. Create configuration under `./workdir/config.properties`
5. Add additional files under `./workdir` (Fitting to your paths in config.properties)
6. Run
   with `docker run -i -v $PWD/workdir:/workdir/ -e EDC_FS_CONFIG=/workdir/config.properties edc-extension4aas:latest`

This docker image can be run individually or **inside a docker-compose file**:

```sh
docker-compose up
```

## Configuration

The EDC and its extensions can be configured with a `.properties` file. In `example/resources/configurations` there are
a few examples of configurations.

For a list of config values provided by the extension, check the [Extension's README](../README.md#configurations).

A few basic EDC config values:

* `web.http.port`: Port and path for e.g., this extension's SelfDescription or Client API.
* `web.http.path`: The default path prefix under which endpoints are available.
* `web.http.protocol.port`: The port on which DSP endpoints are available.
* `web.http.protocol.path`: The path prefix under which DSP endpoints are available.
* `edc.dsp.callback.address`: Set this to the address at which another connector can reach your connector,
  as it is used as a callback address during the contract negotiation, where messages are exchanged
  asynchronously. If you change the protocol port/path, make sure to adjust the webhook address accordingly.
* `edc.api.auth.key`: Value of the header used for authentication when calling endpoints of the data management API.

An example configuration for a ready to use EDC with the _edc-extension4AAS_ and _dsp_:

```
edc.aas.exposeSelfDescription = true

web.http.port=9191
web.http.path=/api
web.http.management.port=9192
web.http.management.path=/management
web.http.protocol.port = 9292
web.http.protocol.path = /dsp
edc.dsp.callback.address=http://localhost:9292/dsp

# x-api-key
edc.api.auth.key=password
```

## Usage of the client's automated contract negotiation and data transfer interfaces

Build the EDC with the extensions.

```sh
cd /EDC-Extension-for-AAS
./gradlew clean build
```

Start the provider connector:

```sh
java -Dedc.fs.config=./example/configurations/provider.properties -jar ./example/build/libs/dataspace-connector.jar
```

Start the consumer connector:

```sh
java -Dedc.fs.config=./example/configurations/consumer.properties -jar ./example/build/libs/dataspace-connector.jar
```

Starting the data transfer from provider to consumer. There is a `postman collection` containing the necessary http
requests located in `/examples/resources`. Complete the following steps:

1. Call the provider's self-description on `http://localhost:8281/api/selfDescription` and choose an element you want
   to fetch. Put its `asset id` as a variable in the postman collection's variables section ("Current value").

### Fully automated

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
   body. This must be a JSON representation of an EDC DataAddress like for example an HttpDataAddress:
    ```json
    {
       "type": "HttpData",
       "properties": {
           "https://w3id.org/edc/v0.0.1/ns/baseUrl": "https://my-aas:1234/api/v3.0/submodels/xyz/submodel-elements/a.b.c",
           "method": "PATCH",
           "header:x-api-key": "password"
       }
    }
    ```

2. If everything went right, the response should already be the data behind the `asset id` you selected (in case of
   DataAddress, the data should be at the target address).

### Separate requests

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

## HTTPS connector configuration files

Even though 'provider-https.properties' and 'consumer-https.properties' are valid connectors on their own, they cannot
communicate with each other since they both use self-signed TLS certificates. Those certificates are not allowed when
communicating with another connector, so data transfer will not be possible.

## Debugging the extension

With the gradle goal `run` and the additional flag `--debug-jvm`, the extension can be debugged while running within the
example launcher (or any other launcher). A configuration file can be provided by creating a file named
_dataspaceconnector-configuration.properties_ in the same folder as the _build.gradle.kts_ file of the launcher (e.g.,
_./example/dataspaceconnector-configuration.properties_). After executing the gradle run goal, attach to the debugger
with your IDE. The following snippet is an example _launch.json_ file to attach to a debugger in vscode running on port
5005:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "Attach to debugger at port 5005",
      "request": "attach",
      "hostName": "localhost",
      "port": "5005"
    }
  ]
}
```

### Debugging TL;DR

1. `./gradlew :example:run --debug-jvm`

2. Attach to debugger at exposed port (see console logs)