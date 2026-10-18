package com.ecoaula.config;

import com.ecoaula.model.Contenido;
import com.ecoaula.model.ContenidoVersion;
import com.ecoaula.model.Curso;
import com.ecoaula.model.Evaluacion;
import com.ecoaula.model.OpcionPregunta;
import com.ecoaula.model.Pregunta;
import com.ecoaula.model.enums.CriterioAccesibilidad;
import com.ecoaula.model.enums.EstadoContenido;
import com.ecoaula.repository.AprendizRepository;
import com.ecoaula.repository.ContenidoRepository;
import com.ecoaula.repository.ContenidoVersionRepository;
import com.ecoaula.repository.CursoRepository;
import com.ecoaula.repository.EvaluacionRepository;
import com.ecoaula.repository.OpcionPreguntaRepository;
import com.ecoaula.repository.PreguntaRepository;
import com.ecoaula.service.ContenidoService;
import com.ecoaula.service.GestionContenidoService;
import com.ecoaula.service.RevisionContenidoService;
import com.ecoaula.storage.AudioDemoGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Datos de prueba del Proceso 2 (perfiles dev y dev-h2) para validar el flujo del gestor y del revisor:
 * <ul>
 *   <li>Un curso con borradores pendientes de enviar a revisión.</li>
 *   <li>Un curso con versiones en revisión, una de ellas subsanación de una versión rechazada.</li>
 *   <li>Un curso revisado y con evaluación, listo para publicar en el catálogo.</li>
 *   <li>Una nueva versión en revisión de una lección de un curso ya publicado.</li>
 * </ul>
 * Se construyen con los servicios de la aplicación, así que respetan sus reglas (licencia, checklist,
 * separación de funciones, subsanación inmutable). Cada bloque se crea solo si no existe.
 */
@Component
@Profile({"dev", "dev-h2"})
@Order(2)
public class DatosPruebaRevisionInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosPruebaRevisionInitializer.class);

    private static final String GESTOR = "gestor@ecoaula.local";
    private static final String REVISOR = "revisor@ecoaula.local";
    private static final String FUENTE = "Elaboración propia del equipo EcoAula (contenido de prueba).";

    static final String CURSO_POR_ENVIAR = "Prueba: Hojas de cálculo accesibles";
    static final String CURSO_EN_REVISION = "Prueba: Correo electrónico accesible";
    static final String CURSO_POR_PUBLICAR = "Prueba: Inglés para el trabajo";

    private final GestionContenidoService gestionService;
    private final RevisionContenidoService revisionService;
    private final ContenidoService contenidoService;
    private final AprendizRepository aprendizRepository;
    private final CursoRepository cursoRepository;
    private final ContenidoRepository contenidoRepository;
    private final ContenidoVersionRepository versionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionPreguntaRepository opcionRepository;

    private final byte[] audio = AudioDemoGenerator.generarTonoSuaveWav(4, 523.25);

    public DatosPruebaRevisionInitializer(GestionContenidoService gestionService,
                                          RevisionContenidoService revisionService,
                                          ContenidoService contenidoService,
                                          AprendizRepository aprendizRepository,
                                          CursoRepository cursoRepository,
                                          ContenidoRepository contenidoRepository,
                                          ContenidoVersionRepository versionRepository,
                                          EvaluacionRepository evaluacionRepository,
                                          PreguntaRepository preguntaRepository,
                                          OpcionPreguntaRepository opcionRepository) {
        this.gestionService = gestionService;
        this.revisionService = revisionService;
        this.contenidoService = contenidoService;
        this.aprendizRepository = aprendizRepository;
        this.cursoRepository = cursoRepository;
        this.contenidoRepository = contenidoRepository;
        this.versionRepository = versionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!aprendizRepository.existsByCorreo(GESTOR) || !aprendizRepository.existsByCorreo(REVISOR)) {
            log.info("Datos de prueba del Proceso 2 omitidos: no existen el gestor y el revisor de demostración.");
            return;
        }
        if (!existeCurso(CURSO_POR_ENVIAR)) {
            crearCursoPendienteDeEnviar();
        }
        if (!existeCurso(CURSO_EN_REVISION)) {
            crearCursoEnRevision();
        }
        if (!existeCurso(CURSO_POR_PUBLICAR)) {
            crearCursoRevisadoPorPublicar();
        }
        crearActualizacionDeCursoPublicado();
    }

    /** Borradores sin enviar: uno listo, uno sin declarar licencia y una lección con su borrador vacío. */
    private void crearCursoPendienteDeEnviar() {
        Curso curso = gestionService.crearCurso(CURSO_POR_ENVIAR,
                "Uso de hojas de cálculo con lector de pantalla: celdas, fórmulas y gráficos descritos en texto.",
                12, "Ofimática", "Virtual accesible", null);

        ContenidoVersion celdas = primeraVersion(gestionService.agregarLeccion(curso.getId(),
                "Celdas, filas y columnas con lector de pantalla", null, GESTOR));
        guardar(celdas, "Una hoja de cálculo se organiza en filas numeradas y columnas identificadas con letras. "
                + "Cada celda se nombra con la letra de su columna y el número de su fila, por ejemplo B3. "
                + "Con las flechas del teclado se recorre la hoja y el lector de pantalla anuncia la celda y su contenido.", true);
        contenidoService.agregarImagen(celdas.getId(), GESTOR, "hoja.png", imagen(new Color(0, 64, 133)),
                "Hoja de cálculo con la celda B3 seleccionada",
                "La celda B3 está en la columna B, segunda de izquierda a derecha, y en la fila 3. Contiene el valor 1500.");

        ContenidoVersion formulas = primeraVersion(gestionService.agregarLeccion(curso.getId(),
                "Fórmulas básicas: suma y promedio", null, GESTOR));
        guardar(formulas, "Toda fórmula comienza con el signo igual. La función SUMA agrega los valores de un rango, "
                + "por ejemplo =SUMA(B2:B6). La función PROMEDIO calcula la media del mismo rango.", false);

        gestionService.agregarLeccion(curso.getId(), "Gráficos accesibles", null, GESTOR);
        log.info("Datos de prueba: curso '{}' con borradores pendientes de enviar a revisión.", CURSO_POR_ENVIAR);
    }

    /** Dos versiones esperando al revisor; la segunda es la subsanación de una versión rechazada. */
    private void crearCursoEnRevision() {
        Curso curso = gestionService.crearCurso(CURSO_EN_REVISION,
                "Redacción, envío y organización del correo electrónico usando solo el teclado.",
                8, "Comunicación digital", "Virtual accesible", null);

        ContenidoVersion redactar = primeraVersion(gestionService.agregarLeccion(curso.getId(),
                "Redactar y enviar un correo con el teclado", null, GESTOR));
        guardar(redactar, "Para redactar un correo nuevo use el atajo Control más N. Escriba el destinatario, pulse Tabulador para "
                + "pasar al asunto y de nuevo Tabulador para el cuerpo del mensaje. Envíelo con Control más Intro.", true);
        contenidoService.agregarImagen(redactar.getId(), GESTOR, "correo.png", imagen(new Color(21, 87, 36)),
                "Ventana de correo nuevo con los campos Para, Asunto y Mensaje", null);
        gestionService.enviarARevision(redactar.getId(), GESTOR);

        ContenidoVersion adjuntos = primeraVersion(gestionService.agregarLeccion(curso.getId(),
                "Adjuntar archivos y organizar la bandeja", null, GESTOR));
        guardar(adjuntos, "Para adjuntar un archivo haga clic en el clip verde de la barra superior.", true);
        gestionService.enviarARevision(adjuntos.getId(), GESTOR);
        revisionService.rechazar(adjuntos.getId(), REVISOR,
                "La instrucción depende de la forma, el color y la ubicación visual del botón (WCAG 1.3.3). "
                        + "Indique el atajo de teclado y el nombre accesible del botón.",
                criteriosExcepto(CriterioAccesibilidad.SIN_SOLO_VISUAL));
        ContenidoVersion subsanacion = gestionService.crearVersionDesdeRechazada(adjuntos.getId(), GESTOR);
        guardar(subsanacion, "Para adjuntar un archivo use el botón Adjuntar archivo o el atajo Control más Mayúscula más A. "
                + "Se abre el explorador de archivos: elija el documento con las flechas y pulse Intro.", true);
        gestionService.enviarARevision(subsanacion.getId(), GESTOR);

        log.info("Datos de prueba: curso '{}' con versiones pendientes del revisor.", CURSO_EN_REVISION);
    }

    /** Lecciones aprobadas (una en primera revisión, otra tras una subsanación) y evaluación configurada. */
    private void crearCursoRevisadoPorPublicar() {
        Curso curso = gestionService.crearCurso(CURSO_POR_PUBLICAR,
                "Vocabulario y expresiones de inglés instrumental para el entorno laboral.",
                16, "Idiomas", "Virtual accesible", null);

        ContenidoVersion saludos = primeraVersion(gestionService.agregarLeccion(curso.getId(), "Saludos y presentaciones", null, GESTOR));
        guardar(saludos, "Hello significa hola. My name is significa mi nombre es. Nice to meet you significa mucho gusto.", true);
        gestionService.enviarARevision(saludos.getId(), GESTOR);
        revisionService.aprobarYPublicar(saludos.getId(), REVISOR, todosLosCriterios());

        ContenidoVersion oficina = primeraVersion(gestionService.agregarLeccion(curso.getId(), "Vocabulario de oficina", null, GESTOR));
        guardar(oficina, "Desk es escritorio y meeting es reunión.", true);
        gestionService.enviarARevision(oficina.getId(), GESTOR);
        revisionService.rechazar(oficina.getId(), REVISOR,
                "Los términos en inglés no están señalados ni explicados para el lector de pantalla (WCAG 3.1.2).",
                criteriosExcepto(CriterioAccesibilidad.IDIOMA_ESPANOL));
        ContenidoVersion corregida = gestionService.crearVersionDesdeRechazada(oficina.getId(), GESTOR);
        guardar(corregida, "La palabra en inglés desk, que se escribe d, e, s, k, significa escritorio. "
                + "La palabra en inglés meeting significa reunión.", true);
        gestionService.enviarARevision(corregida.getId(), GESTOR);
        revisionService.aprobarYPublicar(corregida.getId(), REVISOR, todosLosCriterios());

        Evaluacion evaluacion = evaluacionRepository.save(new Evaluacion(curso));
        crearPregunta(evaluacion, 1, "¿Qué significa la expresión Nice to meet you?", "Mucho gusto", "Hasta luego", "Buenos días", "Gracias");
        crearPregunta(evaluacion, 2, "¿Cómo se dice escritorio en inglés?", "Desk", "Meeting", "Chair", "Window");
        crearPregunta(evaluacion, 3, "¿Qué significa la palabra en inglés meeting?", "Reunión", "Oficina", "Almuerzo", "Correo");

        log.info("Datos de prueba: curso '{}' revisado y listo para publicar en el catálogo.", CURSO_POR_PUBLICAR);
    }

    /** Una nueva versión en revisión de una lección de un curso ya publicado: el revisor la compara con la vigente. */
    private void crearActualizacionDeCursoPublicado() {
        cursoRepository.findAll().stream()
                .filter(c -> c.getTitulo().contains("Atención al Cliente"))
                .findFirst()
                .flatMap(c -> contenidoRepository.findByCursoIdAndOrden(c.getId(), 2))
                .filter(l -> versionRepository.findByContenidoIdOrderByNumeroVersionDesc(l.getId()).size() == 1)
                .ifPresent(leccion -> {
                    ContenidoVersion nueva = gestionService.crearNuevaVersionBorrador(leccion.getId(), GESTOR);
                    guardar(nueva, "Transcripción actualizada: al ofrecer acompañamiento, pregunte primero si la persona desea ayuda. "
                            + "Si acepta, ofrezca el codo para que lo tome y camine medio paso por delante, "
                            + "anunciando escalones, puertas y cambios de dirección.", true);
                    gestionService.enviarARevision(nueva.getId(), GESTOR);
                    log.info("Datos de prueba: nueva versión en revisión para la lección '{}'.", leccion.getTitulo());
                });
    }

    private ContenidoVersion primeraVersion(Contenido leccion) {
        return versionRepository.findByContenidoIdOrderByNumeroVersionDesc(leccion.getId()).stream()
                .filter(v -> v.getEstado() == EstadoContenido.BORRADOR)
                .findFirst().orElseThrow();
    }

    private void guardar(ContenidoVersion version, String transcripcion, boolean licenciaDeclarada) {
        gestionService.actualizarBorrador(version.getId(), transcripcion,
                version.getRutaAudio() == null ? new ArchivoEnMemoria("leccion.wav", "audio/wav", audio) : null,
                FUENTE, licenciaDeclarada, GESTOR);
    }

    private static Set<String> todosLosCriterios() {
        return CriterioAccesibilidad.todos().stream().map(CriterioAccesibilidad::getCodigo).collect(Collectors.toSet());
    }

    private static Set<String> criteriosExcepto(CriterioAccesibilidad excluido) {
        return CriterioAccesibilidad.todos().stream().filter(c -> c != excluido)
                .map(CriterioAccesibilidad::getCodigo).collect(Collectors.toSet());
    }

    private boolean existeCurso(String titulo) {
        return cursoRepository.findAll().stream().anyMatch(c -> c.getTitulo().equals(titulo));
    }

    private void crearPregunta(Evaluacion evaluacion, int orden, String enunciado,
                               String correcta, String opcion2, String opcion3, String opcion4) {
        Pregunta pregunta = preguntaRepository.save(new Pregunta(evaluacion, enunciado, orden));
        opcionRepository.save(new OpcionPregunta(pregunta, correcta, "Opción 1", true));
        opcionRepository.save(new OpcionPregunta(pregunta, opcion2, "Opción 2", false));
        opcionRepository.save(new OpcionPregunta(pregunta, opcion3, "Opción 3", false));
        opcionRepository.save(new OpcionPregunta(pregunta, opcion4, "Opción 4", false));
    }

    /** Imagen PNG simple generada en memoria para las pruebas. */
    private static byte[] imagen(Color color) {
        BufferedImage img = new BufferedImage(320, 180, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 320, 180);
        g.setColor(color);
        g.fillRect(20, 20, 280, 140);
        g.dispose();
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar la imagen de prueba", e);
        }
    }

    /** Archivo en memoria para usar los mismos servicios que el formulario del gestor. */
    private static final class ArchivoEnMemoria implements MultipartFile {
        private final String nombre;
        private final String tipo;
        private final byte[] datos;

        private ArchivoEnMemoria(String nombre, String tipo, byte[] datos) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.datos = datos;
        }

        @Override
        public String getName() {
            return "archivoAudio";
        }

        @Override
        public String getOriginalFilename() {
            return nombre;
        }

        @Override
        public String getContentType() {
            return tipo;
        }

        @Override
        public boolean isEmpty() {
            return datos.length == 0;
        }

        @Override
        public long getSize() {
            return datos.length;
        }

        @Override
        public byte[] getBytes() {
            return datos;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(datos);
        }

        @Override
        public void transferTo(File destino) throws IOException {
            Files.write(destino.toPath(), datos);
        }
    }
}
