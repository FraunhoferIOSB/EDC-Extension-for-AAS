# Provider System Test (docker-compose)

Dockerized variant of [`../provider`](../provider): the same test steps against the same expected
resources, but the connectors run as docker containers via [`docker-compose.yaml`](docker-compose.yaml)
instead of being launched on the host machine via `./gradlew launchers:<name>:run`.

## How it works

- `test.sh` first builds the `provider` and `consumer` launcher images from the checked-out sources
  (same `launchers/Dockerfile` and image tags that `./gradlew dockerize` uses). Nothing is patched for
  this: the provider image keeps its hashicorp vault extension.
- `docker-compose.yaml` starts both connectors, mounts the docker-specific configuration
  ([`config/`](config)) and the shared [`../resources`](../resources) (AAS model), and publishes the
  usual test ports to the host. The stack also contains an **ephemeral HashiCorp Vault dev server**
  (`hashicorp/vault` in dev mode: in-memory state, root token `root`, reachable only inside the
  compose network), which the provider connector uses as its vault
  (`edc.vault.hashicorp.*` in [`config/provider.properties`](config/provider.properties)). The vault
  seeds the `consumer` alias (the management API key, resolved via
  `web.http.management.auth.key.alias`, value `password` to match the test requests) at boot; its
  healthcheck only passes once the seeding finished, so the provider starts strictly after that.
  Dev mode is in-memory and the stack is recreated per run, so every test run starts from a fresh,
  freshly-seeded vault.
- The provider container keeps its **localhost identity** (`edc.hostname=localhost`,
  `edc.dsp.callback.address=…localhost…`): every URL the provider advertises (FA³ST service
  `https://localhost:60875`, data-plane public API `http://localhost:8185/api/public`, DSP callback
  `http://localhost:13340/dsp`) is only ever resolved by the provider container itself. This is
  essential: the AAS asset IDs are hashes of those localhost URLs (see
  `AssetIdUtil.ofHashedAccessUrl`), so the shared expected resources (`catalog.json`, `assets.json`,
  ...) keep matching unmodified.
- The consumer container instead advertises its compose service name
  (`edc.dsp.callback.address=http://consumer:23340/dsp`), because the provider resolves that URL to
  push DSP messages to the consumer. Conversely, requests that go through the consumer carry the
  provider's `counterPartyAddress` rewritten to `http://provider:13340` so it resolves inside the
  compose network.
- Readiness is enforced by **docker healthchecks** (`docker-compose.yaml`): the published ports
  accept connections while the API contexts are still deploying, and the AAS assets appear in the
  catalog only after the internally started FA³ST service registered them (up to ~60s after boot).
  The provider is `healthy` once its catalog answers with datasets (the probe greps for the
  ConceptDescription asset id prefix); the consumer once its dsp-client API answers with HTTP 200.
  The probes use busybox `wget`/`grep`, since the alpine launcher images ship no curl. `test.sh`
  blocks on `docker compose up --wait`, so the test steps start once the containers are healthy.
- On exit (success or failure) the container logs are written to `logs/provider.log` and
  `logs/consumer.log` and the compose project is torn down.

## Prerequisites

- Docker (running daemon), and docker compose: either the `docker compose` CLI plugin or a
  `docker-compose` binary. Both must talk to the *same* daemon as `docker` itself.
- No other connectors may occupy the published ports (13337-13341, 23337-23340, 8185).

## Usage

From the repository root run either

```bash
./gradlew testSystemProviderDockerized
```

or directly

```bash
bash system-tests/provider-dockerized/test.sh
```

### Environment variables

| Variable | Default | Description |
|:---------|:--------|:------------|
| `SKIP_IMAGE_BUILD` | unset | Skip the image build entirely and reuse existing images (`provider:latest`, `consumer:latest`). Implies the images contain the in-memory-vault variant of the provider. |
| `PROVIDER_IMAGE` | `provider:latest` | Image used for the provider connector |
| `CONSUMER_IMAGE` | `consumer:latest` | Image used for the consumer connector |
| `START_RUNTIME_TIMEOUT` | `300` | Seconds to wait for a connector container to become healthy (`docker compose up --wait-timeout`; the healthchecks themselves allow up to `retries × interval` seconds) |

## Differences to the non-dockerized test

- The connectors boot from the built launcher images instead of `gradlew run`; boot logs are
  captured from `docker compose logs` into `logs/*.log` on exit. If a healthcheck fails, `up --wait`
  reports the container as `unhealthy` and the test aborts with the dumped logs.
- No build-file patching at all: the hashicorp vault dependency stays in the provider image and a
  dev-mode HashiCorp Vault runs in the compose stack. (The non-dockerized test strips the dependency
  to make the provider use the in-memory vault and restores the build file at the end of the test —
  an aborted run can leave a `build.gradle.kts.bak` behind.)
- The data-plane public API (port 8185) is published to the host for debugging the EDR endpoint;
  the provider resolves it as `http://localhost:8185` inside its own container.
