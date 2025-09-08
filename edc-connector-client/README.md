# edc connector client extension

## What

The edc connector client establishes a connection between an EDC runtime and a remote EDC control-plane instance's
management API by implementing the necessary interfaces AssetIndex, ContractDefinitionStore and PolicyDefinitionStore.
It provides these implementation via the @Inject mechanism of the EDC.

Currently, the following services are supported by this extension:

- AssetIndex
- PolicyDefinitionStore
- ContractDefinitionStore

The extension also supports the following authentication mechanisms for the control-plane:

- No auth
- api key

## Configuration

The configuration variables are self-explanatory. Either the following configuration values need to be supplied:

- `edc.controlplane.`
    - `protocol`;
    - `hostname`;
    - `management.port`;
    - `management.path`;
    - `version.port`;
    - `version.path`.

They can all be found in the control-plane's configuration.

**Alternatively**, the control-plane's management API url can be supplied:

- `edc.controlplane.management.url`.

If the control-plane requires authentication, the following configuration needs to be added:

- `edc.controlplane.apiKey`

## Why

Installing a new EDC control-plane component requires adding it to the EDC's build file, building the resulting runtime
and
deploying it. A deployment of EDC components next to a running control-plane is not possible if the component to be
deployed needs access to the control-plane's APIs (e.g., management API: Assets, Contracts, Policies, ...).

This extension allows to establish a remote connection to the control-plane without modifications to the component to be
deployed by providing implementations of management services.

## Q&A

Why not use an EDC client?

- this extension directly uses EDC components such as the EdcHttpClient
    - no external data model
- Easy to install
    - Add the dependency to your EDC runtime's build file
    - Add configuration values for the remote control-plane
        - control-plane management API URL
        - or: supply the control-plane base URL, the management API endpoint and the version endpoint to let the client
          extension discover the latest API version
