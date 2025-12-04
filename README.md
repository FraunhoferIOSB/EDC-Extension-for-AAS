# EDC Extension for Asset Administration Shell [![Build Status](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions/workflows/gradle.yml/badge.svg)](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS/actions)

This [Eclipse Dataspace Connector (EDC)](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector) extension
provides an easy way to share
an [Asset Administration Shell (AAS)](https://www.plattform-i40.de/SiteGlobals/IP/Forms/Listen/Downloads/EN/Downloads_Formular.html?cl2Categories_TechnologieAnwendungsbereich_name=Verwaltungsschale)
model via the EDC.

## Version compatibility

| Specification                                                                                                                                                                                                                                                                | Version                                                                                                      |
|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| [Eclipse Dataspace Connector](https://github.com/eclipse-dataspaceconnector/DataSpaceConnector)                                                                                                                                                                              | v0.15.0                                                                                                      |
| [AAS - Details of the Asset Administration Shell - Part 1](https://www.plattform-i40.de/IP/Redaktion/EN/Downloads/Publikation/Details_of_the_Asset_Administration_Shell_Part1_V3.html)<br />The exchange of information between partners in the value chain of Industrie 4.0 | AAS Specs – Part 1 V3.0 (final)<br/>(based on [eclipse-aas4j/aas4j](https://github.com/eclipse-aas4j/aas4j)) |

## Repo Structure

The repository contains several material:

- `extensions`: Extensions to the Eclipse Dataspace Components (EDC)
- `launchers`: several connector configurations: control-planes, data-planes, standalone extension, tractus-x, ...
- `samples`: Example use cases, configuration files and resources
- `misc`: Supplemental files

<!-- ------------------Template Section --------------------------- -->

## Example Usage

For a data transfer example using two connectors communicating with the DSP protocol, check
the [samples README](samples/README.md).

## Functionality

AAS data can be shared over the EDC by linking an EDC Asset to the HTTP endpoint of the AAS element/submodel.
Additionally, contracts have to be defined for each element. In order to minimize configuration effort and prevent
errors, this extension is able to link running AAS into EDC Assets by connecting to AAS repositories or AAS registries.
Furthermore, this extension can also start an AAS repository by reading an AAS model (AASX / JSON). A default contract
can be chosen to be applied for all elements. For critical elements, additional contracts can be placed via API.
External changes to the model of an AAS are automatically synchronized by the extension, reducing management of metadata
significantly.

Additionally, a client extension providing API calls for aggregations of processes such as contract negotiation and data
transfer is available. The result is a one-click negotiation and data transfer, ideal for SME or individuals.

### Use Cases

Provide digital twin (AAS) data to business partners in Data Spaces. Data Providers can share their AAS Repository / AAS
Registry with other participants.

## Technical Details

Interfaces, dependencies and configuration variables are defined in the respective README files:

[AAS Extension](./extensions/control-plane/edc-extension4aas/README.md)

[Client Extension](./extensions/control-plane/client/README.md)

[Public API Management Extension](./extensions/control-plane/public-api-management/README.md)

[AAS Library Extension](./extensions/common/aas-lib/README.md)

[AAS Data-Plane Extension](./extensions/data-plane/data-plane-aas/README.md)

[EDC Connector Client Extension](./extensions/edc-connector-client/README.md)

## Terminology

| Term          | Description                                                                                                                                                                                                                                              |
|:--------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AAS           | Asset Administration Shell (see [AAS reading guide](https://industrialdigitaltwin.org/wp-content/uploads/2024/11/2024-11_IDTA_AAS-Reading-Guide.pdf) or [AAS specifications](https://industrialdigitaltwin.io/aas-specifications/index/home/index.html)) |
| FA³ST Service | [Open Source](https://github.com/FraunhoferIOSB/FAAAST-Service) java implementation of the AAS part 2                                                                                                                                                    |

## Roadmap

Features in development:

- Graphical interface to simplify providing and requesting
  AAS [&#x2713;](https://github.com/FraunhoferIOSB/EDC-Extension-for-AAS-Dashboard)  (update required)
- AAS data-plane for EDC [&#x2713;](extensions/data-plane/data-plane-aas)
- Docker Hub container deployment [&#x2713;](.github/workflows/push_to_main.yml)
- Support for AAS Registries: Share all AAS from AAS Registry &#x2713;
- Client DPP Viewer: Directly view the requested DPP (AAS format)
- Listen for updates on FA³ST message bus &#x2713;
