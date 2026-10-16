# Modelo de Datos y Entidad-Relación — EcoAula

Este documento detalla el modelo de datos relacional implementado en EcoAula conforme al ADR-04 (MySQL relacional con llaves foráneas y restricciones explícitas) y ADR-08 (Versionamiento inmutable y ciclo de vida editorial de contenidos accesibles).

---

## 1. Diagrama Entidad-Relación (Mermaid)

```mermaid
erDiagram
    APRENDIZ ||--|| PERFIL_ACCESIBILIDAD : "configura (1:1)"
    APRENDIZ ||--o{ MATRICULA : "se inscribe"
    APRENDIZ ||--o{ AVANCE_LECCION : "registra progreso"
    APRENDIZ ||--o{ CONTENIDO_VERSION : "crea como gestor"
    APRENDIZ ||--o{ REVISION_ACCESIBILIDAD : "audita como revisor"
    
    CURSO ||--o{ CONTENIDO : "contiene lecciones ordenadas"
    CURSO ||--o{ MATRICULA : "recibe estudiantes"
    CURSO ||--|| EVALUACION : "evalúa (1:1)"
    
    CONTENIDO ||--o{ CONTENIDO_VERSION : "versiona inmutable (ADR-08)"
    CONTENIDO_VERSION ||--o{ AVANCE_LECCION : "asocia versión vista"
    CONTENIDO_VERSION ||--o{ REVISION_ACCESIBILIDAD : "recibe auditorías técnicas"
    CONTENIDO_VERSION ||--o| CONTENIDO_VERSION : "subsanada desde (versionOrigen)"
    
    REVISION_ACCESIBILIDAD ||--o{ REVISION_CHEQUEOS : "registra criterios conformes"
    
    MATRICULA ||--o{ INTENTO_EVALUACION : "presenta"
    MATRICULA ||--o| CERTIFICADO : "obtiene al aprobar (1:1)"
    
    EVALUACION ||--o{ PREGUNTA : "formula"
    PREGUNTA ||--o{ OPCION_PREGUNTA : "ofrece opciones"
    
    INTENTO_EVALUACION ||--o{ RESPUESTA_INTENTO : "registra selecciones"
    PREGUNTA ||--o{ RESPUESTA_INTENTO : "es respondida"
    OPCION_PREGUNTA ||--o{ RESPUESTA_INTENTO : "es seleccionada"

    APRENDIZ {
        bigint id PK
        varchar nombreCompleto
        varchar correo UK
        varchar telefono
        varchar passwordHash
        varchar rol "APRENDIZ, GESTOR_CONTENIDO, REVISOR_ACCESIBILIDAD..."
        text condicionVisual "Cifrado AES-GCM (ADR-09)"
        boolean consentimientoDatos "Ley 1581/2012"
        datetime fechaConsentimiento
        datetime creadoEn
    }

    PERFIL_ACCESIBILIDAD {
        bigint id PK
        bigint aprendiz_id FK,UK
        double velocidadAudio "0.75 a 2.0"
        varchar modoVisual "ESTANDAR, ALTO_CONTRASTE"
        varchar tamanoTexto "NORMAL, GRANDE"
        boolean soloAudio
    }

    CURSO {
        bigint id PK
        varchar titulo
        text descripcion
        int horas
        varchar area
        varchar modalidad
        boolean activo
    }

    CONTENIDO {
        bigint id PK
        bigint curso_id FK
        varchar titulo
        int orden
    }

    CONTENIDO_VERSION {
        bigint id PK
        bigint contenido_id FK
        int numeroVersion
        varchar estado "BORRADOR, EN_REVISION, PUBLICADA, RECHAZADA, RETIRADA"
        varchar rutaAudio "Nullable en borrador inicial"
        longtext transcripcion "Nullable en borrador inicial"
        datetime publicadaEn "Nullable"
        bigint creado_por_id FK "Nullable"
        datetime creadoEn
        datetime enviadaARevisionEn "Nullable"
        text motivoRechazo "Nullable"
        bigint version_origen_id FK "Nullable"
    }

    REVISION_ACCESIBILIDAD {
        bigint id PK
        bigint version_id FK
        bigint revisor_id FK
        datetime fecha
        varchar resultado "APROBADA, RECHAZADA"
        text motivo "Nullable si aprobada"
    }

    REVISION_CHEQUEOS {
        bigint revision_id FK
        varchar item_codigo
    }

    AVANCE_LECCION {
        bigint id PK
        bigint aprendiz_id FK
        bigint contenido_version_id FK
        datetime completadaEn
    }

    MATRICULA {
        bigint id PK
        bigint aprendiz_id FK
        bigint curso_id FK
        varchar estado "ACTIVA, COMPLETADA"
        datetime fecha
    }

    EVALUACION {
        bigint id PK
        bigint curso_id FK,UK
    }

    PREGUNTA {
        bigint id PK
        bigint evaluacion_id FK
        text enunciado
        int orden
    }

    OPCION_PREGUNTA {
        bigint id PK
        bigint pregunta_id FK
        varchar texto
        varchar descripcionCorta
        boolean correcta
    }

    INTENTO_EVALUACION {
        bigint id PK
        bigint matricula_id FK
        varchar estado "EN_CURSO, ENVIADO, CALIFICADO"
        double puntaje
        boolean aprobado
        datetime fechaInicio
        datetime fechaFin
    }

    RESPUESTA_INTENTO {
        bigint id PK
        bigint intento_id FK
        bigint pregunta_id FK
        bigint opcion_id FK
    }

    CERTIFICADO {
        bigint id PK
        bigint matricula_id FK,UK
        varchar codigoVerificacion UK "SecureRandom EA-XXXX-XXXX-XXXX-XXXX"
        datetime emitidoEn
    }
```

---

## 2. Reglas de Integridad y Restricciones Centrales

1. **Restricción Única de Matrícula (`uk_matricula_aprendiz_curso`):**
   - Un aprendiz no puede matricularse más de una vez en el mismo curso (`UNIQUE (aprendiz_id, curso_id)`).
2. **Inmutabilidad y Trazabilidad de Contenido (ADR-08):**
   - `AvanceLeccion` almacena la llave foránea exacta a `ContenidoVersion`. Esto garantiza que si una lección se actualiza tras ser cursada, queda registrado qué versión exacta consumió el estudiante.
   - Solo puede existir **una versión `PUBLICADA`** a la vez por lección (`Contenido`). Al publicar una nueva versión aprobada, la anterior pasa automáticamente al estado `RETIRADA` sin eliminarse jamás de la base de datos ni del disco.
   - Las versiones en estado `PUBLICADA` y `RETIRADA` son estrictamente de solo lectura.
   - Una versión `RECHAZADA` conserva de manera inmutable el motivo de no conformidad; para corregirla, el gestor genera un nuevo `BORRADOR` que referencia a la versión rechazada a través de `version_origen_id`.
3. **Separación Estricta de Funciones en Auditoría:**
   - Un usuario no puede auditar una versión que él mismo creó como gestor (`creado_por_id != revisor_id`).
   - Los revisores no pueden editar contenidos ni borradores (`REVISOR_ACCESIBILIDAD` restringido en la capa de servicio).
   - Los gestores no pueden aprobar publicaciones (`GESTOR_CONTENIDO` restringido en la capa de servicio).
4. **Cifrado de Datos Sensibles (ADR-09):**
   - El campo `condicionVisual` en `APRENDIZ` se cifra de forma transparente mediante `CondicionVisualConverter` con el algoritmo AES-GCM (128 bits de autenticación).
5. **Verificación Pública Sin Exposición (ADR-06):**
   - El código en `CERTIFICADO` es un identificador aleatorio de alta entropía. La API pública valida únicamente su existencia y vigencia, sin revelar la identidad del estudiante.

---

## 3. Alcance del Proceso 2 (Contenido Accesible)

- **Implementado en esta entrega:**
  - Ciclo editorial completo: `BORRADOR → EN_REVISION → (PUBLICADA | RECHAZADA)`.
  - Requisito de accesibilidad no negociable: Toda lección debe poseer archivo de audio nativo (`.mp3`, `.wav`, `.ogg`) y transcripción textual completa antes de poder enviarse a revisión.
  - Almacenamiento desacoplado en disco local con nombres saneados únicos (`UUID`) y límite configurable de 15 MB.
  - Lista de chequeo técnica de 6 criterios WCAG 2.1 AA / NTC 5854.
  - Auditoría de trazabilidad (`RevisionAccesibilidad`) e indicadores de calidad (tasa de accesibilidad en primera revisión y tiempo promedio de publicación).
- **Fuera de alcance (exclusiones de la entrega):**
  - No se soportan imágenes con texto alternativo (los cursos se basan exclusivamente en audio nativo y transcripciones textuales accesibles).
  - No se soportan videos con audiodescripción sincronizada ni lenguaje de señas.
