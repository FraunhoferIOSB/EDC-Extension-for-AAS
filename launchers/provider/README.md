# Default

This is the default EDC-Extension-for-AAS launcher. It contains all the components needed to launch an EDC instance (control-plane, AAS extension, data-plane).  It contains the following extensions:

- `edc-extension4aas`: The AAS extension to publish your AAS in the data space.
- `client`: Convenience for negotiation and data transfer.
- `controlplane-base-bom`: Base control-plane dependencies.
- `vault-hashicorp`: Enables vault lookups for secrets
- `auth-configuration`: Enables configuring authentication values (e.g., x-api-key value)
- `iam-mock`: Since no DCP is set up for this launcher, the DefaultParticipantIdExtractionFunction is used from the mock IAM. This will get the participant from the claim key `client_id`.