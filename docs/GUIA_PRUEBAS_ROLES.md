# Guía de Pruebas Manuales por Rol — EcoAula

> Cubre el **Proceso 1** (inscripción y matrícula accesible) y el **Proceso 2** (gestión de contenido accesible) con los cuatro roles: aprendiz, gestor de contenido, revisor de accesibilidad y coordinación de matrícula.

Marque cada casilla a medida que verifique el resultado esperado. Los escenarios del gestor y del revisor están encadenados: siga el orden de las secciones 3 y 4.

---

## 1. Arranque

```bash
./mvnw clean
SERVER_PORT=8081 ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- `clean` evita clases viejas en `target/` (por ejemplo, el antiguo `AudioController`).
- `SERVER_PORT` permite usar otro puerto si el 8080 está ocupado.
- Si su base MySQL de desarrollo tiene un esquema anterior incompatible, reiníciela: `docker compose down -v && docker compose up -d`.

### Usuarios de demostración

| Rol | Correo | Contraseña | Al iniciar sesión llega a |
|---|---|---|---|
| Aprendiz | `aprendiz@ecoaula.local` | `EcoAula2026!` | `/cursos` |
| Gestor de contenido | `gestor@ecoaula.local` | `GestorEcoAula2026!` | `/gestor/cursos` |
| Revisor de accesibilidad | `revisor@ecoaula.local` | `RevisorEcoAula2026!` | `/revisor/cola` |
| Coordinación de matrícula | `coordinador@ecoaula.local` | `CoordinadorEcoAula2026!` | `/reportes-barrera` |

Use una ventana de incógnito por rol. Tenga a mano un audio `.wav`, `.mp3` u `.ogg` y una imagen `.png` o `.jpg`.

### Datos de prueba sembrados

| Curso | Estado | Qué permite probar |
|---|---|---|
| **Prueba: Hojas de cálculo accesibles** | Fuera del catálogo | Lección 1: borrador completo con imagen, listo para enviar. Lección 2: borrador **sin licencia** (el envío falla). Lección 3: borrador vacío |
| **Prueba: Correo electrónico accesible** | Fuera del catálogo, en revisión | Lección 1 en revisión (con imagen). Lección 2: versión 1 rechazada y versión 2 (subsanación) en revisión |
| **Prueba: Inglés para el trabajo** | Revisado, pendiente de publicar | Lecciones aprobadas y evaluación final: **se puede publicar** |
| *Demostración: Atención al Cliente* (publicado) | Lección 2 con una versión 2 en revisión | Comparar con la vigente y, al aprobar, el aviso a los matriculados |

La cola del revisor inicia con 3 versiones y la Tasa First-Pass en 33,3 % (1 de 3): las subsanaciones no cuentan como primera revisión.

---

## 2. Aprendiz

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| A1 | Registrarse en `/registro` **sin** marcar el consentimiento | Error: el consentimiento es obligatorio (Ley 1581 de 2012) | ☐ |
| A2 | Registrarse con el consentimiento e iniciar sesión | Llega al catálogo | ☐ |
| A3 | Escribir en la URL `/cursos/1/lecciones/1` sin estar matriculado | **403**: "Debe matricularse en el curso…" | ☐ |
| A4 | Matricularse en un curso | Confirmación con el aviso "matrícula registrada con éxito" y el botón "Comenzar curso" | ☐ |
| A5 | Completar la lección 1, volver al catálogo y pulsar "Continuar curso" | "Mi curso": "1 de N lecciones completadas" y "Continuar con la lección 2"; **sin** repetir el aviso de matrícula | ☐ |
| A6 | Reproducir, **adelantar y retroceder** el audio | Funciona (peticiones `Range`) | ☐ |
| A7 | Descargar la transcripción `.txt` | Incluye la descripción de las imágenes, si las hay | ☐ |
| A8 | Completar todas las lecciones | "Mi curso" ofrece "Presentar evaluación final" | ☐ |
| A9 | En `/perfil`, alto contraste, texto grande y velocidad **1.5x** | Se aplican en todas las páginas; el audio de la lección suena a 1.5x | ☐ |
| A10 | En `/perfil` elegir una voz y pulsar "Probar voz y velocidad" | Se escucha la voz elegida (requiere voces en español en el navegador) | ☐ |
| A11 | En una lección, "Leer en voz alta la transcripción" y luego "Detener" | Lee con la voz y la velocidad del perfil | ☐ |
| A12 | Abrir `/gestor/cursos` o `/revisor/cola` | **403** | ☐ |
| A13 | Sin sesión, abrir `/audio/leccion_demo_ofimatica_1.wav` | Redirige al inicio de sesión | ☐ |
| A14 | Matricularse en un curso con el cupo lleno (ver G12) | "Cupos disponibles: 0 de N", aviso y sin botón de matrícula | ☐ |
| A15 | Sin sesión, desde el login, "Reporte una barrera de accesibilidad" | Formulario público; vacío da error, con descripción muestra el número de reporte | ☐ |
| A16 | En una lección, "Reportar una barrera en esta lección" | La etapa "Contenido" y el lugar (curso y lección) llegan prellenados | ☐ |
| A17 | Abrir "Mis reportes" | Lista sus reportes con estado; tras C3 muestra la respuesta del equipo | ☐ |

---

## 3. Gestor de contenido (`/gestor/cursos`)

### 3.1 Nueva versión de una lección

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| G1 | Entrar al listado de cursos | Cada curso muestra si está en el catálogo, sus cupos y si tiene evaluación | ☐ |
| G2 | *Hojas de cálculo* → Gestionar lecciones → lección 2 → Versiones → Editar borrador → "Enviar esta versión a Revisión" | Mensaje: falta registrar la fuente y la licencia (Ley 23 de 1982) | ☐ |
| G3 | En el mismo borrador, agregar una imagen **sin** texto alternativo | Error: el texto alternativo es obligatorio (WCAG 1.1.1) | ☐ |
| G4 | Agregar la imagen con texto alternativo y descripción detallada | Aparece en "Imágenes de la lección" | ☐ |
| G5 | Escribir la fuente, marcar la licencia, guardar y enviar a revisión | "Ha sido enviada a revisión técnica"; queda EN_REVISION | ☐ |
| G6 | En una lección publicada, "Crear Nueva Versión en Borrador" | El borrador parte de la versión vigente (transcripción, audio, fuente e imágenes) | ☐ |

### 3.2 Curso nuevo y catálogo

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| G7 | "Crear Nuevo Curso" con **5 horas** | Error: mínimo 8 horas | ☐ |
| G8 | Crearlo con 16 horas | Queda **fuera del catálogo**; el aprendiz no lo ve | ☐ |
| G9 | Agregar una lección y pulsar "Publicar en el catálogo" | Error con el detalle: la lección no tiene versión aprobada y falta la evaluación final | ☐ |
| G10 | Publicar *Prueba: Inglés para el trabajo* | Queda en el catálogo; un aprendiz puede matricularse | ☐ |
| G11 | Retirar del catálogo *Atención al Cliente* | Pasa a "Fuera del catálogo"; si tiene matriculados, no se ofrece agregar ni eliminar lecciones | ☐ |
| G12 | Antes de G10, editar *Inglés para el trabajo* con **cupo máximo 1** | El listado muestra "0 de 1 cupos ocupados" | ☐ |

### 3.3 Modificar y eliminar (solo cursos fuera del catálogo)

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| E1 | Editar título y horas de *Hojas de cálculo* | Datos actualizados | ☐ |
| E2 | En sus lecciones, "Modificar o eliminar la lección 3" → nuevo título | Título actualizado | ☐ |
| E3 | Eliminar una lección **sin** marcar la casilla | Mensaje: marque la casilla de confirmación | ☐ |
| E4 | Eliminar la lección 1 confirmando | Se elimina; las siguientes pasan a ser la 1 y la 2 | ☐ |
| E5 | Intentar eliminar una lección de *Correo electrónico accesible* | Error: tiene una versión en revisión | ☐ |
| E6 | Abrir por URL `/gestor/cursos/{id}/editar` de un curso publicado y guardar | Error: retírelo del catálogo para modificar sus datos | ☐ |
| E7 | Retirar un curso con matriculados e intentar eliminarlo | Error: tiene aprendices matriculados | ☐ |
| E8 | Eliminar un curso de prueba confirmando | Desaparece con sus lecciones, versiones, imágenes, informes y evaluación | ☐ |

---

## 4. Revisor de accesibilidad (`/revisor/cola`)

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| R1 | Entrar a la cola | Versiones pendientes, incluida la de G5; la subsanación indica su versión de origen | ☐ |
| R2 | "Auditar y Calificar" la versión de G5 | Audio, transcripción, imágenes con texto alternativo, fuente y licencia | ☐ |
| R3 | Marcar 2 criterios y "Aprobar y Publicar" | Vuelve a la auditoría con el aviso de que faltan criterios; los marcados se conservan | ☐ |
| R4 | Escribir el motivo y "Rechazar Versión con este Motivo" | La versión queda RECHAZADA | ☐ |
| R5 | Abrir "Informes de revisión" y el informe de R4 | Tabla con "Cumple / No cumple" por criterio y el motivo; con `Ctrl + P` se imprime sin el menú | ☐ |
| R6 | Auditar la subsanación de *Correo electrónico accesible* (lección 2) | Muestra el historial: "Versión 1: rechazada…" con enlace a su informe | ☐ |
| R7 | Marcar todos los criterios y aprobar | Se publica; la anterior queda RETIRADA | ☐ |
| R8 | Abrir `/gestor/cursos` | **403** | ☐ |

### Subsanación por el gestor

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| G13 | En las versiones de la lección rechazada en R4 | Muestra el motivo y "Ver informe de revisión completo" | ☐ |
| G14 | "Subsanar (Crear borrador desde esta)", corregir y reenviar | Se crea una versión nueva con la imagen y la fuente de la rechazada; la rechazada no cambia | ☐ |

---

## 5. Coordinación de matrícula y tablero

| # | Acción | Resultado esperado | ✔ |
|---|---|---|---|
| C1 | Iniciar sesión como coordinación | Llega a "Reportes de barreras" con los reportes de A15 y A16 | ☐ |
| C2 | Abrir el reporte de A16 y marcarlo "Resuelto" **sin** respuesta | Error: escriba la respuesta para el aprendiz | ☐ |
| C3 | Escribir la respuesta y guardar como "Resuelto" | Queda resuelto, atendido por la coordinación | ☐ |
| C4 | Abrir "Indicadores" (también para gestor y revisor) | Procesos 1 y 2 con valor, meta, resultado y cómo se calcula cada indicador; incluye First-Pass y tiempo promedio de publicación | ☐ |
| C5 | Revisar "Aprendices en riesgo de abandono" | Cursos incompletos con 7 días o más sin actividad (vacío al principio) | ☐ |
| C6 | Como aprendiz, abrir `/indicadores` o `/reportes-barrera` | **403** | ☐ |

> Los indicadores salen únicamente del uso real (la plataforma no siembra eventos ni reportes ficticios), para que las cifras sirvan como evidencia del Plan de Calidad. Solo cuenta la actividad de los aprendices.

---

## 6. Verificaciones entre roles

| # | Escenario | Resultado esperado | ✔ |
|---|---|---|---|
| X1 | Un aprendiz que **ya había completado** una lección antes de R7 la abre de nuevo | Sigue viendo su versión anterior (ADR-08) | ☐ |
| X2 | Un aprendiz matriculado que **no la había iniciado** la abre | Ve la versión nueva, con sus imágenes | ☐ |
| X3 | Un aprendiz abre la URL `/imagenes/...` de un **borrador** | **404** | ☐ |
| X4 | Tras G11, un aprendiz **no matriculado** revisa el catálogo | No ve el curso ni puede matricularse | ☐ |
| X5 | Tras G11, un aprendiz **matriculado** revisa el catálogo | Ve el curso con el aviso "retirado del catálogo" y sigue entrando a sus lecciones | ☐ |
| X6 | Tras R7 en *Atención al Cliente*, revisar la consola de la aplicación | `[NOTIFICACION CONTENIDO]` por cada aprendiz matriculado | ☐ |

---

## 7. Fuera del alcance de esta guía (pendiente)

- Funciones restantes de la coordinación de matrícula (gestionar matrículas).
- Auditoría automatizada WCAG en el CI.
- Crear la evaluación final desde la interfaz (Proceso 3): por eso un curso creado desde la interfaz aún no se puede publicar.
