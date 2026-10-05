# EcoAula — Plataforma Web de Aprendizaje Accesible

> **Proyecto de Ingeniería de Software III — Universidad del Quindío**  
> Diseñado exclusivamente para personas con discapacidad visual (ciegas o con baja visión), garantizando autonomía total con lectores de pantalla (NVDA, JAWS, VoiceOver), navegación exclusiva por teclado y contraste alto.

---

## 🚀 Requisitos Previos

- **Java 21 (JDK 21)** instalado y configurado en el `PATH` (o variable `JAVA_HOME`).
- **Docker y Docker Compose** (recomendado para el perfil estándar con MySQL 8).
- **Maven** (opcional, se incluye el wrapper `./mvnw` y `mvnw.cmd`).

---

## ⚡ Puesta en Marcha Rápida

### Opción 1: Perfil de Desarrollo Estándar (MySQL con Docker Compose — ADR-04)

Es el perfil predeterminado de la plataforma (`dev`), el cual persiste los datos en un contenedor MySQL 8 oficial.

#### 1. Iniciar la base de datos MySQL:
```bash
docker compose up -d
```

#### 2. Ejecutar la aplicación:

- **Linux / macOS (Bash / Zsh):**
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```
- **Windows (PowerShell):**
  ```powershell
  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
  ```
- **Windows (CMD):**
  ```cmd
  mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev
  ```

---

### Opción 2: Perfil Autónomo Alterno (H2 en memoria en modo MySQL — `dev-h2`)

Permite ejecutar y explorar la aplicación inmediatamente sin necesidad de tener Docker instalado ni servicios activos:

- **Linux / macOS (Bash / Zsh):**
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-h2
  ```
- **Windows (PowerShell):**
  ```powershell
  .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev-h2"
  ```
- **Windows (CMD):**
  ```cmd
  mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev-h2
  ```

La aplicación quedará disponible en: **`http://localhost:8080`**

---

## 👤 Usuario de Demostración (Seed Data)

La plataforma inicializa automáticamente datos de prueba con propósitos exclusivamente demostrativos:

| Campo | Valor |
|---|---|
| **Correo electrónico** | `aprendiz@ecoaula.local` |
| **Contraseña** | `EcoAula2026!` |
| **Rol** | `APRENDIZ` |
| **Preferencias iniciales** | Velocidad de audio 1.0x, Modo estándar, Tamaño texto normal |

---

## 🛠️ Comandos de Validación y Pruebas

### Ejecutar todas las pruebas y generar reporte de cobertura JaCoCo:

- **Linux / macOS (Bash / Zsh):**
  ```bash
  # Pruebas estándar y cobertura (100% autónomo sin Docker / MySQL):
  ./mvnw clean verify

  # Pruebas de integración completa contra MySQL 8 (requiere docker compose up -d):
  ./mvnw clean verify -Pintegracion-mysql
  ```
- **Windows (PowerShell):**
  ```powershell
  # Pruebas estándar y cobertura (100% autónomo sin Docker / MySQL):
  .\mvnw.cmd clean verify

  # Pruebas de integración completa contra MySQL 8 (requiere docker compose up -d):
  .\mvnw.cmd clean verify -Pintegracion-mysql
  ```
- **Windows (CMD):**
  ```cmd
  REM Pruebas estándar y cobertura (100% autónomo sin Docker / MySQL):
  mvnw.cmd clean verify

  REM Pruebas de integración completa contra MySQL 8 (requiere docker compose up -d):
  mvnw.cmd clean verify -Pintegracion-mysql
  ```

El reporte interactivo de cobertura se genera en: `target/site/jacoco/index.html`.

---

## 🌐 Verificación Pública de Certificados

- **Página pública web:** `http://localhost:8080/verificar`
- **API REST pública (ADR-06, sin sesión, rate limiting):**
  ```bash
  curl http://localhost:8080/api/v1/certificados/EA-XXXX-XXXX-XXXX-XXXX/verificacion
  ```
  Retorna estrictamente: `{"valido": true}` o `{"valido": false}`.

---

## 📂 Documentación del Proyecto

- [Convenciones de Código y Arquitectura](docs/CONVENCIONES.md)
- [Flujo de Trabajo GitFlow](docs/GITFLOW.md)
- [Modelo de Datos y Diagrama ER](docs/MODELO_DATOS.md)
- [Registro de Decisiones Pendientes](docs/DECISIONES_PENDIENTES.md)
- [Protocolo de Pruebas de Accesibilidad](docs/PRUEBAS_ACCESIBILIDAD.md)
