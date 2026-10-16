#!/usr/bin/env bash
# Script de respaldo automatizado para EcoAula (Base de datos MySQL + Audios)
set -euo pipefail

BACKUP_DIR="/var/backups/ecoaula"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_CONTAINER="ecoaula-mysql"
DB_NAME="ecoaula_db"
DB_USER="ecoaula_user"
DB_PASS="${DB_PASSWORD:-ecoaula_secret}"
AUDIO_DIR="/home/ubuntu/ecoaula/storage/contenidos"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Iniciando respaldo de EcoAula..."

# 1. Respaldo de MySQL
DB_BACKUP_FILE="${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql.gz"
echo "  -> Generando volcado de base de datos..."
docker exec "${DB_CONTAINER}" mysqldump -u "${DB_USER}" -p"${DB_PASS}" "${DB_NAME}" | gzip > "${DB_BACKUP_FILE}"
chmod 600 "${DB_BACKUP_FILE}"
echo "     Volcado guardado en: ${DB_BACKUP_FILE}"

# 2. Respaldo de Archivos de Audio
AUDIO_BACKUP_FILE="${BACKUP_DIR}/audio_backup_${TIMESTAMP}.tar.gz"
if [ -d "${AUDIO_DIR}" ]; then
    echo "  -> Empaquetando almacenamiento de audios..."
    tar -czf "${AUDIO_BACKUP_FILE}" -C "${AUDIO_DIR}" .
    chmod 600 "${AUDIO_BACKUP_FILE}"
    echo "     Audios guardados en: ${AUDIO_BACKUP_FILE}"
fi

# 3. Limpieza: Mantener solo respaldos de los últimos 7 días
find "${BACKUP_DIR}" -type f -name "*.gz" -mtime +7 -delete

echo "[$(date)] Respaldo completado exitosamente en ${BACKUP_DIR}."
