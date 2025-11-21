# AAS Data Plane

This is a data plane launcher featuring the AAS data plane and the HTTP data plane. It contains the following
extensions:

- `data-plane-aas`: AAS data plane enabling PUSH data transfers with AAS operation invocation and AAS state modification
- `dataplane-base-bom`: Base data plane dependencies
- `configuration-filesystem`: Enables configuration for the control-plane
- `auth-tokenbased`: Enables token-based authentication (e.g., x-api-key)
- `vault-hashicorp`: Enables vault lookups for secrets
- `api-core`: Provides ApiAuthenticationRegistry to enable authentication
- `auth-configuration`: Enables configuring authentication values (e.g., x-api-key value)