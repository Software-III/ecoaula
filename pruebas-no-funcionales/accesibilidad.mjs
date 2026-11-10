// PNF-12 / PNF-06: recorre las pantallas de la plataforma desplegada y ejecuta axe-core (WCAG 2.0/2.1 A y AA).
// Uso:  npm ci && npx playwright install chromium && node accesibilidad.mjs
// Variables opcionales: ECOAULA_URL, ECOAULA_<ROL>_CORREO y ECOAULA_<ROL>_CLAVE (rol = APRENDIZ, GESTOR, REVISOR, COORDINADOR).
// Sale con código 1 si alguna pantalla tiene violaciones "critical" o "serious".
import { AxeBuilder } from '@axe-core/playwright';
import { writeFileSync, mkdirSync } from 'node:fs';
import { BASE_URL, CUENTAS, abrirNavegador, iniciarSesion, enlacesInternos } from './lib.mjs';

const MAX_PAGINAS_POR_ROL = Number(process.env.ECOAULA_MAX_PAGINAS || 25);
const PUBLICAS = ['/login', '/registro', '/politica-datos', '/verificar', '/ruta-que-no-existe'];
const ETIQUETAS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'];

const navegador = await abrirNavegador();
const resultados = [];

async function auditar(pagina, ruta, rol) {
  const respuesta = await pagina.goto(`${BASE_URL}${ruta}`);
  const estado = respuesta ? respuesta.status() : 0;
  const axe = await new AxeBuilder({ page: pagina }).withTags(ETIQUETAS).analyze();
  const graves = axe.violations.filter((v) => v.impact === 'critical' || v.impact === 'serious');
  resultados.push({
    rol, ruta, estado,
    violaciones: axe.violations.map((v) => ({ id: v.id, impacto: v.impact, criterio: v.tags.filter((t) => /^wcag\d/.test(t)), nodos: v.nodes.length, ayuda: v.help })),
    graves: graves.length,
  });
  console.log(`${graves.length === 0 ? 'OK  ' : 'FALLA'} [${rol}] ${ruta} (HTTP ${estado}) violaciones: ${axe.violations.length}, críticas/serias: ${graves.length}`);
}

// 1. Pantallas públicas (incluye la página de error 404)
{
  const contexto = await navegador.newContext();
  const pagina = await contexto.newPage();
  for (const ruta of PUBLICAS) await auditar(pagina, ruta, 'anonimo');
  await contexto.close();
}

// 2. Pantallas de cada rol: se parte de su inicio y se siguen los enlaces internos de primer nivel
for (const cuenta of CUENTAS) {
  if (!cuenta.correo || !cuenta.clave) { console.log(`OMITIDO [${cuenta.rol}] sin credenciales en el entorno`); continue; }
  const contexto = await navegador.newContext();
  const pagina = await iniciarSesion(contexto, cuenta);
  const pendientes = [cuenta.inicio];
  const hechas = new Set();
  while (pendientes.length && hechas.size < MAX_PAGINAS_POR_ROL) {
    const ruta = pendientes.shift();
    if (hechas.has(ruta)) continue;
    hechas.add(ruta);
    await auditar(pagina, ruta, cuenta.rol);
    if (hechas.size === 1 || hechas.size < 8) {
      for (const e of await enlacesInternos(pagina)) if (!hechas.has(e)) pendientes.push(e);
    }
  }
  await contexto.close();
}
await navegador.close();

const total = resultados.length;
const sinGraves = resultados.filter((r) => r.graves === 0).length;
const porcentaje = total ? ((sinGraves / total) * 100).toFixed(1) : '0.0';
console.log(`\nPantallas auditadas: ${total}. Sin errores críticos ni serios: ${sinGraves} (${porcentaje} %).`);
mkdirSync('resultados', { recursive: true });
writeFileSync('resultados/accesibilidad.json', JSON.stringify({ fecha: new Date().toISOString(), baseUrl: BASE_URL, total, sinGraves, porcentaje, resultados }, null, 2));
process.exit(sinGraves === total ? 0 : 1);
