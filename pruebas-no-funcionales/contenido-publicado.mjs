// PNF-16: toda lección de un curso publicado abre sin error y su audio responde.
// Requiere una cuenta de aprendiz matriculada en los cursos que se quieran revisar.
// Uso: ECOAULA_APRENDIZ_CORREO=... ECOAULA_APRENDIZ_CLAVE=... node contenido-publicado.mjs
import { writeFileSync, mkdirSync } from 'node:fs';
import { BASE_URL, CUENTAS, abrirNavegador, iniciarSesion } from './lib.mjs';

const cuenta = CUENTAS.find((c) => c.rol === 'aprendiz');
if (!cuenta.correo || !cuenta.clave) { console.error('Defina ECOAULA_APRENDIZ_CORREO y ECOAULA_APRENDIZ_CLAVE'); process.exit(2); }

const navegador = await abrirNavegador();
const contexto = await navegador.newContext();
const pagina = await iniciarSesion(contexto, cuenta);

async function hrefs(patron) {
  const todos = await pagina.$$eval('a[href]', (as) => as.map((a) => a.getAttribute('href')));
  return [...new Set(todos.filter((h) => h && patron.test(h)).map((h) => new URL(h, pagina.url()).pathname))];
}

await pagina.goto(`${BASE_URL}/cursos`);
const cursos = await hrefs(/^\/(cursos|matriculas)\/\d+$/);
const lecciones = [];
for (const curso of cursos) {
  await pagina.goto(`${BASE_URL}${curso}`);
  for (const l of await hrefs(/\/cursos\/\d+\/lecciones\/\d+$/)) lecciones.push({ curso, ruta: l });
  for (const l of await hrefs(/\/cursos\/\d+\/lecciones$/)) {
    await pagina.goto(`${BASE_URL}${l}`);
    for (const x of await hrefs(/\/cursos\/\d+\/lecciones\/\d+$/)) lecciones.push({ curso, ruta: x });
  }
}

const resultados = [];
for (const { curso, ruta } of [...new Map(lecciones.map((l) => [l.ruta, l])).values()]) {
  const respuesta = await pagina.goto(`${BASE_URL}${ruta}`);
  const estado = respuesta ? respuesta.status() : 0;
  const fuentes = await pagina.$$eval('audio source, audio[src]', (els) => els.map((e) => e.getAttribute('src')).filter(Boolean));
  const estadoAudio = [];
  for (const f of fuentes) {
    const r = await contexto.request.get(new URL(f, pagina.url()).toString(), { headers: { Range: 'bytes=0-1' } });
    estadoAudio.push(r.status());
  }
  const transcripcion = await pagina.locator('main').innerText().then((t) => t.trim().length > 0).catch(() => false);
  const ok = estado === 200 && fuentes.length > 0 && estadoAudio.every((s) => s === 200 || s === 206) && transcripcion;
  resultados.push({ curso, ruta, estado, audios: estadoAudio, transcripcion, ok });
  console.log(`${ok ? 'OK  ' : 'FALLA'} ${ruta} (HTTP ${estado}, audio ${estadoAudio.join(',') || 'sin audio'})`);
}
await navegador.close();

const total = resultados.length;
const bien = resultados.filter((r) => r.ok).length;
console.log(`\nLecciones revisadas: ${total}. Correctas: ${bien} (${total ? ((bien / total) * 100).toFixed(1) : '0.0'} %).`);
mkdirSync('resultados', { recursive: true });
writeFileSync('resultados/contenido-publicado.json', JSON.stringify({ fecha: new Date().toISOString(), baseUrl: BASE_URL, total, correctas: bien, resultados }, null, 2));
process.exit(total > 0 && bien === total ? 0 : 1);
