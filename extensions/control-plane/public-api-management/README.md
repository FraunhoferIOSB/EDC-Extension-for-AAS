# Public API Management Extension

This extension manages public API endpoints. For example, unauthenticated endpoints or temporary authenticate endpoints
can be published.

## Configuration

No configuration variables.

## Interfaces

No outward-facing API.

### Dependencies

| Name                        | Description               |
|:----------------------------|:--------------------------|
| org.eclipse.edc:auth-spi    | EDC Authentication SPI    |
| org.eclipse.edc:jersey-core | WebService                |
| org.eclipse.edc:jetty-core  | WebService                |
| org.eclipse.edc:api-core    | ApiAuthenticationRegistry |
