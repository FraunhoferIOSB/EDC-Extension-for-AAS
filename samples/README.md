# Samples

This directory contains material for exploring the extensions and development resources.

# Example Use Case

In `full`, a use-case with a provider and consumer with all extensions is demonstrated as a starting point for
registering AAS in a data space.

## Note on the HTTPS connector configuration files

Even though 'provider-https.properties' and 'consumer-https.properties' are valid connectors on their own, they cannot
communicate with each other since they both use self-signed TLS certificates. Those certificates are not allowed when
communicating with another connector, so data transfer will not be possible.
