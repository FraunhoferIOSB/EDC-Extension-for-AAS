# EDC Connector Client Extension

Installing a new EDC control-plane component requires adding it to the EDC's build file, building the resulting runtime
and deploying it. A deployment of EDC components next to a running control-plane is not possible if the component to be
deployed needs access to the control-plane's APIs (e.g., management API: Assets, Contracts, Policies, ...).

The edc connector client establishes a connection between an EDC runtime and a remote EDC control-plane instance's
management API by implementing the necessary interfaces AssetIndex, ContractDefinitionStore and PolicyDefinitionStore.
It provides these implementation via the @Inject mechanism of the EDC.

Currently, the following services are supported by this extension:

- AssetIndex
- PolicyDefinitionStore
- ContractDefinitionStore

The extension also supports the following authentication mechanisms to talk to the control-plane:

- No auth
- api key
- api key via vault

## Configuration

| Key (edc.controlplane.) | Value Type | Description                                                |
|:------------------------|:-----------|:-----------------------------------------------------------|
| management.url          | URL        | Remote control plane full management API URL               |
| auth.key                | String     | Remote control-plane API Key                               |
| auth.key.alias          | String     | Remote control-plane vault secret alias for authentication |

## Interfaces

No outward-facing API.

### Dependencies

| Name                                    | Description                                                  |
|:----------------------------------------|:-------------------------------------------------------------|
| aas-lib (local)                         | Provides common objects for AAS data-plane and AAS extension |
| org.eclipse.edc:asset-spi               | EDC Asset                                                    |
| org.eclipse.edc:contract-spi            | EDC Policy/Contract                                          |
| org.eclipse.edc:transform-lib           | Transformers to / from JSON                                  |
| org.eclipse.edc:control-plane-transform | Transformers to / from JSON                                  |
| org.eclipse.edc:json-ld                 | JSON-LD expansion / compaction                               |
| org.eclipse.edc:runtime-core            | Core services                                                |
| org.eclipse.edc:connector-core          | Core services                                                |

## Q&A

Why not use an EDC client?

- this extension directly uses EDC components such as the EdcHttpClient
    - no external data model
    - extensions using only the aforementioned stores as a means to communicate with the control-plane can be outsourced
      to their own EDC runtime without many changes necessary
- Easy to install
    - Add the dependency to your EDC runtime's build file
    - Configure mgmt url and authentication