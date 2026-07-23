# Hercules Sample Environment

Hercules is a comprehensive sample environment demonstrating the EDC Extension for AAS (Asset Administration Shell). It provides a complete end-to-end setup with multiple EDC connectors, identity services, and a FA³ST service for managing digital twins and their asset information.

## Overview

The Hercules sample implements a multi-participant EDC (Eclipse Dataspace Connectors) environment with:
- **Control Plane**: Primary EDC connector with management and protocol endpoints
- **Standalone**: EDC extension for AAS integration with FA³ST service
- **Data Plane**: Secure data transfer endpoint
- **Identity Hub**: DID-based identity management for participants
- **Issuer Service**: Verifiable credential issuance (MembershipCredentials)
- **FA³ST Registry**: Digital Twin Registry for AAS asset descriptors
- **PostgreSQL**: Shared database for all services
- **HashiCorp Vault**: Secrets management for credentials and keys

## Architecture

<img src="./architecture.drawio.svg">


**Note**: All services communicate via Docker's internal DNS on the default network.
- Services may be exposed to external host for demonstration (e.g., vault) or debug.
- Every EDC service is debuggable, expose port 5005 and use remote debugging in your IDE
- Internal communication uses container hostnames (e.g., `http://control-plane:8081`)

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)
- [Bruno](https://www.usebruno.com/) (optional, for API testing)

## Quick Start

1. **Copy environment file** (optional, defaults will be used):
   ```bash
   cd samples/hercules
   cp .env.example .env
   ```

2. **Build the standalone extension** (if modified):
   ```bash
   cd samples/hercules/standalone-hercules
   ./gradlew shadowJar
   cd ../..
   docker compose build standalone
   ```

3. **Start all services**:
   ```bash
   docker compose up -d
   ```

4. **Initialize participants and credentials** (in correct order):
   ```bash
   # Wait for services to be healthy (takes ~30-60 seconds)
   docker compose ps
   
   # Initialize issuer first
   docker compose exec toolbox ./init_issuer.sh
   
   # Then initialize participants
   docker compose exec toolbox ./init_participant.sh
   ```

4. **Access services**:
   - Control Plane Management: `http://localhost:8081/management`
   - Standalone AAS Extension: `http://localhost:8080`
   - FA³ST Registry: `http://localhost:8090/shell-descriptors`
   - Identity Hub: `http://localhost:10100`
   - PostgreSQL: `localhost:5432`
   - Vault: `http://localhost:8200`

## Service Details

### Control Plane (Participant 1)
- **Image**: `ghcr.io/factory-x-contributions/edc-controlplane-postgresql-hashicorp-vault`
- **Ports**:
  - `8081`: Management API (`/management`)
  - `8084`: DSP Protocol (`/api/v1/dsp`)
  - `8085`: Catalog API (`/catalog`)
- **Purpose**: Primary EDC connector managing assets, policies, and negotiations
- **Participant ID**: `did:web:identity-hub%3A10100:participant1`

### Standalone (Extension)
- **Image**: `standalone-hercules:latest` (custom build)
- **Ports**:
  - `8080`: FA³ST Service
  - `8181`: EDC Extension API (internal)
  - `5005`: Debug port
- **Purpose**: Integrates FA³ST AAS service with EDC for digital twin management
- **Configuration**: `config/edc/extension/configuration.properties`
- **Network Access**: Connects to `control-plane`, `dtr`, `data-plane`, `identity-hub`, `issuer-service`, and `vault` via Docker network
- **Participant**: Uses `participant1` identity (`did:web:identity-hub%3A10100:participant1`)

### Data Plane
- **Image**: `ghcr.io/factory-x-contributions/edc-dataplane-hashicorp-vault`
- **Ports**:
  - `9500`: Public data endpoint (`/public`)
- **Purpose**: Secure data transfer endpoint with signature-based access
- **Keys**: Generated and stored in HashiCorp Vault during initialization
- **Network Access**: Connects to `control-plane` and `vault` via Docker network; accessed by `standalone` for data transfer
- **Participant**: Uses `participant1` identity

### Identity Hub
- **Image**: `tractusx/identityhub:v0.3.2`
- **Ports**:
  - `7171`: Well-known API
  - `13131`: Credentials API
  - `10100`: DID endpoint
  - `15151`: Identity API
  - `9292`: STS (Security Token Service)
- **Purpose**: DID-based identity management for participants
- **Configuration**: `config/edc/identityhub/participant1.properties`

### Issuer Service
- **Image**: `ghcr.io/factory-x-contributions/fx-id-hub-charts/issuerservice`
- **Ports**:
  - `9999`: Status List
  - `13132`: Issuance API
  - `15152`: Issuer Admin API
- **Purpose**: Issues verifiable credentials (MembershipCredentials)
- **Configuration**: `config/edc/issuerservice/configuration.properties`

### FA³ST Registry (DTR)
- **Image**: `fraunhoferiosb/faaast-registry:1.2.0-SNAPSHOT`
- **Ports**:
  - `8090`: Shell Descriptors endpoint
- **Purpose**: Digital Twin Registry storing AAS descriptions
- **Configuration**: `config/aas/dtr.properties`
- **Initial Model**: `config/aas/model.aasx`

### HashiCorp Vault
- **Image**: `hashicorp/vault`
- **Ports**:
  - `8200`: Vault API
- **Mode**: Dev mode (not for production)
- **Token**: Configured via `VAULT_TOKEN` in `.env`
- **Stores**:
  - Database credentials
  - API keys for control plane and extension
  - STS client secrets
  - Data plane public/private keys

### PostgreSQL
- **Image**: `postgres:16.4-alpine`
- **Ports**:
  - `5432`: PostgreSQL (exposed via docker-compose network)
- **Databases**: `edc`, `identity_hub`, `issuer_service`, `participant2`
- **Initialization**: `config/postgres/pg_init.sql`

### Toolbox
- **Image**: Custom `debian:bookworm-slim` (builds from `toolbox/`)
- **Purpose**: Utility container with tools for debugging and initialization
- **Includes**: `curl`, `jq`, `vim`, `iputils-ping`, `ca-certificates`

### Participant2
- **Image**: `ghcr.io/factory-x-contributions/edc-controlplane-postgresql-hashicorp-vault`
- **Purpose**: Secondary EDC connector for testing cross-participant communication
- **Participant ID**: `did:web:identity-hub%3A10100:participant2`
- **Network Access**: Connects to `postgres2` and `vault` via Docker network; accessible from all other containers
- **Note**: No host ports exposed, accessed only from other containers (e.g., `curl http://participant2:8081/management`)
- **Configuration**: `config/edc/controlplane/participant2.properties`

## Directory Structure

```
samples/hercules/
├── .env.example                  # Environment variable template
├── docker-compose.yaml           # Main compose definition
├── bruno/                        #Bruno API test collections
│   ├── opencollection.yml       # Shared collection config
│   ├── control-plane/           # Control plane API tests
│   ├── discovery/               # Discovery protocol tests
│   └── data-access/             # Data access tests
├── config/                       # Configuration files
│   ├── aas/                     # FA³ST/AAS configuration
│   │   ├── model.aasx          # Default AAS model
│   │   ├── fa3st-config.json   # FA³ST service config
│   │   └── dtr.properties      # Digital Twin Registry config
│   ├── edc/                     # EDC connector configurations
│   │   ├── controlplane/       # Control plane configs
│   │   │   ├── participant1.properties
│   │   │   └── participant2.properties
│   │   ├── extension/          # AAS extension config
│   │   │   ├── configuration.properties
│   │   │   └── default_policy.json
│   │   ├── dataplane/          # Data plane config
│   │   │   └── configuration.properties
│   │   ├── identityhub/        # Identity hub config
│   │   │   └── participant1.properties
│   │   ├── issuerservice/      # Issuer service config
│   │   │   ├── configuration.properties
│   │   │   └── credentials/
│   │   │       └── membership.json
│   │   ├── logging.properties
│   │   ├── log4j2.xml
│   │   ├── opentelemetry.properties
│   │   └── vault/              # Vault initialization
│   ├── postgres/               # Database initialization
│   │   └── pg_init.sql
│   └── toolbox/                # Toolbox initialization
│       ├── data/               #Participant/issuer data
│       │   ├── participant1.json
│       │   ├── participant2.json
│       │   └── issuer.json
│       ├── templates/          #JSON templates for API calls
│       │   ├── participant.json
│       │   ├── holder.json
│       │   ├── attestation.json
│       │   ├── credential.json
│       │   └── credential_request.json
│       ├── init_participant.sh #Participant initialization script
│       └── init_issuer.sh      #Issuer initialization script
├── standalone-hercules/        # Standalone application
│   ├── build.gradle.kts        # Gradle build configuration
│   └── build/                  # Built JAR (generated)
└── toolbox/                    # Toolbox container definition
    ├── Dockerfile
    └── entrypoint.sh
```

## Network Architecture

Docker Compose creates a default bridge network named `<project_name>_default` (e.g., `hercules_default`). All services that don't specify a custom network are connected to this default network.

### Default Docker Network

All services in this environment are connected to the default network:

#### Network Topology

```
                    Docker Default Network (hercules_default)
                    ───────────────────────────────────────────
                    
    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
    │  control-   │    │  data-      │    │  identity-  │    │  issuer-    │
    │  plane      │────│  plane      │    │  hub        │    │  service    │
    │             │    │             │    │             │    │             │
    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
           │                  │                  │                  │
           │                  │                  │                  │
    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
    │  standalone │────│  dtr        │    │  vault      │    │  postgres   │
    │             │    │             │    │             │    │             │
    └─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
           │
           │
    ┌─────────────┐
    │  toolbox    │
    │             │
    └─────────────┘
           │
    ┌─────────────┐    ┌─────────────┐
    │  participant2│   │  postgres2  │
    └─────────────┘    └─────────────┘
```

### Service Hostname Resolution

| Service Name | Hostname | IP (Docker DNS) |
|-------------|----------|-----------------|
| control-plane | `control-plane` | 172.x.x.x |
| standalone | `standalone` | 172.x.x.x |
| data-plane | `data-plane` | 172.x.x.x |
| identity-hub | `identity-hub` | 172.x.x.x |
| issuer-service | `issuer-service` | 172.x.x.x |
| dtr | `dtr` | 172.x.x.x |
| participant2 | `participant2` | 172.x.x.x |
| toolbox | `toolbox` | 172.x.x.x |
| postgres | `postgres` | 172.x.x.x |
| postgres2 | `postgres2` | 172.x.x.x |
| vault | `vault` | 172.x.x.x |

### Key Network Connections

| Service | Connects To | Purpose |
|---------|-------------|---------|
| `standalone` | `control-plane:8081` | EDC management API |
| `standalone` | `data-plane:9500` | Data access via public endpoint |
| `standalone` | `dtr:8090` | FA³ST registry for AAS |
| `standalone` | `identity-hub:15151` | Identity management |
| `standalone` | `issuer-service:13132` | Credential issuance |
| `standalone` | `vault:8200` | Secrets management |
| `data-plane` | `control-plane:8083` | Dataplane selector |
| `data-plane` | `vault:8200` |Keys management |
| `participant2` | `postgres2:5432` | PostgreSQL database |
| `participant2` | `vault:8200` | Secrets management |
| All services | `identity-hub:13131` | Credentials API |
| All services | `issuer-service:15152` | Issuer admin API |

## Understanding the Workflow

### Initialization Flow

1. **Service Startup**: All services start via `docker-compose up`
   - Vault initializes first with secret keys
   - PostgreSQL creates databases
   - Services wait for dependencies (health checks)
   
2. **Network Resolution**: Services connect via Docker's internal DNS:
   - Hostnames like `control-plane`, `data-plane`, `identity-hub` resolve automatically
   - All services must be on the same Docker network (default bridge)

3. **Participant Initialization** (`init_participant.sh`):
   - Creates participant entries in Identity Hub
   - Requests MembershipCredentials from Issuer
   - Generates data plane key pairs
   - Stores secrets in HashiCorp Vault

3. **Issuer Initialization** (`init_issuer.sh`):
   - Registers issuer participant in Identity Hub
   - Creates attestation templates
   - Creates credential definitions

### Credential Flow

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│ Participant  │────▶│  Identity Hub   │◀────│   Issuer     │
│   1          │     │                 │     │   Service    │
└──────────────┘     └─────────────────┘     └──────────────┘
                          │                          │
                          │                          │
                          ▼                          ▼
                  ┌─────────────────┐     ┌──────────────┐
                  │  STS Token      │     │ Attestation  │
                  │  Service        │     │ / Credentials│
                  └─────────────────┘     └──────────────┘
```

1. Participant registers with Identity Hub
2. Participant requests MembershipCredential from Issuer
3. Issuer verifies participant (via attestation)
4. Issuer issues signed credential
5. Credential stored in participant's Identity Hub

### Data Access Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Participant  │────▶│  Control     │────▶│  Data Plane  │
│   2          │     │  Plane       │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
                          │
                          │
                  ┌──────────────┐
                  │  FA³ST       │
                  │  Service     │
                  └──────────────┘
```

1. Participant 2 requests catalog from Participant 1
2. Negotiation for asset access occurs
3. Contract agreement established
4. Data plane token generated
5. Data access via signed token

## Configuration

### Environment Variables (`.env`)

| Variable | Description | Default |
|----------|-------------|---------|
| `CONTROL_PLANE_API_KEY` | API key for control plane management | `password` |
| `EXTENSION_API_KEY` | API key for standalone extension | `password` |
| `IDENTITY_HUB_SUPERUSER_ID` | Identity Hub admin user | `admin` |
| `IDENTITY_HUB_SUPERUSER_KEY` | Identity Hub admin key | `YWRtaW4=.s3cr3t` |
| `ISSUER_SERVICE_SUPERUSER_ID` | Issuer service admin user | `admin` |
| `ISSUER_SERVICE_SUPERUSER_KEY` | Issuer service admin key | `YWRtaW4=.s3cr3t` |
| `POSTGRES_USER` | PostgreSQL username | `postgres` |
| `POSTGRES_PASSWORD` | PostgreSQL password | `password` |
| `VAULT_TOKEN` | HashiCorp Vault root token | `token` |
| `VAULT_PORT` | HashiCorp Vault port | `8200` |

### Participant Configuration

Each participant has a JSON configuration file (`config/toolbox/data/*.json`):
```json
{
  "apiKey": "YWRtaW4=.s3cr3t",
  "did": "did:web:identity-hub%3A10100:participant1",
  "contextId": "did:web:identity-hub%3A10100:participant1",
  "credentialsApi": "http://identity-hub:13131/api/credentials",
  "identityApi": "http://identity-hub:15151/api/identity",
  "stsClientSecretAlias": "participant1-sts-client-secret",
  "dataPlanePrivateKeyAlias": "private-key",
  "dataPlanePublicKeyAlias": "public-key"
}
```

### Key Configuration Files

- **Extension**: `config/edc/extension/configuration.properties`
  - Controls AAS/DTR integration
  - Sets access policies
  - Configures FA³ST service connection

- **FA³ST**: `config/aas/fa3st-config.json`
  - Configures AAS registry endpoints
  - Sets endpoint configurations
  - Defines persistence layer

##Bruno API Testing

Bruno collections are provided for testing APIs:

```bash
# Import into Bruno:
# File: samples/hercules/bruno/opencollection.yml

# Example: Get catalog from Participant 2
cd samples/hercules/bruno
bruno run "discovery/Get Catalog.yml" -e default
```

Available collections:
- `control-plane/`: Policy and configuration queries
- `discovery/`: EDC discovery protocol (catalog, negotiation)
- `data-access/`: Data transfer operations

## Common Tasks

### viewing logs

```bash
docker compose logs -f
docker compose logs -f control-plane
docker compose logs -f Standalone
```

### Restarting a service

```bash
docker compose restart control-plane
docker compose restart standalone
```

### Accessing toolbox for debugging

```bash
docker compose exec toolbox sh
# Inside toolbox:
curl http://control-plane:8081/management
jq . config/toolbox/data/issuer.json
```

### Stopping all services

```bash
docker compose down
# To remove volumes:
docker compose down -v
```

## Development

### Rebuild standalone extension

The standalone extension is built using Gradle:

```bash
cd samples/hercules/standalone-hercules
./gradlew shadowJar
# The JAR is created at: build/libs/standalone-hercules.jar
```

### Add new participant

1. Create participant configuration in `config/toolbox/data/`
2. Add to `init_participant.sh` initialization function
3. Restart and reinitialize toolbox container

### Modify AAS model

1. Edit `config/aas/model.aasx` (binary FA³ST model file)
2. Restart FA³ST service (standalone container)

## Troubleshooting

### vault health check failing

```bash
docker compose exec vault vault status
docker compose logs vault
```

### participant initialization failing

```bash
docker compose exec toolbox cat /scripts/data/issuer.json
docker compose exec toolbox curl -v http://identity-hub:15151/api/identity
docker compose exec toolbox curl -v http://data-plane:9500/public
docker compose exec toolbox curl -v http://participant2:8081/management
docker compose exec toolbox ping data-plane
```

### data access not working

```bash
# Check data plane registration
docker compose exec control-plane curl http://data-plane:9500/public
# Check vault keys
docker compose exec vault vault kv get secret/edc
```

### FA³ST not connecting

```bash
docker compose exec dtr curl http://localhost:8090/shell-descriptors
docker compose exec Standalone cat /resources/fa3st/config.json
docker compose exec Standalone curl http://dtr:8090/shell-descriptors
docker compose exec Standalone curl http://data-plane:9500/public
```

### Network connectivity issues

If services cannot reach each other:

```bash
# Check if toolbox can reach other services
docker compose exec toolbox ping control-plane
docker compose exec toolbox ping data-plane
docker compose exec toolbox ping participant2
docker compose exec toolbox curl -v http://identity-hub:15151/api/identity

# Verify services are on the same network
docker compose network inspect hercules_default

# Check service health
docker compose ps

# Restart services and rebuild networks
docker compose down
docker compose up -d
```

**Common causes**:
- Services not running (check `docker compose ps`)
- Service name typos in configuration
- Services starting before dependencies are healthy
- Docker network not properly created

## Security Notes

⚠️ **This is a sample environment, NOT production-ready:**

- HashiCorp Vault is running in dev mode with known token
- Database credentials are hardcoded in configuration
- API keys are simple passwords
- TLS/SSL is disabled for most services
- Self-signed certificates (if any) are not validated

For production deployment:
- Use production-grade Vault with proper secrets management
- Implement proper certificate chain with trusted CAs
- Use secure password generation and rotation
- Enable TLS for all services
- Implement network policies and firewall rules

## See Also

- [EDC Extension for AAS](../../README.md)
- [FA³ST Service](https://github.com/admin-shell-io/faaast-service)
- [Eclipse DITTO Connectors](https://github.com/eclipse-ditto/ditto-clients)
- [Tractus-X Identity Hub](https://github.com/eclipse-tractusx/identity-hub)
- [FA³ST Registry](https://github.com/FraunhoferIOSB/FAAAST-Registry)