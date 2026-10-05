# EcoAula — Plataforma Web de Aprendizaje Accesible

> Proyecto de Ingeniería de Software III — Universidad del Quindío  
> Diseñado para personas con discapacidad visual (ciegas o con baja visión), garantizando autonomía total con lectores de pantalla (NVDA, JAWS, VoiceOver), navegación exclusiva por teclado y contraste alto.

---

## 🚀 Requisitos Previos

- **Java 21** (JDK 21)
- **Maven 3.9+** (o usar el script `./mvnw` incluido)
- **Docker y Docker Compose** (opcional, para ejecutar MySQL local)

---

## ⚡ Puesta en Marcha Rápida (Entorno Local)

### Opción 1: Desarrollo con base de datos H2 en memoria (Sin dependencias externas)

Es la forma más rápida de ejecutar la aplicación sin necesidad de levantar contenedores Docker:

```bash
# 1. Clonar el repositorio y situarse en la raíz
cd ecoaula

# 2. Ejecutar la aplicación con perfil 'dev'
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
# En Windows PowerShell si no tienes JAVA_HOME global:
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

La aplicación iniciará en: **`http://localhost:8080`**

### Opción 2: Desarrollo con base de datos MySQL vía Docker

```bash
# 1. Levantar el contenedor de MySQL
docker compose up -d

# 2. Ejecutar la aplicación con el perfil dev-mysql
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev-mysql
```

---

## 👤 Usuario de Demostración (Seed Data)

Para ingresar inmediatamente a la plataforma sin necesidad de registrarse previamente:

| Campo | Valor |
|---|---|
| **Correo electrónico** | `aprendiz@ecoaula.local` |
| **Contraseña** | `EcoAula2026!` |
| **Rol** | `APRENDIZ` |
| **Perfil configurado** | Velocidad audio 1.0x, Modo estándar, Tamaño texto normal |

---

## 🛠️ Comandos Principales

```bash
# Ejecutar compilación y pruebas con reporte JaCoCo
./mvnw clean verify

# Ver reporte de cobertura generado por JaCoCo
# Se genera en: target/site/jacoco/index.html
```

---

## 📂 Documentación del Proyecto

- [Convenciones de Código y Arquitectura](docs/CONVENCIONES.md)
- [Flujo de Trabajo GitFlow](docs/GITFLOW.md)
- [Modelo de Datos y Diagrama ER](docs/MODELO_DATOS.md)
- [Registro de Decisiones Pendientes](docs/DECISIONES_PENDIENTES.md)
- [Protocolo de Pruebas de Accesibilidad](docs/PRUEBAS_ACCESIBILIDAD.md)
