#!/usr/bin/env bash
# Script de restauración para EcoAula (Base de datos MySQL + Audios)
set -euo pipefail

if [ "$#" -lt 1 ]; then
    echo "Uso: $0 <archivo_db_backup.sql.gz> [archivo_audio_backup.tar.gz] [nombre_base_datos]"
    echo "Ejemplo: $0 /var/backups/ecoaula/db_backup_20261016_191538.sql.gz"
    exit 1
fi

DB_BACKUP_FILE="$1"
AUDIO_BACKUP_FILE="${2:-}"
DB_NAME="${3:-ecoaula_db}"
DB_CONTAINER="ecoaula-mysql"
DB_USER="ecoaula_user"
DB_PASS="${DB_PASSWORD:-ecoaula_secret}"
AUDIO_DIR="/home/ubuntu/ecoaula/storage/contenidos"

echo "[$(date)] Iniciando restauración en ${DB_NAME}..."

if [ ! -f "${DB_BACKUP_FILE}" ]; then
    echo "ERROR: El archivo de volcado no existe: ${DB_BACKUP_FILE}"
    exit 1
fi

# 1. Restaurar base de datos
echo "  -> Descomprimiendo y restaurando volcado en contenedor ${DB_CONTAINER}..."
gunzip -c "${DB_BACKUP_FILE}" | docker exec -i "${DB_CONTAINER}" mysql -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}"
echo "     Base de datos restaurada exitosamente."

# 2. Restaurar audios si se proporcionó archivo
if [ -n "${AUDIO_BACKUP_FILE}" ] && [ -f "${AUDIO_BACKUP_FILE}" ]; then
    echo "  -> Restaurando archivos de audio en ${AUDIO_DIR}..."
    mkdir -p "${AUDIO_DIR}"
    tar -xzf "${AUDIO_BACKUP_FILE}" -C "${AUDIO_DIR}/"
    echo "     Archivos de audio restaurados exitosamente."
fi

echo "[$(date)] Restauración completada exitosamente."
