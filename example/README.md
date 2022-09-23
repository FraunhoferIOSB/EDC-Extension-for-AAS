# Example Use Case

The example use case starts two connectors with the edc-extension4aas. The first connector is a provider of an AAS model and the second connector is an example consumer which wants to retrieve data from the provider.

The example has following structure:
- `configurations`: contains configuration files for the provider and consumer connector
- `resources`: contains two example AAS files and an example config file for the used AAS service. Additionally, there is a postman collection which can be used for requesting the consumer connector.
- `build.gradle.kts`: build file for an EDC with the edc-extension4aas. Can be used as a launcher for an ready to use EDC. _Connectors can be started using the concept of "launchers", which are essentially compositions of Java modules defined as gradle build files._ - [EDC readme](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector#run-your-first-connector).

## Getting Started

First, the extension and the connector is built via the following command:

```sh
cd /edc-aas-extension
./gradlew clean build
```

The following command starts an EDC connector with the _EDC AAS Extension_ with a configration file:

```sh
java -Dedc.fs.config=./example/resources/configurations/provider.properties -jar ./example/build/libs/dataspace-connector.jar
```

In case using **PowerShell** the `-D` parameter needs to be encapsulated with `"`: 
```sh
java "-Dedc.fs.config=./example/resources/configurations/provider.properties" -jar ./example/build/libs/dataspace-connector.jar
```

## Configuration

The EDC and its extensions can be configured with a `.properties` file. In `example/resources/configurations` there are few examples of configurations. 

For a list of config values provided by the extension, check the [Extension's README](../README.md#configurations). 

A few basic EDC config values:

* `web.http.port`: The EDC's port defaults to 8181. Adjust this property to run it on a different port.
* `web.http.path`: The default path prefix under which endpoints are available.
* `web.http.ids.port`: The port on which IDS endpoints (currently only the Multipart endpoint) are available.
* `web.http.ids.path`: The path prefix under which IDS endpoints (currently only the Multipart endpoint) are available.
* `ids.webhook.address`: Set this to the address at which another connector can reach your connector, 
  as it is used as a callback address during the contract negotiation, where messages are exchanged 
  asynchronously. If you change the IDS API port, make sure to adjust the webhook address accordingly.
* `edc.api.auth.key`: Value of the header used for authentication when calling 
  endpoints of the data management API.

An example configuration for an ready to use EDC with the _edc-extension4AAS_ and _ids_:
```
# extension4AAS
edc.idsaasapp.logPrefix = EDC-AAS-Extension
edc.idsaasapp.localAASModelPath = ./example/resources/FestoDemoAAS.json
edc.idsaasapp.localAASServicePort = 8080
edc.idsaasapp.syncPeriod = 100

# EDC 
web.http.port=8181
web.http.path=/api
web.http.data.port=8182
web.http.data.path=/api/v1/data
web.http.ids.port = 8282
web.http.ids.path = /api/v1/ids
edc.transfer.functions.enabled.protocols = http
edc.hostname = localhost
edc.api.auth.key=password

# IDS specific (see EDC/data-protocols/ids/ids-core/README.md)
edc.ids.id = urn:connector:provider
edc.ids.title = "Eclipse Dataspace Connector with AAS support"
edc.ids.description = "EDC with extension IDS-AAS-App enabled"
edc.ids.maintainer = iosb
edc.ids.curator = https://example.com
edc.ids.endpoint = https://example.com
edc.ids.security.profile = base
edc.ids.catalog.id = urn:catalog:default

ids.webhook.address=http://localhost:8282
```

## Running the Example

Build the EDC with the extensions.
```sh
cd /edc-aas-extension
./gradlew clean build
```

Start the provider connector:

```sh
java -Dedc.fs.config=./example/resources/configurations/provider.properties -jar ./example/build/libs/dataspace-connector.jar
```

Open another console and start the consumer connector:

```sh
java -Dedc.fs.config=./example/resources/configurations/consumer.properties -jar ./example/build/libs/dataspace-connector.jar
```

Starting the data transfer from provider to consumer. There is a `postman collection` containing all necessary http request for data transfer in this extensions repository located in `/examples/resources`. Do following steps:

1. Call the provider's self description on `http://localhost:8181/api/selfDescription`, and choose an element you want to fetch. Put its `asset id` and `contract id` as variables in the postman collection.
   
2. Send the contract offer to the EDC Provider. Execute request 1 of the data transfer folder.
You should get a contract negotiation ID (consumer's negotiation ID) like this: `"id":"<negotiation-id>"}`. Put this `<negotiation-id>` as negotiation id variable in the postman collection.

3. With this `<negotiation-id>`, query the consumer connector about the state of the negotiation. Execute request 2 of the data transfer folder.
It should return:

```json
{
  "contractAgreementId": "<agreement-id>",
  "counterPartyAddress": "http://localhost:8282/api/v1/ids/data",
  "errorDetail": null,
  "id": "ac6e1c97-13d6-41ff-8b79-1029d7f094bb",
  "protocol": "ids-multipart",
  "state": "CONFIRMED",
  "type": "CONSUMER"
}
```

4. Put the `<agreement-id>` in the postman collection's agreement-id variable.
Execute request 3 of the Data Transfer folder. The provider connector should now send the data, and in the consumer edc's logs there should be confirmation of the transfer. The consumer prints as console output the fetched AAS element.
