# Pruebas no funcionales automatizadas

Se ejecutan sobre el sitio desplegado (o sobre una instancia local con `ECOAULA_URL`). Las credenciales de prueba se pasan por variables de entorno; no se guardan en el repositorio.

```bash
cd pruebas-no-funcionales
npm install
npx playwright install chromium

# PNF-12 y PNF-06: axe-core (WCAG 2.1 A/AA) en las pantallas públicas y de cada rol
ECOAULA_GESTOR_CORREO=... ECOAULA_GESTOR_CLAVE=... \
ECOAULA_REVISOR_CORREO=... ECOAULA_REVISOR_CLAVE=... \
ECOAULA_APRENDIZ_CORREO=... ECOAULA_APRENDIZ_CLAVE=... \
ECOAULA_COORDINADOR_CORREO=... ECOAULA_COORDINADOR_CLAVE=... \
npm run accesibilidad

# PNF-16: las lecciones de cursos publicados cargan con audio y transcripción
ECOAULA_APRENDIZ_CORREO=... ECOAULA_APRENDIZ_CLAVE=... npm run contenido-publicado
```

Cada script escribe su informe en `resultados/*.json` (se adjunta como evidencia) y termina con código 1 si falla.
Lighthouse (puntaje de accesibilidad por pantalla): `npx lighthouse <URL> --only-categories=accessibility --output=json`.
