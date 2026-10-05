## Tipo de cambio
- [ ] `feat`: Nueva funcionalidad
- [ ] `fix`: Corrección de errores
- [ ] `docs`: Cambios en la documentación
- [ ] `test`: Adición o modificación de pruebas
- [ ] `refactor`: Refactorización de código sin cambio de comportamiento
- [ ] `chore`: Tareas de mantenimiento o configuración

## Descripción
Resumen conciso del cambio introducido y el problema que resuelve.

## Lista de verificación de accesibilidad (WCAG 2.1 AA / NTC 5854)
- [ ] Las vistas tienen `<html lang="es">` y un solo `<h1>`.
- [ ] Estructura semántica con landmarks (`header`, `nav`, `main`).
- [ ] Enlace "Saltar al contenido" presente y funcional con teclado.
- [ ] Formulario con `<label>` explícitos y mensajes de error asociados con `aria-describedby`.
- [ ] Foco de teclado claramente visible en todos los elementos interactivos.
- [ ] Contraste de colores verificado (mínimo 4.5:1 / 7:1 en textos principales).
- [ ] Verificado funcionamiento al 200% de zoom sin desbordamiento horizontal.
- [ ] Elementos de audio usan control nativo `<audio controls>` con transcripción textual debajo.

## Pruebas realizadas
- [ ] Pruebas unitarias ejecutadas con éxito (`mvn test`).
- [ ] Recorrido manual realizado exclusivamente con teclado (`Tab`, `Shift+Tab`, `Enter`, `Espacio`).
