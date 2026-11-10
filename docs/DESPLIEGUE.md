# Manual de Despliegue, Operaciones y Seguridad en Producción — EcoAula

> **Entorno de Producción Oficial:** AWS Lightsail  
> **Dirección IP Pública:** `3.237.185.159`  
> **Dominio HTTPS (SSL/TLS):** `https://3-237-185-159.sslip.io`  
> **Rama de despliegue:** `main`

---

## 1. Arquitectura de Despliegue

La solución corre sobre una instancia **AWS Lightsail** (Ubuntu 24.04 LTS, 2 vCPU, 2 GB RAM, 60 GB SSD NVMe, 2 GB Swap):
1. **Nginx (Proxy Inverso):** Escucha en los puertos estándar 80 (HTTP) y 443 (HTTPS con certificado Let's Encrypt vía Certbot). Realiza la terminación TLS, redirige HTTP a HTTPS, establece `client_max_body_size 30M` para subida de audios, e inyecta la cabecera `X-Real-IP $remote_addr` para que el limitador de tasa de peticiones no sea falsificable.
2. **Spring Boot (Monolito Modular):** Ejecuta bajo `systemd` como el servicio `ecoaula.service` en el puerto 8080 (solo accesible internamente en localhost) bajo el perfil oficial `prod`. Consume sus secretos y credenciales exclusivamente desde `/etc/ecoaula/ecoaula.env`, validando de forma estricta la existencia de `ECOAULA_CLAVE_CIFRADO`.
3. **MySQL 8.0 (Docker):** Corre en contenedor Docker oficial `ecoaula-mysql` con volumen persistente `ecoaula_mysql_data`. Expone el puerto `3307` **estrictamente vinculado a `127.0.0.1` (`127.0.0.1:3307:3306`)**, haciendo imposible el acceso directo desde el exterior.
4. **Almacenamiento de Medios (ADR-08 / DEC-06):** Los archivos de audio MP3/WAV/OGG subidos por los gestores se persisten en `/home/ubuntu/ecoaula/storage/contenidos` en el sistema de archivos del servidor host, evitando pérdidas ante reinicios de contenedores o servicios.

---

## 2. Gestión de Secretos y Variables de Entorno

**REGLA DE SEGURIDAD:** Ninguna contraseña de producción ni clave de cifrado vive en el repositorio de Git.

Todas las variables de entorno de producción residen en el archivo protegido en el servidor:
```bash
/etc/ecoaula/ecoaula.env
```
Este archivo tiene permisos restrictivos `chmod 600` (solo legible por `root` y el usuario del servicio):

```ini
# Configuración de Base de Datos
DB_HOST=127.0.0.1
DB_PORT=3307
DB_NAME=ecoaula_db
DB_USER=ecoaula_user
# Contraseñas propias, generadas en el servidor (openssl rand -base64 24). Nunca las que aparecían en versiones
# anteriores de este documento. DB_ROOT_PASSWORD la usa solo docker-compose y la prueba de restauración.
DB_PASSWORD=<generada en el servidor>
DB_ROOT_PASSWORD=<generada en el servidor, distinta de la anterior>

# Respaldos (sección 4). Bucket de AWS S3 para la copia fuera del servidor; vacío = solo copia local.
BACKUP_S3_BUCKET=<nombre-del-bucket>

# Clave Maestra de Cifrado AES-256 (ADR-09). Generarla en el servidor y no escribirla en el repositorio:
#   openssl rand -base64 32
ECOAULA_CLAVE_CIFRADO=<valor generado en el servidor>

# Cuentas de demostración en producción (DataInitializer).
# - Solo se crea la cuenta cuya variable esté definida con una contraseña propia de 12 o más caracteres,
#   distinta de las de desarrollo del repositorio; si falta o no cumple, esa cuenta se omite (aviso en el log).
# - Las contraseñas se usan al crear la cuenta y NO se restablecen en cada arranque. Para cambiarlas de nuevo
#   desde aquí, arrancar una vez con ECOAULA_SEED_SINCRONIZAR_CONTRASENAS=true.
# - Para no sembrar nada (ni cuentas ni cursos de demostración): ECOAULA_SEED_HABILITADO=false
ECOAULA_SEED_HABILITADO=true
ECOAULA_SEED_APRENDIZ_PASSWORD=<contraseña propia, 12+ caracteres>
ECOAULA_SEED_GESTOR_PASSWORD=<contraseña propia, 12+ caracteres>
ECOAULA_SEED_REVISOR_PASSWORD=<contraseña propia, 12+ caracteres>
ECOAULA_SEED_COORDINADOR_PASSWORD=<contraseña propia, 12+ caracteres>

# Seguridad de Sesión
COOKIE_SECURE=true
```

### 2.1 Cómo se entregan las contraseñas a MySQL (docker-compose)

`docker-compose.yml` no contiene contraseñas: las lee de variables de entorno y se detiene si faltan. En el servidor se
levanta indicando el archivo protegido:

```bash
cd /home/ubuntu/ecoaula
sudo docker compose --env-file /etc/ecoaula/ecoaula.env up -d mysql
```

### 2.2 Rotación de contraseñas (hacerla si alguna vez estuvo escrita en el repositorio o en un documento)

Cambiar la variable en el archivo **no** cambia la contraseña que MySQL ya guardó en su volumen: hay que cambiar las dos.
Orden: primero MySQL, después el archivo, al final reiniciar la aplicación.

```bash
# 1. Generar contraseñas nuevas
NUEVA_APP=$(openssl rand -base64 24 | tr -d '/+=')
NUEVA_ROOT=$(openssl rand -base64 24 | tr -d '/+=')

# 2. Cambiarlas en MySQL (la contraseña vigente de root se pide por teclado; no queda en el historial)
sudo docker exec -it ecoaula-mysql mysql -u root -p -e "ALTER USER 'ecoaula_user'@'%' IDENTIFIED BY '${NUEVA_APP}'; ALTER USER 'root'@'%' IDENTIFIED BY '${NUEVA_ROOT}'; ALTER USER 'root'@'localhost' IDENTIFIED BY '${NUEVA_ROOT}'; FLUSH PRIVILEGES;"

# 3. Actualizar DB_PASSWORD y DB_ROOT_PASSWORD en /etc/ecoaula/ecoaula.env (sudo nano; chmod 600)
# 4. Reiniciar y comprobar
sudo systemctl restart ecoaula && sudo systemctl status ecoaula --no-pager
```

**No rotar `ECOAULA_CLAVE_CIFRADO`** sin migrar los datos: la condición visual ya guardada quedaría ilegible (ADR-09).
Las contraseñas de las cuentas de demostración se cambian como indica la sección 2 (`ECOAULA_SEED_SINCRONIZAR_CONTRASENAS`).

---

## 3. Procedimiento para Redesplegar (Actualizar Código)

Cuando se fusionen nuevos cambios en el repositorio (`git push` a `main`), el redespliegue se ejecuta con los siguientes pasos en la terminal SSH del servidor:

```bash
cd /home/ubuntu/ecoaula

# 1. Traer los últimos cambios
git pull origin main

# 2. Recompilar el paquete ejecutable
./mvnw clean package -DskipTests

# 3. Reiniciar el servicio de Spring Boot
sudo systemctl restart ecoaula

# 4. Verificar estado del servicio
sudo systemctl status ecoaula
```

---

## 4. Política y Procedimiento de Respaldo (Backups)

### Respaldo automático, copia remota y objetivos de recuperación

| Elemento | Definición |
|---|---|
| Frecuencia | Diario a las 02:00 (temporizador `ecoaula-backup.timer` de systemd) |
| **RPO** (pérdida máxima de datos aceptada) | 24 horas |
| **RTO** (tiempo máximo para volver a operar) | 1 hora (restaurar volcado y audios, y reiniciar el servicio) |
| Retención local | 7 días en `/var/backups/ecoaula` |
| Copia fuera del servidor | AWS S3 (`BACKUP_S3_BUCKET`), para que una falla del servidor no se lleve los respaldos |

Activación (una sola vez, en el servidor):

```bash
sudo cp /home/ubuntu/ecoaula/deploy/systemd/ecoaula-backup.service /home/ubuntu/ecoaula/deploy/systemd/ecoaula-backup.timer /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now ecoaula-backup.timer
systemctl list-timers ecoaula-backup.timer        # muestra la próxima ejecución
sudo systemctl start ecoaula-backup.service        # ejecución de prueba inmediata
journalctl -u ecoaula-backup.service --no-pager -n 20
```

Para la copia remota: instalar la AWS CLI, crear un bucket S3 privado y un usuario de AWS con permiso solo de `s3:PutObject` sobre ese
bucket, y configurarlo con `aws configure`. Después, definir `BACKUP_S3_BUCKET` en `/etc/ecoaula/ecoaula.env`.

### Prueba de restauración (evidencia de que el respaldo sirve)

Restaura el último respaldo en una base temporal, verifica tablas y datos, mide el tiempo y la elimina; no toca producción:

```bash
sudo bash -c 'set -a; . /etc/ecoaula/ecoaula.env; set +a; /home/ubuntu/ecoaula/deploy/scripts/verificar-restauracion.sh'
```

Guarde la salida (fecha, tablas restauradas y segundos) como evidencia en el documento de pruebas.

### Generar Respaldo Manual Inmediato
Se provee un script en `/home/ubuntu/ecoaula/deploy/scripts/backup.sh`. Lee `DB_PASSWORD` del archivo protegido:
```bash
sudo bash -c 'set -a; . /etc/ecoaula/ecoaula.env; set +a; /home/ubuntu/ecoaula/deploy/scripts/backup.sh'
```

El script genera dos artefactos en `/var/backups/ecoaula/`:
1. `db_backup_YYYYMMDD_HHMMSS.sql.gz`: Volcado binario comprimido de MySQL (`mysqldump`).
2. `audio_backup_YYYYMMDD_HHMMSS.tar.gz`: Empaquetado comprimido del directorio de audios persistentes.

### Restaurar un Respaldo

1. **Restaurar la Base de Datos:**
   ```bash
   sudo bash -c 'set -a; . /etc/ecoaula/ecoaula.env; set +a; /home/ubuntu/ecoaula/deploy/scripts/restore.sh /var/backups/ecoaula/db_backup_YYYYMMDD_HHMMSS.sql.gz'
   ```
2. **Restaurar los Archivos de Audio:**
   ```bash
   tar -xzf /var/backups/ecoaula/audio_backup_YYYYMMDD_HHMMSS.tar.gz -C /home/ubuntu/ecoaula/storage/contenidos/
   ```
3. **Reiniciar la aplicación:**
   ```bash
   sudo systemctl restart ecoaula
   ```

---

## 5. Control de Acceso y Reglas de Firewall

### Acceso SSH al Servidor
- **Método 1:** Consola Web segura de AWS Lightsail (acceso autenticado con la cuenta de AWS).
- **Método 2:** Llave SSH privada oficial (`LightsailDefaultKey-us-east-1.pem`), descargable únicamente por los administradores de la cuenta de AWS.

### Reglas Activas de Firewall (AWS Lightsail)
- **Puerto 22 (SSH):** Abierto para administración remota.
- **Puerto 80 (HTTP):** Abierto al público (Nginx redirige inmediatamente a HTTPS 443).
- **Puerto 443 (HTTPS):** Abierto al público con certificado SSL/TLS emitido por Let's Encrypt.
- **Puertos 3306 / 3307 (MySQL):** **CERRADOS en el firewall** y vinculados exclusivamente a `127.0.0.1` en el host, imposibilitando conexiones no autorizadas desde internet.
