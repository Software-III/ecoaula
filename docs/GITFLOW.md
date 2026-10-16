# Flujo de Trabajo en Git (GitFlow) — EcoAula

Este documento define la estrategia de ramificación, ciclo de vida de cambios y convención de commits para el equipo de desarrollo de EcoAula.

---

## 1. Estructura de Ramas

- **`main`**:
  - Rama de producción/estable.
  - Solo recibe fusiones probadas y verificadas provenientes de `develop`.
- **`develop`**:
  - Rama de integración principal activa del equipo.
  - Todo trabajo finalizado se integra aquí mediante Pull Request.
- **`feature/<historia-o-modulo>`**:
  - Ramas de funcionalidad corta nacidas a partir de `develop`.
  - Ejemplo: `feature/fase0-base`, `feature/proceso1-matricula`, `feature/evaluacion-accesible`.

---

## 2. Formato de Commits (Conventional Commits)

Los mensajes de commit deben seguir la estructura:
```text
tipo(ámbito): descripción corta en presente y minúsculas
```

### Tipos Permitidos:
- **`feat`**: Nueva funcionalidad para el usuario.
- **`fix`**: Corrección de un error funcional o visual.
- **`docs`**: Modificaciones exclusivamente en documentación.
- **`test`**: Adición o ajuste de pruebas automatizadas.
- **`refactor`**: Cambios de código que ni agregan funcionalidad ni corrigen bugs.
- **`chore`**: Tareas de configuración, dependencias o tooling.

### Ejemplos:
- `feat(seguridad): configurar spring security y cifrado aes-gcm`
- `feat(matricula): implementar formulario accesible de registro`
- `docs(arquitectura): agregar diagrama de modelo de datos er`
- `test(evaluacion): verificar regla de bloqueo de evaluacion incompleta`

---

## 3. Política de Integración y Pull Requests

1. No se realiza `push` directo a `main` ni a `develop`.
2. Todo Pull Request hacia `develop` debe:
   - Superar el pipeline de integración continua (`mvn -B verify` en verde).
   - Incluir la lista de verificación de accesibilidad marcada en la plantilla.
   - Contar con la revisión y aprobación de al menos un compañero del equipo.
