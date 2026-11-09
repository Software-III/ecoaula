#!/usr/bin/env bash
# Prueba de restauración de EcoAula: restaura el respaldo más reciente (o el indicado) en una base
# TEMPORAL, comprueba que las tablas y los datos clave existen y la elimina. No toca la base de producción.
# Uso:  sudo -E ./verificar-restauracion.sh [archivo_db_backup.sql.gz]
# Requiere DB_ROOT_PASSWORD (contraseña de root de MySQL) en el entorno.
set -euo pipefail

BACKUP_DIR="/var/backups/ecoaula"
DB_CONTAINER="ecoaula-mysql"
TEMP_DB="ecoaula_restauracion_prueba"
ROOT_PASS="${DB_ROOT_PASSWORD:?Defina DB_ROOT_PASSWORD (contraseña de root de MySQL)}"
DB_BACKUP_FILE="${1:-$(ls -1t "${BACKUP_DIR}"/db_backup_*.sql.gz | head -n 1)}"

mysql_root() { docker exec -i -e MYSQL_PWD="${ROOT_PASS}" "${DB_CONTAINER}" mysql -u root "$@"; }

echo "[$(date)] Prueba de restauración con: ${DB_BACKUP_FILE}"
INICIO=$(date +%s)
mysql_root -e "DROP DATABASE IF EXISTS ${TEMP_DB}; CREATE DATABASE ${TEMP_DB} CHARACTER SET utf8mb4;"
gunzip -c "${DB_BACKUP_FILE}" | mysql_root "${TEMP_DB}"

TABLAS=$(mysql_root -N -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${TEMP_DB}';")
CURSOS=$(mysql_root -N -e "SELECT COUNT(*) FROM ${TEMP_DB}.cursos;")
USUARIOS=$(mysql_root -N -e "SELECT COUNT(*) FROM ${TEMP_DB}.aprendices;")
mysql_root -e "DROP DATABASE ${TEMP_DB};"
FIN=$(date +%s)

echo "  Tablas restauradas: ${TABLAS} | cursos: ${CURSOS} | usuarios: ${USUARIOS}"
echo "  Tiempo de restauración medido: $((FIN - INICIO)) s"
if [ "${TABLAS}" -lt 10 ] || [ "${USUARIOS}" -lt 1 ]; then
    echo "ERROR: el respaldo no parece completo." >&2
    exit 1
fi
echo "[$(date)] Prueba de restauración EXITOSA. Guarde esta salida como evidencia."
