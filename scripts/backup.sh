#!/usr/bin/env bash
# Compatibilidad: el script vigente vive en deploy/scripts/backup.sh (una sola copia mantenida).
exec "$(dirname "$0")/../deploy/scripts/backup.sh" "$@"
