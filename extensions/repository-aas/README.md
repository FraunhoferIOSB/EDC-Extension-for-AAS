# AAS Repository Extension

This extension allows for management of self-hosted AAS Repository Instances (FA³ST Service) and remote AAS servers.
This includes starting/stopping services, getting AAS environments and descriptors, watching for changes in local FA³ST
servers.

## Configuration

No configuration variables.

## Interfaces

No outward-facing API.

### Dependencies

| Name                                          | Description                         |
|:----------------------------------------------|:------------------------------------|
| aas-lib (local)                               | Provides common AAS objects         |
| de.fraunhofer.iosb.ilt.faaast.client:core     | Communication w/ remote AAS servers |
| de.fraunhofer.iosb.ilt.faaast.service:starter | Starting FA³ST services             |
| org.eclipse.edc:boot-spi                      | EDC extension bootstrapping         |
| org.eclipse.edc:util-lib                      | getFreePort for FA³ST services      |
