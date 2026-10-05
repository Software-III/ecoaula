# Modelo de Datos y Entidad-Relación — EcoAula

Este documento detalla el modelo de datos relacional implementado en EcoAula conforme al ADR-04 (MySQL relacional con llaves foráneas y restricciones explícitas).

---

## 1. Diagrama Entidad-Relación (Mermaid)

```mermaid
erDiagram
    APRENDIZ ||--|| PERFIL_ACCESIBILIDAD : "configura (1:1)"
    APRENDIZ ||--o{ MATRICULA : "se inscribe"
    APRENDIZ ||--o{ AVANCE_LECCION : "registra progreso"
    
    CURSO ||--o{ CONTENIDO : "contiene lecciones ordenadas"
    CURSO ||--o{ MATRICULA : "recibe estudiantes"
    CURSO ||--|| EVALUACION : "evalúa (1:1)"
    
    CONTENIDO ||--o{ CONTENIDO_VERSION : "versiona inmutable (ADR-08)"
    CONTENIDO_VERSION ||--o{ AVANCE_LECCION : "asocia versión vista"
    
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
        varchar rol
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
        varchar estado "PUBLICADA, BORRADOR..."
        varchar rutaAudio
        longtext transcripcion
        datetime publicadaEn
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
   - `AvanceLeccion` almacena la llave foránea exacta a `ContenidoVersion`. Esto garantiza que si una lección se actualiza después de que el estudiante la cursó, queda registrado qué versión exacta consumió.
3. **Cifrado de Datos Sensibles (ADR-09):**
   - El campo `condicionVisual` en `APRENDIZ` se cifra de forma transparente mediante `CondicionVisualConverter` con el algoritmo AES-GCM (128 bits de autenticación).
4. **Verificación Pública Sin Exposición (ADR-06):**
   - El código en `CERTIFICADO` es un identificador aleatorio de alta entropía. La API pública valida únicamente su existencia y vigencia, sin revelar la identidad del estudiante.
