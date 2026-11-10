// Utilidades comunes de las pruebas no funcionales sobre el sitio desplegado.
import { chromium } from 'playwright';

export const BASE_URL = (process.env.ECOAULA_URL || 'https://3-237-185-159.sslip.io').replace(/\/$/, '');

/** Cuentas de prueba: se leen del entorno, nunca del repositorio. Una cuenta sin variables se omite. */
export const CUENTAS = [
  { rol: 'aprendiz', correo: process.env.ECOAULA_APRENDIZ_CORREO, clave: process.env.ECOAULA_APRENDIZ_CLAVE, inicio: '/cursos' },
  { rol: 'gestor', correo: process.env.ECOAULA_GESTOR_CORREO, clave: process.env.ECOAULA_GESTOR_CLAVE, inicio: '/gestor/cursos' },
  { rol: 'revisor', correo: process.env.ECOAULA_REVISOR_CORREO, clave: process.env.ECOAULA_REVISOR_CLAVE, inicio: '/revisor/cola' },
  { rol: 'coordinador', correo: process.env.ECOAULA_COORDINADOR_CORREO, clave: process.env.ECOAULA_COORDINADOR_CLAVE, inicio: '/indicadores' },
];

export async function abrirNavegador() {
  return chromium.launch();
}

export async function iniciarSesion(contexto, cuenta) {
  const pagina = await contexto.newPage();
  await pagina.goto(`${BASE_URL}/login`);
  await pagina.fill('#correo', cuenta.correo);
  await pagina.fill('#password', cuenta.clave);
  await Promise.all([
    pagina.waitForLoadState('load'),
    pagina.click('form[action$="/login"] button[type="submit"]'),
  ]);
  if (pagina.url().includes('/login')) {
    throw new Error(`No se pudo iniciar sesión como ${cuenta.rol}: revise las variables de entorno`);
  }
  return pagina;
}

/** Enlaces internos GET de la página actual, sin cierres de sesión, descargas ni archivos. */
export async function enlacesInternos(pagina) {
  const hrefs = await pagina.$$eval('a[href]', (as) => as.map((a) => a.getAttribute('href')));
  const vistos = new Set();
  for (const h of hrefs) {
    if (!h || h.startsWith('#') || h.startsWith('mailto:') || h.startsWith('javascript:')) continue;
    let url;
    try { url = new URL(h, pagina.url()); } catch { continue; }
    if (url.origin !== new URL(BASE_URL).origin) continue;
    if (/\/(logout|descargar|audio|imagenes|eliminar|retirar)/.test(url.pathname)) continue;
    vistos.add(url.pathname);
  }
  return [...vistos];
}
