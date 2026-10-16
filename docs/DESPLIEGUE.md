# Manual de Despliegue, Operaciones y Seguridad en Producción — EcoAula

> **Entorno de Producción Oficial:** AWS Lightsail  
> **Dirección IP Pública:** `3.237.185.159`  
> **Dominio HTTPS (SSL/TLS):** `https://3-237-185-159.sslip.io`  
> **Rama de despliegue:** `feature/base-inicial` (preparada para merge a `main` para entrega final)

---

## 1. Arquitectura de Despliegue

La solución corre sobre una instancia **AWS Lightsail** (Ubuntu 24.04 LTS, 2 vCPU, 2 GB RAM, 60 GB SSD NVMe, 2 GB Swap):
1. **Nginx (Proxy Inverso):** Escucha en los puertos estándar 80 (HTTP) y 443 (HTTPS con certificado Let's Encrypt vía Certbot). Realiza la terminación TLS, redirige HTTP a HTTPS, establece `client_max_body_size 30M` para subida de audios, e inyecta la cabecera `X-Real-IP $remote_addr` para que el limitador de tasa de peticiones no sea falsificable.
2. **Spring Boot (Monolito Modular):** Ejecuta bajo `systemd` como el servicio `ecoaula.service` en el puerto 8080 (solo accesible internamente en localhost). Consume sus secretos y credenciales exclusivamente desde `/etc/ecoaula/ecoaula.env`.
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
DB_PASSWORD=ecoaula_secret

# Clave Maestra de Cifrado AES-256 (ADR-09) - Generada criptográficamente (fuera del repositorio)
ECOAULA_CLAVE_CIFRADO=k8F#m9$vL2!xP0@qW7^zR4*yB1&cT5(e

# Contraseñas de Cuentas de Demostración para Producción
ECOAULA_SEED_APRENDIZ_PASSWORD=Aprendiz#Seguro2026!
ECOAULA_SEED_GESTOR_PASSWORD=Gestor#Seguro2026!
ECOAULA_SEED_REVISOR_PASSWORD=Revisor#Seguro2026!

# Seguridad de Sesión
COOKIE_SECURE=true
```

---

## 3. Procedimiento para Redesplegar (Actualizar Código)

Cuando se fusionen nuevos cambios en el repositorio (`git push` a `feature/base-inicial` o `main`), el redespliegue se ejecuta con los siguientes pasos en la terminal SSH del servidor:

```bash
cd /home/ubuntu/ecoaula

# 1. Traer los últimos cambios
git pull origin feature/base-inicial

# 2. Recompilar el paquete ejecutable
./mvnw clean package -DskipTests

# 3. Reiniciar el servicio de Spring Boot
sudo systemctl restart ecoaula

# 4. Verificar estado del servicio
sudo systemctl status ecoaula
```

---

## 4. Política y Procedimiento de Respaldo (Backups)

### Generar Respaldo Manual Inmediato
Se provee un script automatizado en `/home/ubuntu/ecoaula/scripts/backup.sh`:
```bash
sudo /home/ubuntu/ecoaula/scripts/backup.sh
```

El script genera dos artefactos en `/var/backups/ecoaula/`:
1. `db_backup_YYYYMMDD_HHMMSS.sql.gz`: Volcado binario comprimido de MySQL (`mysqldump`).
2. `audio_backup_YYYYMMDD_HHMMSS.tar.gz`: Empaquetado comprimido del directorio de audios persistentes.

### Restaurar un Respaldo

1. **Restaurar la Base de Datos:**
   ```bash
   gunzip -c /var/backups/ecoaula/db_backup_YYYYMMDD_HHMMSS.sql.gz | sudo docker exec -i ecoaula-mysql mysql -u ecoaula_user -pecoaula_secret ecoaula_db
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
