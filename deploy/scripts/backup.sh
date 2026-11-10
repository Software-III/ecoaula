#!/usr/bin/env bash
# Script de respaldo automatizado para EcoAula (Base de datos MySQL + Audios).
# Se ejecuta a diario con el temporizador deploy/systemd/ecoaula-backup.timer (ver docs/DESPLIEGUE.md, sección 4).
# Si BACKUP_S3_BUCKET está definido (y existe la AWS CLI), copia los respaldos a almacenamiento remoto
# fuera del servidor: así una falla del servidor no se lleva también los respaldos.
set -euo pipefail

BACKUP_DIR="/var/backups/ecoaula"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
DB_CONTAINER="ecoaula-mysql"
DB_NAME="ecoaula_db"
DB_USER="ecoaula_user"
DB_PASS="${DB_PASSWORD:?Defina DB_PASSWORD (ver /etc/ecoaula/ecoaula.env)}"
AUDIO_DIR="/home/ubuntu/ecoaula/storage/contenidos"

mkdir -p "${BACKUP_DIR}"

echo "[$(date)] Iniciando respaldo de EcoAula..."

# 1. Respaldo de MySQL
DB_BACKUP_FILE="${BACKUP_DIR}/db_backup_${TIMESTAMP}.sql.gz"
echo "  -> Generando volcado de base de datos..."
docker exec -e MYSQL_PWD="${DB_PASS}" "${DB_CONTAINER}" mysqldump --no-tablespaces -u "${DB_USER}" "${DB_NAME}" | gzip > "${DB_BACKUP_FILE}"
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

# 3. Copia remota (fuera del servidor) cuando está configurada
if [ -n "${BACKUP_S3_BUCKET:-}" ]; then
    if command -v aws >/dev/null 2>&1; then
        echo "  -> Copiando respaldos a s3://${BACKUP_S3_BUCKET}/ ..."
        aws s3 cp "${DB_BACKUP_FILE}" "s3://${BACKUP_S3_BUCKET}/db/" --only-show-errors
        if [ -f "${AUDIO_BACKUP_FILE}" ]; then
            aws s3 cp "${AUDIO_BACKUP_FILE}" "s3://${BACKUP_S3_BUCKET}/audio/" --only-show-errors
        fi
        echo "     Copia remota completada."
    else
        echo "ERROR: BACKUP_S3_BUCKET está definido pero la AWS CLI no está instalada." >&2
        exit 1
    fi
else
    echo "  AVISO: BACKUP_S3_BUCKET no está definido; los respaldos solo quedan en este servidor."
fi

# 4. Limpieza local: mantener solo respaldos de los últimos 7 días
find "${BACKUP_DIR}" -type f -name "*.gz" -mtime +7 -delete

echo "[$(date)] Respaldo completado exitosamente en ${BACKUP_DIR}."
