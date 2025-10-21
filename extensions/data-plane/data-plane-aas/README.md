# AAS Data-Plane Extension

Out-of-the-box, the EDC data-plane supports HTTPS calls for the AAS API, but some use-cases (processing AAS data/calling
operations) require setting up endpoint services or configuration of PUSH transfer-requests. For consumers, this is not
an easy task and hinders the cross-company usage of AAS. Additionally, many AAS services use self-signed company
certificates, which are not supported by the EDC data-plane. This custom data-plane adds a configuration option to
accept self-signed company certificates, enabling data sharing for those AAS services.

This extension provides extended functionality to the default EDC HTTP data-plane implementation:

- Calling AAS operations, modifying AAS state with PUSH transfers
- Supporting self-signed certificates (configurable)

## Configuration

| Key (edc.dataplane.aas.)            | Value Type | Description                                                                                                                                   |
|:------------------------------------|:-----------|:----------------------------------------------------------------------------------------------------------------------------------------------|
| acceptOwnSelfSignedCertificates     | boolean    | Accept self-signed certificates from own AAS services <u>if the configured EDC is a data provider.</u>                                        |
| acceptForeignSelfSignedCertificates | boolean    | Accept self-signed certificates from ALL AAS services <u>if the configured EDC shall send data to services with self-signed certificates.</u> |

## Interfaces

No additional outward-facing API.

### Dependencies

| Name                           | Description                                   |
|:-------------------------------|:----------------------------------------------|
| aas-lib (local)                | Provides common AAS objects                   |
| org.eclipse.edc:data-plane-spi | Data-plane functionality                      |
| org.eclipse.edc:http-lib       | OkHttp3 Fields + EdcHttpClient implementation |

## Roadmap

Features in development:

- Use external AAS properties as input variables in AAS operation
- Store external AAS submodel-properties in own AAS submodel-properties
- AAS Events

