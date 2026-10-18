# Calidad en el pipeline — Cobertura y SonarCloud

> Relación con el Plan de Gestión de la Calidad: métrica **Cobertura de pruebas** (sección 3.3, medida en cada Pull Request) y requisitos **RNF-10** (módulo de certificación) y **RNF-11** (módulo de matrícula).

## 1. Qué valida el pipeline (`.github/workflows/ci.yml`)

| Paso | Qué valida | Falla cuando |
|---|---|---|
| `./mvnw verify` | Compilación, pruebas unitarias y de integración (H2) | Una prueba falla |
| `jacoco:check` (dentro de `verify`) | Umbrales de cobertura del `pom.xml` | La cobertura baja de los umbrales (sección 2) |
| Análisis SonarCloud | Bugs, vulnerabilidades, *hotspots*, *code smells*, duplicación y cobertura | No se pasa el Quality Gate (sección 3) |
| Job `integracion-mysql` | Las mismas pruebas más el recorrido completo contra MySQL 8 | Una prueba falla |

El análisis de SonarCloud solo corre si existe el secreto `SONAR_TOKEN`; mientras no se configure, el pipeline sigue funcionando y deja un aviso en el resumen de la ejecución.

## 2. Umbrales de cobertura (JaCoCo)

Definidos como propiedades en `pom.xml` (`cobertura.minima.*`):

| Alcance | Líneas | Ramas | Valor al configurarlos |
|---|---|---|---|
| Todo el proyecto | ≥ 80 % | ≥ 60 % | 85,6 % / 65,7 % |
| `MatriculaService` (RNF-11) | ≥ 85 % | ≥ 60 % | 91,2 % / 100 % |
| `EvaluacionService` y `CertificacionService` (RNF-10) | ≥ 85 % | ≥ 60 % | 90,9 % / 66,7 % y 95,5 % / 75 % |

Se excluyen de la medición los datos de demostración (`DataInitializer`, `DatosPruebaRevisionInitializer`) y la clase de arranque, porque se ejecutan en cada prueba y no son lógica a probar.

Comprobarlo en local:

```bash
./mvnw verify                      # incluye jacoco:check
xdg-open target/site/jacoco/index.html   # reporte detallado (en macOS: open)
```

Para subir los umbrales a medida que crezcan las pruebas, basta con cambiar las propiedades del `pom.xml`.

## 3. SonarCloud (pasos de configuración, una sola vez)

SonarCloud es gratuito para repositorios públicos.

1. Entrar a <https://sonarcloud.io> con la cuenta de GitHub que administra la organización `Software-III`.
2. **+ → Analyze new project** → importar la organización `Software-III` y seleccionar el repositorio `ecoaula`.
3. En *Administration → Analysis Method*, **desactivar *Automatic Analysis*** (el análisis lo hace el pipeline, con la cobertura).
4. Verificar que la **organización** y la **clave del proyecto** coincidan con las del `pom.xml` (`sonar.organization=software-iii`, `sonar.projectKey=Software-III_ecoaula`). Si SonarCloud asignó otras, actualizar esas dos propiedades.
5. En *My Account → Security*, generar un token.
6. En GitHub: *Settings → Secrets and variables → Actions → New repository secret*, nombre **`SONAR_TOKEN`**, valor: el token.
7. (Recomendado) En *Administration → New Code*, elegir **Previous version** o **Number of days**, y mantener el Quality Gate **Sonar way**.

A partir del siguiente *push*, el job `build` analiza el código y espera el resultado del Quality Gate (`-Dsonar.qualitygate.wait=true`): si no se cumple, el pipeline falla. En los Pull Request, SonarCloud comenta los hallazgos.

### Quality Gate "Sonar way" (código nuevo)

- Cobertura ≥ 80 %
- Líneas duplicadas ≤ 3 %
- Calificación A en fiabilidad, seguridad y mantenibilidad
- 100 % de los *security hotspots* revisados

## 4. Estado del primer análisis (verificado con SonarQube local)

| Métrica | Resultado |
|---|---|
| Bugs | 0 (fiabilidad **A**) — se corrigieron 3 que encontró el análisis |
| Vulnerabilidades / *hotspots* | 0 / 0 (seguridad **A**) |
| *Code smells* | 63 (mantenibilidad **A**) |
| Cobertura | 81,4 % (Sonar combina líneas y ramas) |
| Duplicación | 3,1 % |

*Code smells* más frecuentes, para ir limpiando: `java:S1192` (31, literales repetidos, en su mayoría mensajes y rutas) y `java:S5778` (13, más de una llamada que puede lanzar excepción dentro de `assertThrows` en pruebas). La duplicación total (3,1 %) está cerca del límite del 3 % que el Quality Gate aplica al código nuevo: conviene no copiar bloques al agregar vistas o pruebas.

## 5. Análisis local opcional (sin nube)

```bash
docker run -d --rm --name sonarqube -e SONAR_ES_BOOTSTRAP_CHECKS_DISABLE=true -p 9000:9000 sonarqube:lts-community
# En http://localhost:9000 (admin/admin): cambiar la contraseña y generar un token
./mvnw verify org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
  -Dsonar.host.url=http://localhost:9000 -Dsonar.login=<token> -Dsonar.organization=
```
