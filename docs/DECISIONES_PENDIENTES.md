# Registro de Decisiones Pendientes y Abiertas — EcoAula

Este documento registra los requisitos abiertos, supuestos provisionales y puntos de extensión pendientes de definición por el equipo de ingeniería de software.

---

## 1. Decisiones Provisionales Aplicadas en la Entrega Actual

| ID | Tema | Decisión Provisional Adoptada | Configuración / Justificación | Estado |
|---|---|---|---|---|
| **DEC-01** | Umbral de aprobación de evaluaciones | **70%** como nota mínima para aprobar | Configurable en `application.yml` (`ecoaula.evaluacion.umbral-aprobacion: 70`). No estaba fijado numéricamente. | Pendiente acordar con equipo docente |
| **DEC-02** | Política de reintentos de evaluación | **Sin límite de reintentos**, pero solo un intento activo a la vez | Si el aprendiz reprueba, puede iniciar un nuevo intento tras revisar el contenido. Configurable en `application.yml`. | Provisional |
| **DEC-03** | Calificación de evaluación | **Calificación automática** para preguntas de selección múltiple con única respuesta | El proceso 3 describe calificación por tutor; se deja la arquitectura desacoplada con el estado de intento preparado para permitir revisión manual en fases futuras. | Pendiente de acordar |
| **DEC-04** | Interfaz por voz en el perfil | **No implementada en esta entrega** | Mencionada en la definición general pero ausente en el mockup aprobado. Se prioriza compatibilidad con lectores de pantalla. | Pendiente para P2 |
| **DEC-05** | Migraciones de base de datos | **JPA `ddl-auto: update` (dev) / `validate`** sin Flyway hoy | Se prioriza tener la base funcional ejecutable para la entrega de hoy. Se agregará Flyway en la siguiente iteración. | Pendiente |
| **DEC-06** | Almacenamiento de archivos de audio | **Almacenamiento en disco local** (`AlmacenamientoLocal`) | Detrás de la interfaz `AlmacenamientoContenido` para conectar AWS S3, Cloudinary o MinIO cuando se elija el proveedor en la nube. | Abierto |
| **DEC-07** | Cacheo de perfil de accesibilidad | **Guardado en sesión HTTP** al autenticar y tras editar perfil | Evita consultas repetitivas a la base de datos en cada petición HTML renderizada por Thymeleaf (mitiga riesgo ADR-02 vs ADR-07). | Activo |
| **DEC-08** | Formato de código de certificado | **Cadena alfanumérica de 20 caracteres agrupada con guiones** | Generada mediante `SecureRandom` con alfabeto no ambiguo (sin `0`, `O`, `1`, `I`, `L`): `EA-XXXX-XXXX-XXXX-XXXX`. | Activo |
