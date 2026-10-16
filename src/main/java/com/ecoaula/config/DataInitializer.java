package com.ecoaula.config;

import com.ecoaula.model.*;
import com.ecoaula.model.enums.*;
import com.ecoaula.repository.*;
import com.ecoaula.storage.AlmacenamientoContenido;
import com.ecoaula.storage.AudioDemoGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Inicializador de datos de demostración para perfiles de desarrollo (dev, dev-h2).
 * Presenta todos los cursos y contenidos explícitamente como datos de demostración.
 */
@Component
@Profile({"dev", "dev-h2", "prod"})
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AprendizRepository aprendizRepository;
    private final CursoRepository cursoRepository;
    private final ContenidoRepository contenidoRepository;
    private final ContenidoVersionRepository versionRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final PreguntaRepository preguntaRepository;
    private final OpcionPreguntaRepository opcionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlmacenamientoContenido almacenamientoContenido;

    @org.springframework.beans.factory.annotation.Value("${ecoaula.seed.aprendiz-password:EcoAula2026!}")
    private String passwordAprendiz;

    @org.springframework.beans.factory.annotation.Value("${ecoaula.seed.gestor-password:GestorEcoAula2026!}")
    private String passwordGestor;

    @org.springframework.beans.factory.annotation.Value("${ecoaula.seed.revisor-password:RevisorEcoAula2026!}")
    private String passwordRevisor;

    @org.springframework.beans.factory.annotation.Value("${ecoaula.seed.coordinador-password:CoordinadorEcoAula2026!}")
    private String passwordCoordinador;

    public DataInitializer(AprendizRepository aprendizRepository,
                           CursoRepository cursoRepository,
                           ContenidoRepository contenidoRepository,
                           ContenidoVersionRepository versionRepository,
                           EvaluacionRepository evaluacionRepository,
                           PreguntaRepository preguntaRepository,
                           OpcionPreguntaRepository opcionRepository,
                           PasswordEncoder passwordEncoder,
                           AlmacenamientoContenido almacenamientoContenido) {
        this.aprendizRepository = aprendizRepository;
        this.cursoRepository = cursoRepository;
        this.contenidoRepository = contenidoRepository;
        this.versionRepository = versionRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.preguntaRepository = preguntaRepository;
        this.opcionRepository = opcionRepository;
        this.passwordEncoder = passwordEncoder;
        this.almacenamientoContenido = almacenamientoContenido;
    }

    @Override
    @Transactional
    public void run(String... args) {
        crearCoordinadorSiNoExiste();

        if (cursoRepository.count() > 0) {
            actualizarContrasenasDemo();
            log.info("Datos de demostración ya existen. Contraseñas de demo sincronizadas.");
            return;
        }

        log.info("Inicializando datos de demostración para EcoAula...");

        // 1. Crear Aprendiz de Demostración
        Aprendiz demo = new Aprendiz();
        demo.setNombreCompleto("Aprendiz de Demostración");
        demo.setCorreo("aprendiz@ecoaula.local");
        demo.setTelefono("3001234567");
        demo.setPasswordHash(passwordEncoder.encode(passwordAprendiz));
        demo.setRol(Rol.APRENDIZ);
        demo.setCondicionVisual("Baja visión severa con necesidad de alto contraste");
        demo.setConsentimientoDatos(true);
        demo.setFechaConsentimiento(LocalDateTime.now());

        PerfilAccesibilidad perfil = new PerfilAccesibilidad(demo);
        perfil.setVelocidadAudio(1.0);
        perfil.setModoVisual(ModoVisual.ESTANDAR);
        perfil.setTamanoTexto(TamanoTexto.NORMAL);
        perfil.setSoloAudio(false);
        demo.setPerfilAccesibilidad(perfil);

        aprendizRepository.save(demo);
        log.info("Aprendiz de prueba inicializado: {}", demo.getCorreo());

        // 1.1 Crear Gestor de Contenido de Demostración
        Aprendiz gestor = new Aprendiz();
        gestor.setNombreCompleto("Gestor de Contenido Demo");
        gestor.setCorreo("gestor@ecoaula.local");
        gestor.setTelefono("3009876543");
        gestor.setPasswordHash(passwordEncoder.encode(passwordGestor));
        gestor.setRol(Rol.GESTOR_CONTENIDO);
        gestor.setConsentimientoDatos(true);
        gestor.setFechaConsentimiento(LocalDateTime.now());
        gestor = aprendizRepository.save(gestor);
        log.info("Gestor de contenido inicializado: {}", gestor.getCorreo());

        // 1.2 Crear Revisor de Accesibilidad de Demostración
        Aprendiz revisor = new Aprendiz();
        revisor.setNombreCompleto("Revisor de Accesibilidad Demo");
        revisor.setCorreo("revisor@ecoaula.local");
        revisor.setTelefono("3005551234");
        revisor.setPasswordHash(passwordEncoder.encode(passwordRevisor));
        revisor.setRol(Rol.REVISOR_ACCESIBILIDAD);
        revisor.setConsentimientoDatos(true);
        revisor.setFechaConsentimiento(LocalDateTime.now());
        aprendizRepository.save(revisor);
        log.info("Revisor de accesibilidad inicializado: {}", revisor.getCorreo());

        // 2. Generar archivo de audio base para demostraciones
        byte[] audioWav = AudioDemoGenerator.generarTonoSuaveWav(6, 440.0);
        String rutaAudio1 = almacenamientoContenido.guardarAudio("leccion_demo_ofimatica_1.wav", audioWav);
        String rutaAudio2 = almacenamientoContenido.guardarAudio("leccion_demo_ofimatica_2.wav", audioWav);
        String rutaAudio3 = almacenamientoContenido.guardarAudio("leccion_demo_ofimatica_3.wav", audioWav);

        // 3. Curso 1: Fundamentos de Ofimática Accesible
        Curso c1 = new Curso(
                "Demostración: Fundamentos de Ofimática Accesible",
                "Curso demostrativo enfocado en técnicas de creación de documentos accesibles mediante teclado, uso de encabezados semánticos y lectores de pantalla.",
                10,
                "Tecnología y Productividad",
                "Virtual accesible"
        );
        c1 = cursoRepository.save(c1);

        crearLeccion(c1, 1, "Navegación por teclado en procesadores de texto", rutaAudio1,
                "Transcripción de la Lección 1:\n\n" +
                "Bienvenidos a la primera lección sobre fundamentos de ofimática accesible.\n" +
                "En esta sesión aprenderemos a operar procesadores de texto utilizando exclusivamente el teclado.\n" +
                "La tecla Tabulador nos permite desplazarnos entre opciones interactivas, mientras que Shift + Tab retrocede.\n" +
                "Para navegar por párrafos, utilizamos las flechas direccionales combinadas con la tecla Control.\n" +
                "Recuerde que no se requiere el uso de ratón para dar formato accesible a un documento.", gestor);

        crearLeccion(c1, 2, "Estructura jerárquica con encabezados semánticos", rutaAudio2,
                "Transcripción de la Lección 2:\n\n" +
                "En esta lección abordamos la importancia de los encabezados semánticos de nivel H1, H2 y H3.\n" +
                "Un lector de pantalla utiliza los encabezados como mapa de orientación rápido para saltar de sección en sección.\n" +
                "Nunca utilice simplemente texto en negrita o tamaño grande para simular un título; siempre asigne el estilo de encabezado correspondiente.", gestor);

        crearLeccion(c1, 3, "Texto alternativo en imágenes y exportación a PDF", rutaAudio3,
                "Transcripción de la Lección 3:\n\n" +
                "Para finalizar el curso, analizamos el texto alternativo en imágenes.\n" +
                "Toda imagen con valor informativo debe contar con una descripción concisa que transmita su mensaje a un usuario ciego.\n" +
                "Al exportar a PDF, asegúrese de marcar la opción 'Documento etiquetado para accesibilidad'.", gestor);

        crearEvaluacionOfimatica(c1);

        // 4. Curso 2: Atención al Cliente Inclusiva
        Curso c2 = new Curso(
                "Demostración: Atención al Cliente Inclusiva y Accesible",
                "Curso demostrativo con pautas de comunicación efectiva, trato respetuoso y protocolos de atención accesible en canales presenciales y digitales.",
                8,
                "Habilidades Humanas y Servicio",
                "Virtual accesible"
        );
        c2 = cursoRepository.save(c2);
        crearLeccion(c2, 1, "Principios del trato respetuoso y lenguaje inclusivo", rutaAudio1,
                "Transcripción: Pautas fundamentales para la comunicación clara y directa, dirigiéndose siempre a la persona y no a su acompañante.", gestor);
        crearLeccion(c2, 2, "Pautas de orientación y acompañamiento físico", rutaAudio2,
                "Transcripción: Cómo ofrecer asistencia física adecuadamente mediante la técnica de guía vidente, permitiendo que la persona tome el brazo del guía.", gestor);
        crearLeccion(c2, 3, "Servicio y trámites digitales accesibles", rutaAudio3,
                "Transcripción: Garantizar que los formularios y canales virtuales de atención cumplan con la norma técnica colombiana NTC 5854.", gestor);

        // La cola del revisor se llena con DatosPruebaRevisionInitializer, fuera de los cursos publicados:
        // una lección sin versión publicada en un curso del catálogo impediría a los aprendices terminarlo.

        crearEvaluacionServicio(c2);

        // 5. Curso 3: Principios de Finanzas Personales
        Curso c3 = new Curso(
                "Demostración: Principios de Finanzas Personales Básicas",
                "Curso demostrativo sobre elaboración de presupuestos, ahorro consciente y uso seguro de canales bancarios accesibles.",
                12,
                "Gestión y Finanzas",
                "Virtual accesible"
        );
        c3 = cursoRepository.save(c3);
        crearLeccion(c3, 1, "Elaboración de presupuestos mensuales", rutaAudio1,
                "Transcripción: Estructuración de ingresos y egresos fijos mediante tablas accesibles.", gestor);
        crearLeccion(c3, 2, "Metas de ahorro y fondo de emergencias", rutaAudio2,
                "Transcripción: Importancia de apartar un porcentaje mensual antes de gastos variables.", gestor);
        crearLeccion(c3, 3, "Banca digital accesible y seguridad financiera", rutaAudio3,
                "Transcripción: Recomendaciones de seguridad y uso de lectores de pantalla en portales bancarios.", gestor);
        crearEvaluacionFinanzas(c3);

        log.info("Datos de demostración inicializados con éxito.");
    }

    private void crearLeccion(Curso curso, int orden, String titulo, String rutaAudio, String transcripcion, Aprendiz autor) {
        Contenido c = new Contenido(titulo, orden, curso);
        c = contenidoRepository.save(c);

        ContenidoVersion v = new ContenidoVersion(c, 1, EstadoContenido.PUBLICADA, rutaAudio, transcripcion, LocalDateTime.now());
        v.setCreadoPor(autor);
        v.setCreadoEn(LocalDateTime.now().minusDays(1));
        v.setFuenteMaterial("Elaboración propia del equipo EcoAula (contenido de demostración).");
        v.setLicenciaDeclaradaEn(LocalDateTime.now().minusDays(1));
        versionRepository.save(v);
    }

    private void crearCoordinadorSiNoExiste() {
        String correo = "coordinador@ecoaula.local";
        if (aprendizRepository.existsByCorreo(correo)) {
            return;
        }
        Aprendiz coordinador = new Aprendiz();
        coordinador.setNombreCompleto("Coordinación de Matrícula de Demostración");
        coordinador.setCorreo(correo);
        coordinador.setTelefono("3000000000");
        coordinador.setPasswordHash(passwordEncoder.encode(passwordCoordinador));
        coordinador.setRol(Rol.COORDINADOR);
        coordinador.setConsentimientoDatos(true);
        coordinador.setFechaConsentimiento(LocalDateTime.now());
        coordinador.setPerfilAccesibilidad(new PerfilAccesibilidad(coordinador));
        aprendizRepository.save(coordinador);
        log.info("Coordinación de matrícula creada: {} / CoordinadorEcoAula2026!", correo);
    }

    private void crearEvaluacionOfimatica(Curso curso) {
        Evaluacion ev = new Evaluacion(curso);
        ev = evaluacionRepository.save(ev);

        crearPregunta(ev, 1, "¿Cuál es la tecla principal para desplazarse secuencialmente entre elementos interactivos?",
                "Tecla Tabulador (Tab)", true,
                "Tecla Escape (Esc)", false,
                "Tecla Bloq Mayús", false,
                "Tecla Impr Pant", false);

        crearPregunta(ev, 2, "¿Por qué se deben utilizar estilos de encabezado (H1, H2, H3) en documentos accesibles?",
                "Porque permiten a los lectores de pantalla indexar y navegar por secciones", true,
                "Únicamente para que la letra se vea más grande", false,
                "Para cambiar el color del texto a azul", false,
                "No tienen ninguna utilidad práctica", false);

        crearPregunta(ev, 3, "¿Cuál es la función del texto alternativo en una imagen informativa?",
                "Transmitir en texto el significado y contexto de la imagen para usuarios con discapacidad visual", true,
                "Hacer que la imagen ocupe menos espacio en disco", false,
                "Reemplazar el título del documento", false,
                "Generar una animación automática", false);

        crearPregunta(ev, 4, "¿Qué práctica se recomienda al crear tablas de datos accesibles?",
                "Identificar claramente las celdas de encabezado de fila y columna", true,
                "Insertar la tabla como una imagen escaneada", false,
                "Dejar celdas vacías para separar información", false,
                "Omitir el título de la tabla", false);

        crearPregunta(ev, 5, "¿Qué formato de exportación preserva de mejor manera la accesibilidad estructural?",
                "Documento PDF debidamente etiquetado con texto seleccionable", true,
                "Imagen comprimida en formato JPEG", false,
                "Archivo de solo texto sin ningún tipo de estructura", false,
                "Captura de pantalla recortada", false);
    }

    private void crearEvaluacionServicio(Curso curso) {
        Evaluacion ev = new Evaluacion(curso);
        ev = evaluacionRepository.save(ev);

        crearPregunta(ev, 1, "¿Cómo debe dirigirse el servidor a una persona con discapacidad visual acompañada?",
                "Directamente a la persona interesada, nunca a través del acompañante", true,
                "Exclusivamente al acompañante", false,
                "En voz muy alta", false,
                "Por señas", false);

        crearPregunta(ev, 2, "¿Cuál es la técnica adecuada para brindar orientación física como guía vidente?",
                "Ofrecer el codo o brazo para que la persona lo tome con comodidad", true,
                "Empujar a la persona desde la espalda", false,
                "Halar a la persona del brazo de forma repentina", false,
                "Tomar el bastón de orientación de la persona", false);

        crearPregunta(ev, 3, "¿Qué norma técnica colombiana establece los requisitos de accesibilidad web (WCAG 2.1)?",
                "NTC 5854 y Resolución 1519 de 2020 MinTIC", true,
                "Norma ISO 9001", false,
                "Código de Comercio", false,
                "Decreto de Tránsito", false);

        crearPregunta(ev, 4, "¿Qué se debe hacer antes de brindar ayuda física a una persona ciega?",
                "Preguntar amablemente si requiere asistencia y cómo prefiere recibirla", true,
                "Tomarla de inmediato sin mediar palabra", false,
                "Ignorarla para no incomodar", false,
                "Pedir que espere a un policía", false);

        crearPregunta(ev, 5, "¿Cuál es un canal de atención plenamente accesible recomendado por la normativa?",
                "Portal web con textos claros, alto contraste y compatibilidad con lectores de pantalla", true,
                "Folletos impresos exclusivamente en tipografía minúscula", false,
                "Mensajes en imagen sin texto alternativo", false,
                "Videos sin audio ni subtítulos descriptivos", false);
    }

    private void crearEvaluacionFinanzas(Curso curso) {
        Evaluacion ev = new Evaluacion(curso);
        ev = evaluacionRepository.save(ev);

        crearPregunta(ev, 1, "¿Qué es un presupuesto mensual?",
                "Un plan financiero que detalla ingresos proyectados y límites de gastos para un periodo", true,
                "Una lista de deudas vencidas", false,
                "Un recibo de pago de servicios públicos", false,
                "Un extracto bancario del año anterior", false);

        crearPregunta(ev, 2, "¿Cuál es la regla de oro del ahorro periódico?",
                "Ahorrar al inicio del periodo antes de incurrir en gastos discrecionales", true,
                "Ahorrar únicamente lo que sobre al final del mes si queda algo", false,
                "Gastar todo en promociones", false,
                "Nunca tener un fondo de emergencias", false);

        crearPregunta(ev, 3, "¿Qué característica define a un fondo de emergencias?",
                "Recursos de disponibilidad inmediata para cubrir imprevistos sin endeudarse", true,
                "Dinero invertido en activos de alto riesgo y largo plazo", false,
                "Préstamos solicitados a terceros", false,
                "Tarjetas de crédito al límite", false);

        crearPregunta(ev, 4, "¿Cuál es una recomendación clave de seguridad en banca digital accesible?",
                "Nunca compartir contraseñas ni códigos de seguridad por llamadas o mensajes", true,
                "Anotar la clave en el reverso de la tarjeta", false,
                "Usar la misma clave para todas las plataformas", false,
                "Aceptar ayuda de desconocidos en cajeros automáticos", false);

        crearPregunta(ev, 5, "¿Por qué es importante registrar los gastos diarios?",
                "Para identificar fugas de dinero (gastos hormiga) y ajustar hábitos", true,
                "Para acumular papeles innecesarios", false,
                "No tiene ninguna utilidad práctica", false,
                "Para pagar más comisiones bancarias", false);
    }

    private void crearPregunta(Evaluacion evaluacion, int orden, String enunciado,
                               String opcCorrecta, boolean b1,
                               String opc2, boolean b2,
                               String opc3, boolean b3,
                               String opc4, boolean b4) {
        Pregunta p = new Pregunta(evaluacion, enunciado, orden);
        p = preguntaRepository.save(p);

        opcionRepository.save(new OpcionPregunta(p, opcCorrecta, "Opción 1", b1));
        opcionRepository.save(new OpcionPregunta(p, opc2, "Opción 2", b2));
        opcionRepository.save(new OpcionPregunta(p, opc3, "Opción 3", b3));
        opcionRepository.save(new OpcionPregunta(p, opc4, "Opción 4", b4));
    }

    private void actualizarContrasenasDemo() {
        aprendizRepository.findByCorreo("aprendiz@ecoaula.local").ifPresent(a -> {
            a.setPasswordHash(passwordEncoder.encode(passwordAprendiz));
            aprendizRepository.save(a);
        });
        aprendizRepository.findByCorreo("gestor@ecoaula.local").ifPresent(g -> {
            g.setPasswordHash(passwordEncoder.encode(passwordGestor));
            aprendizRepository.save(g);
        });
        aprendizRepository.findByCorreo("revisor@ecoaula.local").ifPresent(r -> {
            r.setPasswordHash(passwordEncoder.encode(passwordRevisor));
            aprendizRepository.save(r);
        });
        aprendizRepository.findByCorreo("coordinador@ecoaula.local").ifPresent(c -> {
            c.setPasswordHash(passwordEncoder.encode(passwordCoordinador));
            aprendizRepository.save(c);
        });
    }
}
