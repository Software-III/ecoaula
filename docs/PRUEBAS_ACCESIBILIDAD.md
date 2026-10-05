# Protocolo de Pruebas de Accesibilidad — EcoAula

> Basado en la **Resolución 1519 de 2020 de MinTIC**, **Norma Técnica Colombiana NTC 5854** y **WCAG 2.1 Nivel AA**.

Este documento establece la metodología de pruebas de accesibilidad aplicable a las 6 pantallas implementadas en EcoAula para garantizar su uso 100% autónomo por personas con discapacidad visual.

---

## 1. Protocolo de Navegación Exclusiva por Teclado

### Comandos de Prueba:
- **`Tab`**: Desplazarse secuencialmente hacia adelante entre elementos interactivos (enlaces, botones, campos de formulario).
- **`Shift + Tab`**: Retroceder secuencialmente entre elementos interactivos.
- **`Enter`**: Activar enlaces y enviar formularios.
- **`Espacio`**: Seleccionar casillas de verificación (checkboxes), alternar botones de radio y pausar/reanudar `<audio controls>`.
- **`Flechas Arriba / Abajo / Izquierda / Derecha`**: Cambiar opciones dentro de un grupo de radio buttons y controlar el volumen o la línea de tiempo del reproductor de audio nativo.
- **`Escape`**: Cancelar o salir de menús si aplica.

### Criterios de Aceptación:
1. **Foco visible permanente (Criterio 2.4.7):** Cada elemento interactivo muestra un contorno grueso de alto contraste (`outline: 4px solid #002752` en modo estándar o `#00FFFF` en modo alto contraste con offset de 3px).
2. **Enlace de salto al contenido (Criterio 2.4.1):** Es el primer elemento enfocable de la página. Al presionar `Tab` al cargar, aparece visiblemente en la esquina superior izquierda con la leyenda *"Saltar al contenido principal"*. Al presionar `Enter`, el foco se traslada directamente al `<main>` sin tener que pasar por la cabecera.
3. **Sin trampas de teclado (Criterio 2.1.2):** En ningún punto del recorrido el foco queda atrapado en un control sin poder avanzar o retroceder.

---

## 2. Protocolo de Zoom al 200% (Criterio 1.4.10 - Reflow)

### Procedimiento:
1. En el navegador web (Chrome/Firefox/Edge), aplicar un nivel de aumento de **200%** mediante `Ctrl + +`.
2. Verificar que:
   - Todo el contenido y controles se reorganicen en una sola columna vertical.
   - **No exista barra de desplazamiento horizontal** para leer el texto principal ni para acceder a los botones.
   - Los campos de formulario, botones y opciones de radio conserven un tamaño mínimo táctil de 48px de altura.
   - Las transcripciones y certificados sigan siendo legibles sin recortes ni superposiciones de texto.

---

## 3. Protocolo de Lector de Pantalla (NVDA / JAWS / VoiceOver)

### Configuración:
- **NVDA (Windows):** Versión 2023.1 o superior con voz en español (OneCore o Espeak).
- **Modo de navegación:** Navegación por párrafos (`P`), encabezados (`H`), botones (`B`), campos de formulario (`F`) y regiones (`D`).

### Verificaciones Obligatorias:
1. **Identificación de idioma (Criterio 3.1.1):** `<html lang="es">` declarado en la raíz para que el sintetizador de voz pronuncie correctamente en español.
2. **Jerarquía semántica (Criterio 1.3.1):** Exactamente un solo `<h1>` por pantalla que sintetiza el objetivo de la página, seguido de `<h2>` y `<h3>` sin saltos de nivel.
3. **Etiquetado explícito de controles (Criterio 4.1.2):**
   - Todo campo `<input>` cuenta con un `<label for="...">` asociado explícitamente por ID.
   - Los grupos de radio buttons y casillas de verificación se encuentran contenidos en un `<fieldset>` con un `<legend>` que enuncia la pregunta o categoría.
4. **Retroalimentación dinámica (Criterio 4.1.3):**
   - Los errores de validación se anuncian inmediatamente gracias a regiones `role="alert"`.
   - Los mensajes de éxito (p. ej. "Matrícula confirmada", "Preferencias guardadas") se anuncian mediante `aria-live="polite"`.
5. **Reproducción de audio nativa (ADR-05):**
   - El elemento `<audio controls>` es accesible con atajos nativos del lector (`Espacio` para reproducir/pausar, flechas para avanzar/retroceder segundos).

---

## 4. Matriz de Evaluación de las 6 Pantallas Implementadas

| Pantalla | URL | Criterios Clave WCAG 2.1 AA | Estado | Observaciones |
|---|---|---|---|---|
| **1. Registro y Perfil** | `/registro` y `/perfil` | 1.3.1 (Info y relaciones), 2.4.6 (Encabezados), 3.3.1 (Identificación de errores), 3.3.2 (Etiquetas) | **CUMPLE** | Casilla obligatoria de tratamiento de datos (Ley 1581) con fecha; resumen de errores con `role="alert"`; preferencias guardadas en servidor y sesión. |
| **2. Catálogo de cursos** | `/cursos` | 1.3.1 (Listas semánticas), 2.4.3 (Orden del foco), 2.4.4 (Propósito del enlace) | **CUMPLE** | Estructura en lista vertical limpia en una columna; buscador accesible con label; botones de matricular/continuar con texto descriptivo; sin carruseles ni grids. |
| **3. Matrícula confirmada** | `/matriculas/{id}/confirmacion` | 1.3.1 (Estructura ordenada), 2.4.7 (Foco visible), 4.1.3 (Mensajes de estado) | **CUMPLE** | Alerta de confirmación en `aria-live`; resumen del curso y lista ordenada de próximos pasos; botón principal claro "Comenzar curso". |
| **4. Lección accesible** | `/cursos/{id}/lecciones/{n}` | 1.2.1 (Solo audio), 1.2.2 (Transcripción), 2.4.8 (Ubicación/Progreso) | **CUMPLE** | Progreso en texto ("Lección 1 de 3"); control nativo `<audio controls>` (ADR-05); transcripción completa en texto; enlace de descarga en `.txt`; control de versión exacta (ADR-08). |
| **5. Evaluación** | `/matriculas/{id}/evaluacion` | 1.3.1 (Fieldset/Legend), 2.2.1 (Tiempo ajustable / sin límite arbitrario), 3.3.2 (Instrucciones) | **CUMPLE** | Una pregunta a la vez con indicador textual ("Pregunta 2 de 5"); radios grandes accesibles; validación previa de 100% de lecciones completadas; reintento accesible si reprueba. |
| **6. Certificado oficial** | `/certificados/{id}` | 1.1.1 (Contenido no textual), 1.4.3 (Contraste 7:1), 2.4.4 (Propósito de enlaces) | **CUMPLE** | Renderizado en texto HTML real (nunca imagen, ADR-06); código impredecible generado con `SecureRandom`; descarga en `.txt`; verificación pública vinculada. |
