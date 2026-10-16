# Notas de integración — Procesos 1 y 2

> Rama `feature/completando-proces-1-y-2`, creada desde `feature/base-inicial` (incluye el PR #1 `feature/proceso2-interno`).
> Integra sobre la implementación del Proceso 2 de Leonardo el trabajo de Natalia en los Procesos 1 y 2.

## 1. Criterio de integración

Se tomó como **base la implementación de Leonardo** (ya revisada y fusionada en la rama compartida) y se le agregó lo que no tenía. Se conservan sus rutas (`/gestor/**`, `/revisor/**`), sus servicios (`GestionContenidoService`, `RevisionContenidoService`), su modelo (`creadoPor`, `versionOrigen`, `motivoRechazo`, versiones rechazadas inmutables y subsanación) y su ajuste de `pom.xml` para Java 25. Se descartó la implementación paralela de gestor y revisor que había hecho Natalia (`/gestion`, `/revision`).

**Pruebas:** 84 en total, todas en verde (`./mvnw test`, excluyendo la prueba opcional contra MySQL). Se verificaron además 44 pantallas de los cuatro roles contra un MySQL 8.0 limpio.

## 2. Defectos encontrados en la base y corregidos

| Defecto | Efecto | Corrección |
|---|---|---|
| `/gestor/lecciones/{id}/versiones` y `/revisor/cola` leían relaciones *lazy* fuera de la transacción (`open-in-view` está desactivado) | **Error 500** en ambas pantallas: el flujo de revisión no se podía usar de punta a punta. Las pruebas no lo detectaban porque solo verifican permisos | Consultas con `@EntityGraph` para las vistas (`findDetalleById`, `findConDetalle…`); las operaciones internas siguen usando `findById` |
| El gestor marcaba un curso como "activo" libremente y cada lección nueva crea una v1 vacía | Un curso publicado podía tener lecciones sin versión publicada: el aprendiz recibe 404 en esa lección y no puede terminar el curso ni presentar la evaluación | La casilla "activo" se reemplazó por **Publicar / Retirar del catálogo** con validación (ver sección 3) |
| El sembrado agregaba la lección 4 "Atención telefónica…" **en revisión** dentro de un curso publicado | El mismo problema anterior con los datos demo | Se quitó; la cola del revisor se llena con `DatosPruebaRevisionInitializer` (cursos fuera del catálogo) |
| First-Pass contaba la subsanación aprobada como "aprobada en primera revisión" | Indicador inflado (con los datos de prueba recién sembrados: 50 %, 2 de 4, en lugar de 33,3 %, 1 de 3) | `calcularIndicadores` excluye las versiones con `versionOrigen` (prueba nueva: `subsanacionNoCuentaComoPrimeraRevision`) |
| El rechazo no enviaba los criterios marcados (formulario separado) | El informe de un rechazo no decía qué criterios sí se cumplían | Un solo formulario con dos botones (`formaction`) |
| `AccesibilidadService.obtenerPerfilPorCorreo` crea el perfil por defecto en una transacción de solo lectura (código previo) | En MySQL falla el INSERT en cada página que ven gestor y revisor (no tienen perfil sembrado); en H2 no se nota | `@Transactional` sin `readOnly` |
| Agregar lección permitía cualquier número de orden | Huecos o duplicados rompen la navegación del aprendiz (lección n → n+1) | La lección se agrega siempre al final; al eliminar se renumera |

## 3. Reglas nuevas en el flujo del gestor

| Acción | Se permite cuando |
|---|---|
| Publicar en el catálogo | ≥ 1 lección, **todas** con versión PUBLICADA y el curso tiene evaluación final |
| Modificar datos del curso o título de lección | El curso está fuera del catálogo |
| Agregar o eliminar lecciones, eliminar el curso | Fuera del catálogo **y** sin aprendices matriculados |
| Eliminar algo con una versión EN_REVISION | Nunca |
| Enviar a revisión | Además de audio y transcripción: fuente del material y licencia declaradas (Ley 23 de 1982) |
| Nueva versión de una lección publicada | Parte de la vigente (transcripción, audio, fuente e imágenes) en lugar de un borrador vacío |
| Subsanación | Hereda también la fuente y las imágenes de la rechazada |
| Curso nuevo | Nace fuera del catálogo; duración de 8 a 40 horas (caso de estudio); cupo opcional |

## 4. Cambios en el código de Leonardo (para revisar al integrar)

- **`GestionContenidoService`**: constructor con `MatriculaRepository`, `EvaluacionRepository`, `RecursoImagenRepository` y `ContenidoService`; `crearCurso`/`actualizarCurso` reciben `cupoMaximo` en lugar de `activo`; métodos nuevos `publicarEnCatalogo`, `retirarDelCatalogo`, `eliminarCurso`, `eliminarLeccion`, `actualizarTituloLeccion`, `obtenerVersionDetalle`; sobrecarga de `actualizarBorrador` con fuente y licencia (la firma original se conserva).
- **`RevisionContenidoService`**: constructor con `ContenidoRepository`, `MatriculaRepository` y `NotificacionService`; bloqueo pesimista de la lección al publicar; aviso a los matriculados al publicar una versión nueva; `listarInformes`, `obtenerInforme`, `historialDeLeccion`, `obtenerVersionDetalle`.
- **`GestorContenidoController` / `RevisorAccesibilidadController`**: acciones nuevas (publicar, retirar, eliminar, renombrar, imágenes); las reglas de negocio vuelven a la pantalla con mensaje en lugar de la página de error; `/revisor/indicadores` redirige al tablero unificado `/indicadores`.
- **`CursoForm`**: sin `activo`; con `cupoMaximo`; horas 8–40. **`BorradorVersionForm`**: `fuenteMaterial` y `licenciaDeclarada`.
- **`ContenidoVersion`**: `fuenteMaterial`, `licenciaDeclaradaEn`.
- **`AudioController`** se reemplazó por `ArchivoContenidoController` (`/audio/**` e `/imagenes/**` exigen sesión y matrícula; responde `Range` 206; admite OGG).
- **`EcoAulaAuthenticationSuccessHandler`**: registra el inicio de sesión de aprendices (analítica) y lleva al coordinador a `/reportes-barrera`.
- **Vistas** `gestor/*` y `revisor/*`: estado en el catálogo, cupos, publicar/retirar, eliminar con casilla de confirmación, imágenes, licencia, historial e informes; `revisor/indicadores.html` se eliminó.
- **Pruebas de Leonardo actualizadas** (mínimo): constructores con los *mocks* nuevos; el curso de `GestionContenidoServiceTest` queda fuera del catálogo; el borrador de "enviar a revisión exitosamente" declara licencia.

## 5. Lo que aporta el trabajo de Natalia

- **Proceso 1:** cupo con bloqueo pesimista, velocidad de audio aplicada, voz para lectura en voz alta, página "Mi curso" (continuar donde lo dejó), aviso de matrícula solo al matricularse, sin matrícula implícita, avance validado.
- **Proceso 2:** imágenes con texto alternativo, licencia del material, archivos protegidos, informes de revisión imprimibles (`/informes-revision`), datos de prueba para el revisor.
- **Transversal:** reporte de barreras de accesibilidad (`/reportar-barrera`, atención en `/reportes-barrera`), bitácora de eventos y tablero de indicadores del Plan de Calidad (`/indicadores`), usuario coordinador.

## 6. Al integrar

1. **Base de datos de desarrollo:** si alguien tiene en su MySQL el esquema de la rama de respaldo de Natalia (tabla `revisiones_accesibilidad` con columnas `contenido_version_id`, `ronda`…), debe reiniciarla: `docker compose down -v && docker compose up -d`. Con el esquema de Leonardo, `ddl-auto: update` agrega las columnas y tablas nuevas sin problema.
2. **Usuarios demo** (se siembran en una base vacía):

   | Rol | Correo | Contraseña |
   |---|---|---|
   | Aprendiz | `aprendiz@ecoaula.local` | `EcoAula2026!` |
   | Gestor | `gestor@ecoaula.local` | `GestorEcoAula2026!` |
   | Revisor | `revisor@ecoaula.local` | `RevisorEcoAula2026!` |
   | Coordinación | `coordinador@ecoaula.local` | `CoordinadorEcoAula2026!` |

3. **Guía de pruebas manuales por rol:** `docs/GUIA_PRUEBAS_ROLES.md`.

## 7. Pendiente

- Funciones restantes del coordinador de matrícula (consultar y gestionar matrículas).
- Auditoría automatizada WCAG (axe-core o Lighthouse) en el CI: el tablero muestra ese indicador "sin datos".
- Umbral mínimo de cobertura en JaCoCo (`check`), a definir en equipo.
- Proceso 3: no hay pantalla para crear evaluaciones; por eso un curso creado desde la interfaz aún no se puede publicar.
- Sugerencia: validar la firma real de los archivos de audio en `ValidadorAudioService` (hoy confía en la extensión y el tipo que envía el navegador).
