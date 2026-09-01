#!/bin/sh
set -e
chmod +x /scripts/*.sh
/scripts/init_issuer.sh
/scripts/init_participant.sh
exec "$@"
