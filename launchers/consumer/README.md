# Client

This is a control-plane featuring the client extension. It contains the following extensions:

- `client`: Convenience for negotiation and data transfer.
- `configuration-filesystem`: Enables configuration for the control-plane
- `management-api-configuration`: Provides authentication for management API
- `auth-tokenbased`: Enables token-based authentication (e.g., x-api-key)
- `vault-hashicorp`: Enables vault lookups for secrets
- `iam-mock`: DefaultParticipantIdExtraction TODO
- `api-core`: Provides ApiAuthenticationRegistry to enable authentication
- `auth-configuration`: Enables configuring authentication values (e.g., x-api-key value)