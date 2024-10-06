# Data-plane for AAS

Out-of-the-box, the EDC data-plane supports HTTPS calls for the AAS API, but some use-cases (processing AAS data /
calling operations) require setting up endpoint services or configuration of the transfer-requests. For consumers, this
is not an easy task and hinders the cross-company usage of AAS.
Additionally, many AAS services use self-signed company certificates, which are not supported by the EDC data-plane.

This custom data-plane adds a configuration option to accept self-signed company certificates, enabling data sharing for
those AAS services.

Default values:

`edc.dataplane.aas.acceptAllSelfSignedCertificates=False`

`edc.dataplane.aas.acceptOwnSelfSignedCertificates=True`

## Roadmap

Features in development:

- Support AAS operations
- Use external AAS properties as input variables in AAS operation
- Store external AAS submodel-properties in own AAS submodel-properties
- AAS Events

