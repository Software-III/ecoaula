# Modelo de datos — EcoAula (v1)

*Ticket KAN-32 
Base de datos: MySQL (ADR-04). Esquema versionado con migraciones (KAN-34).*

!\[Diagrama entidad-relación](modelo-datos.png)

*Fuente editable del diagrama:* [*`modelo-datos-ecoaula.drawio`*](modelo-datos.drawio) *(se abre en app.diagrams.net o con la extensión Draw.io de VS Code).*

\---

## 1\. Entidades y atributos

Convenciones: ids `BIGINT` autoincrementales; fechas `DATETIME`; estados como `VARCHAR` validado por enumeración en Java.

### Usuario  *(E1)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|email|VARCHAR(150)|UNIQUE, NOT NULL|Identificador de acceso|
|password\_hash|VARCHAR(100)|NOT NULL|Hash (bcrypt), nunca texto plano|
|nombre\_completo|VARCHAR(150)|NOT NULL||
|rol|VARCHAR(30)|NOT NULL|`APRENDIZ`, `COORDINADOR\_MATRICULA`, `GESTOR\_CONTENIDO`, `REVISOR\_ACCESIBILIDAD`, `TUTOR`|
|activo|BOOLEAN|NOT NULL, default true||

### Aprendiz  *(E1)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|usuario\_id|BIGINT|FK → Usuario, UNIQUE||
|condicion\_discapacidad\_cifrada|VARBINARY(255)|NULL|**Dato sensible cifrado en reposo** (KAN-38, Ley 1581/2012)|
|consentimiento\_aceptado|BOOLEAN|NOT NULL|Autorización expresa antes de guardar el dato|
|fecha\_consentimiento|DATETIME|NULL||

### PerfilAccesibilidad  *(E1)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|aprendiz\_id|BIGINT|FK → Aprendiz, UNIQUE|Se guarda por **cuenta**, no por sesión (ADR-07)|
|voz|VARCHAR(50)|NULL|Voz sintética preferida|
|velocidad\_lectura|DECIMAL(3,2)|NOT NULL, default 1.00|0.75 / 1.00 / 1.25 / 1.50 (según mockup)|
|contraste|VARCHAR(20)|NOT NULL, default `ESTANDAR`|`ESTANDAR`, `ALTO\_INVERTIDO`, `MONOCROMATICO`|
|tamano\_fuente\_px|INT|NOT NULL, default 18|18 / 22 / 26|
|modo\_solo\_audio|BOOLEAN|NOT NULL, default false||

### Curso  *(E2)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|nombre|VARCHAR(150)|NOT NULL||
|area|VARCHAR(80)|NOT NULL||
|duracion\_horas|INT|NOT NULL||
|descripcion|TEXT|NOT NULL|Descripción textual (KAN-16)|
|cupo\_maximo|INT|NOT NULL|KAN-17 valida cupo|
|estado|VARCHAR(20)|NOT NULL, default `BORRADOR`|`BORRADOR`, `PUBLICADO` — el catálogo solo lista `PUBLICADO`|

### Leccion  *(E2)*

|Campo|Tipo|Restricción|
|-|-|-|
|id|BIGINT|PK|
|curso\_id|BIGINT|FK → Curso|
|titulo|VARCHAR(150)|NOT NULL|
|orden|INT|NOT NULL — UNIQUE (curso\_id, orden)|

### Contenido  *(E2)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|leccion\_id|BIGINT|FK → Leccion||
|tipo|VARCHAR(20)|NOT NULL|`AUDIO`, `TEXTO`, `IMAGEN`|

### VersionContenido  *(E2 · ADR-08)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|contenido\_id|BIGINT|FK → Contenido||
|numero\_version|INT|NOT NULL — UNIQUE (contenido\_id, numero\_version)|Cada cambio crea una versión nueva; **nunca se sobrescribe**|
|estado|VARCHAR(20)|NOT NULL, default `BORRADOR`|`BORRADOR`, `EN\_REVISION`, `PUBLICADA`, `RECHAZADA`, `REEMPLAZADA`|
|archivo\_ref|VARCHAR(255)|NULL|Clave/URL en el almacenamiento de archivos (depende de N3)|
|texto|TEXT|NULL|Cuerpo si el contenido es texto|
|transcripcion|TEXT|NULL|**Obligatoria** para `AUDIO` antes de pasar a revisión (KAN-20)|
|texto\_alternativo|VARCHAR(500)|NULL|**Obligatorio** para `IMAGEN` antes de pasar a revisión (KAN-20)|
|autor\_id|BIGINT|FK → Usuario|Gestor de contenido|
|revisor\_id|BIGINT|FK → Usuario, NULL|Revisor de accesibilidad|
|checklist\_resultado|JSON|NULL|Resultado de la revisión WCAG/NTC 5854|
|observaciones\_revision|TEXT|NULL||
|fecha\_creacion / fecha\_revision|DATETIME|||

### Matricula  *(E1)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|aprendiz\_id|BIGINT|FK → Aprendiz||
|curso\_id|BIGINT|FK → Curso|UNIQUE (aprendiz\_id, curso\_id): no se matricula dos veces|
|estado|VARCHAR(20)|NOT NULL|`ACTIVA`, `CANCELADA`, `COMPLETADA`|
|fecha\_matricula|DATETIME|NOT NULL||
|observacion\_correccion|VARCHAR(255)|NULL|Correcciones de la coordinadora (KAN-18)|

### AvanceLeccion  *(E2)*

|Campo|Tipo|Restricción|
|-|-|-|
|id|BIGINT|PK|
|matricula\_id|BIGINT|FK → Matricula|
|leccion\_id|BIGINT|FK → Leccion — UNIQUE (matricula\_id, leccion\_id)|
|completada|BOOLEAN|NOT NULL, default false|
|fecha\_completada|DATETIME|NULL|

El porcentaje de avance **no se guarda**: se calcula (lecciones completadas / lecciones del curso).

### Evaluacion  *(E3)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|curso\_id|BIGINT|FK → Curso, UNIQUE|Una evaluación final por curso (v1)|
|puntaje\_minimo|INT|NOT NULL|Umbral de aprobación|
|tiempo\_estandar\_min|INT|NOT NULL||
|porcentaje\_tiempo\_extra|INT|NOT NULL, default 50|Tiempo extendido **configurable por curso** (KAN-25)|

### Pregunta / OpcionPregunta  *(E3)*

|Pregunta|Tipo|Restricción|
|-|-|-|
|id|BIGINT|PK|
|evaluacion\_id|BIGINT|FK → Evaluacion|
|enunciado|TEXT|NOT NULL — solo texto: sin imágenes ni arrastrar y soltar (KAN-24)|
|orden|INT|NOT NULL|

|OpcionPregunta|Tipo|Restricción|
|-|-|-|
|id|BIGINT|PK|
|pregunta\_id|BIGINT|FK → Pregunta|
|texto|VARCHAR(300)|NOT NULL|
|es\_correcta|BOOLEAN|NOT NULL|

### IntentoEvaluacion  *(E3)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|matricula\_id|BIGINT|FK → Matricula|UNIQUE (matricula\_id, numero\_intento)|
|evaluacion\_id|BIGINT|FK → Evaluacion||
|numero\_intento|INT|NOT NULL||
|fecha\_inicio / fecha\_fin|DATETIME|||
|tiempo\_limite\_min|INT|NOT NULL|Se guarda ya con la extensión aplicada|
|calificacion|DECIMAL(5,2)|NULL||
|aprobado|BOOLEAN|NULL||

### Respuesta  *(E3)*

|Campo|Tipo|Restricción|
|-|-|-|
|id|BIGINT|PK|
|intento\_id|BIGINT|FK → IntentoEvaluacion|
|pregunta\_id|BIGINT|FK → Pregunta — UNIQUE (intento\_id, pregunta\_id)|
|opcion\_id|BIGINT|FK → OpcionPregunta|

### Certificado  *(E3 · ADR-06)*

|Campo|Tipo|Restricción|Nota|
|-|-|-|-|
|id|BIGINT|PK||
|matricula\_id|BIGINT|FK → Matricula, UNIQUE|Un certificado por matrícula|
|intento\_id|BIGINT|FK → IntentoEvaluacion|El intento aprobado|
|codigo\_verificacion|VARCHAR(40)|UNIQUE, NOT NULL|**No predecible ni secuencial** (KAN-28): aleatorio de 128 bits|
|fecha\_emision|DATETIME|NOT NULL||
|estado|VARCHAR(20)|NOT NULL, default `VIGENTE`|`VIGENTE`, `REVOCADO`|

La **API pública de verificación** consulta esta tabla por `codigo\_verificacion` y expone **solo** nombre del aprendiz, curso, fecha y estado (nada más).

\---

## 2\. Reglas que NO puede garantizar la base de datos (van en los servicios)

MySQL no tiene índices únicos parciales ni reglas entre tablas, así que estas se validan en Java:

1. **Máximo una `VersionContenido` en `PUBLICADA` por contenido.** Al publicar una nueva, la anterior pasa a `REEMPLAZADA` (en una misma transacción).
2. Una versión no pasa a `EN\_REVISION` si es `AUDIO` sin `transcripcion` o `IMAGEN` sin `texto\_alternativo` (KAN-20).
3. Un `Curso` solo pasa a `PUBLICADO` si **cada lección** tiene al menos un contenido con versión `PUBLICADA`.
4. **Cupo:** la matrícula se rechaza si las `ACTIVA` ya igualan `cupo\_maximo` (KAN-17).
5. **Evaluación habilitada** ⇔ para esa matrícula, todas las lecciones del curso tienen `completada = true` (KAN-27).
6. Solo se crea un `Certificado` si el `IntentoEvaluacion` tiene `aprobado = true`.

\---

## 3\. Los 3 contratos entre módulos

|Contrato|De → a|Qué se pacta|
|-|-|-|
|**Curso publicado**|E2 → E1|El catálogo (KAN-16) lee `Curso.estado = PUBLICADO` y sus `Leccion`.|
|**Matrícula y avance**|E1 + E2 → E3|La regla 5 (KAN-27) usa `Matricula` y `AvanceLeccion`.|
|**Perfil de accesibilidad**|E1 → todos|La plantilla base (N1) carga `PerfilAccesibilidad` del usuario antes de renderizar.|



