# Convenciones de Código y Arquitectura — EcoAula

Este documento define los estándares y acuerdos técnicos obligatorios para el desarrollo de EcoAula (Ingeniería de Software III, Universidad del Quindío).

---

## 1. Convenciones de Nomenclatura

- **Dominio en Español sin tildes:**
  - Clases de entidad: `Aprendiz`, `Matricula`, `Curso`, `Contenido`, `Evaluacion`, `Certificado`.
  - Métodos y operaciones de negocio: `matricular(...)`, `completarLeccion(...)`, `emitirCertificado(...)`, `calificarIntento(...)`.
  - Variables de dominio: `condicionVisual`, `codigoVerificacion`, `puntajeObtenido`.
- **Capas y Elementos Técnicos en Inglés:**
  - Nombres de paquetes: `controller`, `service`, `repository`, `security`, `storage`, `exception`, `notification`, `api`.
  - Sufijos arquitectónicos: `MatriculaController`, `MatriculaService`, `MatriculaRepository`, `SecurityConfig`.
- **Comentarios y Documentación:**
  - Todo comentario, Javadoc y mensajes de excepción en **español claro**.

---

## 2. Responsabilidades por Capa (Arquitectura Estricta)

1. **`controller/` (Controladores Web Thymeleaf):**
   - Reciben solicitudes HTTP, validan formularios (Bean Validation `@Valid`), delegan inmediatamente en la capa de servicio y seleccionan la plantilla Thymeleaf.
   - **Regla estricta:** No contienen lógica de negocio ni manipulan entidades JPA directas en formularios; usan DTOs / Formularios.
2. **`api/` (Endpoints REST Públicos):**
   - Expone servicios sin sesión como la verificación de certificados (`/api/v1/certificados/**`).
   - Retorna exclusivamente DTOs limpios (sin datos sensibles de aprendices ni entidades internas).
3. **`service/` (Lógica de Negocio y Transacciones):**
   - Encapsula las reglas de negocio, validaciones cruzadas (p. ej. lecciones al 100% antes de evaluar, unicidad de matrícula) y transaccionalidad (`@Transactional`).
4. **`repository/` (Acceso a Datos):**
   - Interfaces `JpaRepository` de Spring Data. No contienen lógica de negocio.
5. **`model/` (Entidades y Enums):**
   - Entidades JPA con llaves foráneas explícitas.
6. **`security/` (Seguridad Centralizada):**
   - Único lugar donde reside la autenticación, autorización, hashing de contraseñas (BCrypt), cifrado AES-GCM y rate limiting.
7. **`storage/` (Almacenamiento de Contenido):**
   - Interfaz `AlmacenamientoContenido` para desacoplar el proveedor de archivos (disco local hoy, S3/Blob storage en el futuro).
8. **`notification/` (Notificaciones):**
   - Interfaz `NotificacionService` con implementación `NotificacionLogService` para desacoplar el envío de correos o alertas.
9. **`exception/` (Manejo Centralizado de Errores):**
   - Jerarquía de excepciones que hereda de `EcoAulaException`.
   - `@ControllerAdvice` global que traduce excepciones a vistas de error accesibles.

---

## 3. Accesibilidad como Restricción Central

- **Regla de oro:** Ante cualquier conflicto de diseño visual vs accesibilidad, **prevalece la accesibilidad**.
- Toda vista debe ser navegable y operable 100% mediante teclado.
- Foco siempre visible mediante contorno grueso.
- Elemento nativo `<audio controls>` obligatorio para todo recurso de audio, siempre acompañado de su transcripción completa en texto plano editable/descargable.
- Formatos de texto ampliables hasta un 200% sin pérdida de contenido ni desplazamiento horizontal.
