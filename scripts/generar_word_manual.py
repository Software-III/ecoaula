import os
import docx
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement, parse_xml
from docx.oxml.ns import nsdecls, qn
from PIL import Image

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    tcPr.append(parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>'))

def set_cell_margins(cell, top=100, bottom=100, left=150, right=150):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = OxmlElement('w:tcMar')
    for m, val in [('top', top), ('bottom', bottom), ('left', left), ('right', right)]:
        node = OxmlElement(f'w:{m}')
        node.set(qn('w:w'), str(val))
        node.set(qn('w:type'), 'dxa')
        tcMar.append(node)
    tcPr.append(tcMar)

def create_manual():
    doc = docx.Document()

    # Configurar márgenes normales (1 pulgada = 2.54 cm)
    for section in doc.sections:
        section.top_margin = Inches(1.0)
        section.bottom_margin = Inches(1.0)
        section.left_margin = Inches(1.0)
        section.right_margin = Inches(1.0)

    AZUL_PRIMARIO = RGBColor(0, 39, 82)     # #002752
    AZUL_SECUNDARIO = RGBColor(0, 77, 163)  # #004da3
    GRIS_OSCURO = RGBColor(51, 51, 51)
    VERDE_EXITO = RGBColor(21, 87, 36)

    # ==========================================
    # PORTADA
    # ==========================================
    p_pre = doc.add_paragraph()
    p_pre.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_pre = p_pre.add_run("UNIVERSIDAD DEL QUINDÍO\nFACULTAD DE INGENIERÍA\nPROGRAMA DE INGENIERÍA DE SISTEMAS Y COMPUTACIÓN\nINGENIERÍA DE SOFTWARE III")
    run_pre.font.name = 'Arial'
    run_pre.font.size = Pt(11)
    run_pre.font.bold = True
    run_pre.font.color.rgb = AZUL_PRIMARIO

    p_space = doc.add_paragraph()
    p_space.paragraph_format.space_before = Pt(40)

    p_title = doc.add_paragraph()
    p_title.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_title = p_title.add_run("MANUAL DE USUARIO\nECOAULA")
    run_title.font.name = 'Arial'
    run_title.font.size = Pt(28)
    run_title.font.bold = True
    run_title.font.color.rgb = AZUL_PRIMARIO

    p_sub = doc.add_paragraph()
    p_sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run_sub = p_sub.add_run("Plataforma Accesible de Cursos Complementarios Cortos para Personas con Discapacidad Visual\n\n(Proceso 1: Inscripción y Matrícula · Proceso 2: Gestión de Contenido Accesible)")
    run_sub.font.name = 'Arial'
    run_sub.font.size = Pt(13)
    run_sub.font.italic = True
    run_sub.font.color.rgb = AZUL_SECUNDARIO

    p_space2 = doc.add_paragraph()
    p_space2.paragraph_format.space_before = Pt(60)

    # Tabla de autores y metadatos
    table = doc.add_table(rows=5, cols=2)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False

    datos = [
        ("Proyecto:", "EcoAula — Aprendizaje Accesible"),
        ("Autores:", "Natalia Ramírez Liévano (nramirezl@uqvirtual.edu.co)\nKaren Vanessa Restrepo Morales (karen.restrepom@uqvirtual.edu.co)\nLeonardo Gallego Ríos (lgallegor@uqvirtual.edu.co)"),
        ("Alcance de Entrega:", "Proceso 1: Registro, Perfil y Matrícula\nProceso 2: Gestión Editorial y Revisión WCAG 2.1 AA"),
        ("Despliegue Oficial:", "AWS Lightsail (HTTPS Let's Encrypt)\nURL: https://3-237-185-159.sslip.io"),
        ("Fecha:", "Octubre de 2026 — Armenia, Quindío")
    ]

    for i, (k, v) in enumerate(datos):
        row = table.rows[i]
        c0 = row.cells[0]
        c1 = row.cells[1]
        c0.width = Inches(2.2)
        c1.width = Inches(4.3)
        set_cell_background(c0, "F0F4F8")
        set_cell_background(c1, "FFFFFF")
        set_cell_margins(c0, 60, 60, 100, 100)
        set_cell_margins(c1, 60, 60, 100, 100)
        
        p0 = c0.paragraphs[0]
        r0 = p0.add_run(k)
        r0.font.name = 'Arial'
        r0.font.bold = True
        r0.font.size = Pt(10)
        r0.font.color.rgb = AZUL_PRIMARIO

        p1 = c1.paragraphs[0]
        r1 = p1.add_run(v)
        r1.font.name = 'Arial'
        r1.font.size = Pt(10)
        r1.font.color.rgb = GRIS_OSCURO

    doc.add_page_break()

    # ==========================================
    # BIENVENIDA E INTRODUCCIÓN
    # ==========================================
    h1 = doc.add_heading("Bienvenido a EcoAula", level=1)
    h1.runs[0].font.name = 'Arial'
    h1.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_intro = doc.add_paragraph()
    p_intro.paragraph_format.line_spacing = 1.15
    p_intro.paragraph_format.space_after = Pt(10)
    p_intro.add_run(
        "EcoAula es una solución de software diseñada bajo los principios de diseño universal y accesibilidad web estricta "
        "(WCAG 2.1 nivel AA y NTC 5854), con el propósito de garantizar que personas con discapacidad visual (ciegas o con baja visión) "
        "puedan inscribirse, aprender y culminar cursos complementarios cortos de forma completamente autónoma, sin depender de intermediarios, "
        "llamadas telefónicas o formatos inaccesibles.\n\n"
        "Este manual describe de forma paso a paso y visual el funcionamiento de los dos procesos transaccionales centrales de la plataforma:"
    )

    # Bullet points
    bp1 = doc.add_paragraph(style='List Bullet')
    r_bp1 = bp1.add_run("Proceso 1: Gestión de Inscripción y Matrícula Accesible (Rol Aprendiz). ")
    r_bp1.font.bold = True
    bp1.add_run("Permite la creación de cuentas accesibles, configuración persistente de preferencias de visualización y voz, consulta del catálogo de cursos con control de cupos y matrícula inmediata.")

    bp2 = doc.add_paragraph(style='List Bullet')
    r_bp2 = bp2.add_run("Proceso 2: Gestión y Publicación de Contenido Accesible (Rol Gestor de Contenido y Revisor). ")
    r_bp2.font.bold = True
    bp2.add_run("Garantiza la producción de contenidos educativos bajo un ciclo editorial inmutable (ADR-08), exigencia legal de licencias (Ley 23 de 1982), obligatoriedad de texto alternativo en imágenes (WCAG 1.1.1) y auditoría técnica mediante lista de chequeo.")

    # Callout box
    t_box = doc.add_table(rows=1, cols=1)
    t_box.alignment = WD_TABLE_ALIGNMENT.CENTER
    c_box = t_box.rows[0].cells[0]
    c_box.width = Inches(6.5)
    set_cell_background(c_box, "E8F4FD")
    set_cell_margins(c_box, 100, 100, 150, 150)
    p_box = c_box.paragraphs[0]
    r_box_title = p_box.add_run("Compromiso de Accesibilidad y Tecnologías de Asistencia:\n")
    r_box_title.font.bold = True
    r_box_title.font.color.rgb = AZUL_PRIMARIO
    r_box_desc = p_box.add_run(
        "Toda la interfaz cuenta con navegación completa por teclado (Tab, Enter, Espacio, Flechas), atajos directos "
        "de salto al contenido principal (WCAG 2.4.1), compatibilidad probada con lectores de pantalla (NVDA, JAWS y VoiceOver), "
        "y soporte nativo para el reflow y aumento visual al 200% sin pérdida de información (WCAG 1.4.10)."
    )
    r_box_desc.font.size = Pt(9.5)

    # ------------------------------------------
    # ANTES DE EMPEZAR: ACCESIBILIDAD Y ROLES
    # ------------------------------------------
    h_pre = doc.add_heading("Antes de Empezar: Accesibilidad y Tecnologías Compatibles", level=2)
    h_pre.runs[0].font.name = 'Arial'
    h_pre.runs[0].font.size = Pt(13)
    h_pre.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_pre_desc = doc.add_paragraph(
        "Principio de diseño: No es necesario usar el ratón; todas las funciones de EcoAula operan al 100% con teclado "
        "y tecnologías de asistencia estándar en navegadores web modernos."
    )
    p_pre_desc.paragraph_format.line_spacing = 1.15
    p_pre_desc.paragraph_format.space_after = Pt(6)

    table_tech = doc.add_table(rows=4, cols=2)
    table_tech.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_tech.autofit = False
    techs = [
        ("Categoría", "Tecnologías Soportadas y Recomendadas"),
        ("Lectores de pantalla", "NVDA (Windows), JAWS (Windows), VoiceOver (macOS / iOS), TalkBack (Android)."),
        ("Ampliación y Braille", "Ampliadores nativos de pantalla, ZoomText y terminales de línea braille efímera."),
        ("Navegadores recomendados", "Google Chrome, Microsoft Edge, Mozilla Firefox o Safari (con conexión a internet).")
    ]
    for i, (k, v) in enumerate(techs):
        row = table_tech.rows[i]
        c0, c1 = row.cells[0], row.cells[1]
        c0.width, c1.width = Inches(2.2), Inches(4.3)
        bg = "EBF3EE" if i == 0 else ("F7FAFC" if i % 2 == 1 else "FFFFFF")
        set_cell_background(c0, bg)
        set_cell_background(c1, bg)
        set_cell_margins(c0, 50, 50, 80, 80)
        set_cell_margins(c1, 50, 50, 80, 80)
        p0, p1 = c0.paragraphs[0], c1.paragraphs[0]
        r0, r1 = p0.add_run(k), p1.add_run(v)
        r0.font.name = r1.font.name = 'Arial'
        r0.font.bold = (i == 0 or True)
        r0.font.size = r1.font.size = Pt(9)
        if i == 0:
            r0.font.color.rgb = r1.font.color.rgb = VERDE_EXITO
            r1.font.bold = True
        else:
            r0.font.color.rgb = AZUL_PRIMARIO
            r1.font.color.rgb = GRIS_OSCURO

    h_roles = doc.add_heading("Matriz de Actores: ¿Quién usa EcoAula?", level=2)
    h_roles.runs[0].font.name = 'Arial'
    h_roles.runs[0].font.size = Pt(13)
    h_roles.runs[0].font.color.rgb = AZUL_PRIMARIO

    table_actores = doc.add_table(rows=7, cols=3)
    table_actores.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_actores.autofit = False
    actores = [
        ("Rol / Actor", "Qué hace en la plataforma", "Vista inicial al acceder"),
        ("Visitante (sin cuenta)", "Consulta información pública, se registra, inicia sesión o reporta barreras.", "/login o /reportar-barrera"),
        ("Aprendiz", "Configura su perfil sensorial, se matricula en cursos, estudia con audio/transcripción, presenta evaluaciones y obtiene certificados.", "/cursos (Catálogo)"),
        ("Gestor de contenido", "Diseña cursos, administra lecciones, carga audio nativo e imágenes con alt, declara derechos de autor y envía borradores a revisión.", "/gestor/cursos (Panel)"),
        ("Revisor de accesibilidad", "Audita técnicamente versiones con checklist WCAG 2.1 AA / NTC 5854, aprueba para publicación o emite rechazos con subsanación.", "/revisor/cola (Bandeja)"),
        ("Coordinación de matrícula", "Resuelve y responde reportes de barreras de accesibilidad y hace seguimiento a los indicadores de calidad y riesgo de abandono.", "/reportes-barrera (Incidencias)"),
        ("Entidad verificadora", "Comprueba la autenticidad e integridad de certificados estudiantiles mediante código alfanumérico sin necesidad de tener cuenta.", "/verificar")
    ]
    for i, (col0, col1, col2) in enumerate(actores):
        row = table_actores.rows[i]
        c0, c1, c2 = row.cells[0], row.cells[1], row.cells[2]
        c0.width, c1.width, c2.width = Inches(1.8), Inches(3.2), Inches(1.5)
        bg = "EBF3EE" if i == 0 else ("F7FAFC" if i % 2 == 1 else "FFFFFF")
        for cell in (c0, c1, c2):
            set_cell_background(cell, bg)
            set_cell_margins(cell, 50, 50, 80, 80)
        for idx, (cell, val) in enumerate([(c0, col0), (c1, col1), (c2, col2)]):
            p = cell.paragraphs[0]
            r = p.add_run(val)
            r.font.name = 'Arial'
            r.font.size = Pt(8.5 if i > 0 else 9.5)
            r.font.bold = (i == 0 or idx == 0)
            if i == 0:
                r.font.color.rgb = VERDE_EXITO
            else:
                r.font.color.rgb = AZUL_PRIMARIO if idx == 0 else GRIS_OSCURO

    doc.add_page_break()

    # ==========================================
    # ÍNDICE DE CONTENIDO
    # ==========================================
    h_idx = doc.add_heading("Índice de Contenido", level=1)
    h_idx.runs[0].font.name = 'Arial'
    h_idx.runs[0].font.color.rgb = AZUL_PRIMARIO

    indice_items = [
        ("Parte I: Portal del Aprendiz (Proceso 1 — Inscripción y Matrícula)", [
            "1). Acceso al Portal y Página de Inicio de Sesión",
            "2). Registro de Nueva Cuenta y Consentimiento de Datos Sensibles (Ley 1581)",
            "3). Configuración y Prueba del Perfil de Accesibilidad",
            "4). Exploración del Catálogo de Cursos y Verificación de Cupos",
            "5). Aula Virtual y Reproducción Accesible de Lecciones (Audio y Voz en Alta)"
        ]),
        ("Parte II: Portal del Gestor de Contenidos (Proceso 2 — Producción Editorial)", [
            "1). Acceso y Panel Principal de Cursos Registrados",
            "2). Creación de Cursos y Asignación de Cupos Máximos",
            "3). Estructura y Administración de Lecciones",
            "4). Ciclo Editorial Inmutable: Historial de Versiones (ADR-08)",
            "5). Edición de Borradores: Transcripción, Audio e Imágenes con Texto Alternativo",
            "6). Declaración de Derechos de Autor (Ley 23 de 1982) y Envío a Revisión"
        ]),
        ("Parte III: Portal del Revisor de Accesibilidad (Proceso 2 — Auditoría Técnica)", [
            "1). Bandeja de Cola de Revisión Técnica",
            "2). Auditoría con Checklist Oficial WCAG 2.1 AA y NTC 5854",
            "3). Emisión y Consulta de Informes de Revisión Técnica Imprimibles"
        ]),
        ("Parte IV: Soporte, Reporte de Barreras y Glosario Técnico (Calidad)", [
            "1). Canal Público de Reporte de Barreras de Accesibilidad",
            "2). Glosario Técnico y de Accesibilidad Web"
        ])
    ]

    for seccion, pasos in indice_items:
        p_sec = doc.add_paragraph()
        p_sec.paragraph_format.space_before = Pt(8)
        p_sec.paragraph_format.space_after = Pt(2)
        r_sec = p_sec.add_run(seccion)
        r_sec.font.bold = True
        r_sec.font.size = Pt(11)
        r_sec.font.color.rgb = AZUL_PRIMARIO

        for paso in pasos:
            p_paso = doc.add_paragraph(style='List Bullet')
            p_paso.paragraph_format.space_after = Pt(2)
            r_paso = p_paso.add_run(paso)
            r_paso.font.size = Pt(10)

    doc.add_page_break()

    # Función auxiliar para agregar pasos con imagen
    def agregar_paso(parte_titulo, numero_paso, titulo_paso, descripcion_usuario, descripcion_sistema, imagen_nombre, nota_accesibilidad=None, salto_pagina=True):
        if salto_pagina:
            doc.add_page_break()

        h_paso = doc.add_heading(f"{parte_titulo} — {numero_paso}. {titulo_paso}", level=2)
        h_paso.runs[0].font.name = 'Arial'
        h_paso.runs[0].font.size = Pt(13)
        h_paso.runs[0].font.color.rgb = AZUL_PRIMARIO

        p_desc = doc.add_paragraph()
        p_desc.paragraph_format.line_spacing = 1.15
        p_desc.paragraph_format.space_after = Pt(4)
        r_u_title = p_desc.add_run("Acción del Usuario: ")
        r_u_title.font.bold = True
        p_desc.add_run(descripcion_usuario + "\n")

        r_s_title = p_desc.add_run("Respuesta del Sistema: ")
        r_s_title.font.bold = True
        p_desc.add_run(descripcion_sistema)

        if nota_accesibilidad:
            p_nota = doc.add_paragraph()
            p_nota.paragraph_format.space_after = Pt(4)
            r_n_title = p_nota.add_run("♿ Criterio de Accesibilidad / Norma: ")
            r_n_title.font.bold = True
            r_n_title.font.color.rgb = VERDE_EXITO
            r_n_desc = p_nota.add_run(nota_accesibilidad)
            r_n_desc.font.size = Pt(9.5)

        img_path = os.path.join("docs", "manual_capturas", imagen_nombre)
        if os.path.exists(img_path):
            p_img = doc.add_paragraph()
            p_img.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p_img.paragraph_format.space_before = Pt(4)
            p_img.paragraph_format.space_after = Pt(8)
            run_img = p_img.add_run()

            # Medición inteligente de dimensiones para evitar recortes en Word
            try:
                with Image.open(img_path) as im:
                    w_px, h_px = im.size
                    aspect = h_px / w_px
            except Exception:
                aspect = 1.0

            if aspect > 1.6:
                run_img.add_picture(img_path, height=Inches(5.4))
            elif aspect > 1.1:
                run_img.add_picture(img_path, height=Inches(5.0))
            else:
                run_img.add_picture(img_path, width=Inches(5.8))
        else:
            p_err = doc.add_paragraph(f"[Captura {imagen_nombre} pendiente]")
            p_err.alignment = WD_ALIGN_PARAGRAPH.CENTER

    # ==========================================
    # PARTE I: APRENDIZ (PROCESO 1)
    # ==========================================
    h_p1 = doc.add_heading("Parte I / Portal del Aprendiz: Registro, Accesibilidad y Matrícula", level=1)
    h_p1.runs[0].font.name = 'Arial'
    h_p1.runs[0].font.color.rgb = AZUL_PRIMARIO

    agregar_paso(
        "Parte I", "1", "Acceso al Portal e Inicio de Sesión",
        "El aprendiz ingresa a la URL de la plataforma (https://3-237-185-159.sslip.io/login). Si ya posee cuenta, digita su correo y contraseña y pulsa 'Entrar a mi cuenta'. Si requiere registrarse por primera vez, pulsa el botón 'Crear una cuenta nueva'. En caso de experimentar barreras de acceso, dispone de un enlace público directo para reportarlas sin necesidad de autenticarse.",
        "El sistema valida las credenciales contra la base de datos MySQL (utilizando BCrypt con factor de costo 10). Al autenticar, recupera el perfil de accesibilidad persistente y redirige automáticamente al Catálogo de Cursos.",
        "01_login_portal.png",
        "WCAG 2.4.1 (Salto al contenido principal mediante Tab), etiquetas <label> explícitas en cada campo y región role='alert' para anunciar errores de acceso.",
        salto_pagina=False
    )

    agregar_paso(
        "Parte I", "2", "Registro de Cuenta y Consentimiento de Datos Sensibles",
        "El aspirante completa sus datos personales (nombre, correo institucional, teléfono, contraseña segura) y declara de manera opcional su condición visual (ceguera total o baja visión). Debe marcar obligatoriamente la casilla de consentimiento expreso para el tratamiento de sus datos sensibles antes de enviar el formulario.",
        "El backend cifra el dato sensible de la condición visual en reposo mediante AES-256 en modo GCM con vector de inicialización único (ADR-09), garantizando estricta conformidad con la Ley Estatutaria 1581 de 2012 y el Decreto 1377 de 2013.",
        "02_registro_formulario.png",
        "Ley 1581 de 2012 (Protección de datos sensibles). Validación en tiempo real sin recarga disruptiva y foco visible permanente (#002752)."
    )

    agregar_paso(
        "Parte I", "3", "Configuración y Prueba del Perfil de Accesibilidad",
        "Desde la barra superior, el usuario accede a 'Mi Perfil' (/perfil). Aquí puede seleccionar su modo visual preferido (Normal o Alto Contraste para baja visión), tamaño de texto (Estándar o Grande), velocidad de reproducción (1.0x a 2.0x) y seleccionar una voz en español del sintetizador. Al pulsar 'Probar voz y velocidad', el sistema emite una frase de prueba en los altavoces.",
        "Las preferencias se persisten en base de datos asociadas a la cuenta del usuario (ADR-07) y se almacenan en la sesión HTTP para aplicarse de forma inmediata en todas las pantallas sin reconfiguración manual.",
        "03_perfil_accesibilidad.png",
        "ADR-07 (Perfil de accesibilidad persistente) y Web Speech API con síntesis de voz en el cliente respetando atajos del sistema operativo."
    )

    agregar_paso(
        "Parte I", "4", "Exploración del Catálogo de Cursos y Verificación de Cupos",
        "El aprendiz navega el catálogo (/cursos). Cada tarjeta de curso presenta el título, descripción, intensidad horaria (8 a 40 horas), estado de evaluación y el número de cupos disponibles en tiempo real (ej. 'Cupos disponibles: 15 de 20'). Si el aprendiz no está matriculado, pulsa el botón 'Matricularme'.",
        "El servicio de matrícula aplica bloqueo pesimista en base de datos (PESSIMISTIC_WRITE) para evitar sobrecupos concurrentes en el momento de la confirmación. Si el curso llega a 0 cupos, el botón se inhabilita con un aviso comprensible.",
        "04_catalogo_cursos.png",
        "WCAG 1.3.1 (Información y relaciones con tarjetas semánticas) y control de concurrencia pesimista en base de datos."
    )

    agregar_paso(
        "Parte I", "5", "Aula Virtual y Reproducción Accesible de Lecciones",
        "Al ingresar a una lección (/cursos/{id}/lecciones/{orden}), el aprendiz encuentra el reproductor de audio nativo con controles estándar, la transcripción completa del contenido estructurada por párrafos, imágenes con su respectivo texto alternativo y el botón 'Leer en voz alta la transcripción' que lee el contenido a la velocidad elegida.",
        "El servidor entrega los recursos de audio e imagen mediante cabeceras HTTP Range (código 206 Partial Content), permitiendo adelantar, retroceder y pausar el contenido multimedia fluidamente. Al pulsar 'Completar y continuar', se registra el avance del 100% de la lección.",
        "05_aula_leccion.png",
        "ADR-05 (Reproductor <audio> nativo accesible por teclado con Espacio y Flechas) y WCAG 1.2.1 / 1.2.2 (Transcripción completa de audio)."
    )

    doc.add_page_break()

    # ==========================================
    # PARTE II: GESTOR DE CONTENIDO (PROCESO 2)
    # ==========================================
    h_p2 = doc.add_heading("Parte II / Portal del Gestor: Gestión Editorial y Publicación de Contenido", level=1)
    h_p2.runs[0].font.name = 'Arial'
    h_p2.runs[0].font.color.rgb = AZUL_PRIMARIO

    t_box_p2 = doc.add_table(rows=1, cols=1)
    t_box_p2.alignment = WD_TABLE_ALIGNMENT.CENTER
    c_box_p2 = t_box_p2.rows[0].cells[0]
    c_box_p2.width = Inches(6.5)
    set_cell_background(c_box_p2, "E8F4FD")
    set_cell_margins(c_box_p2, 80, 80, 120, 120)
    p_box_p2 = c_box_p2.paragraphs[0]
    r_bp2_title = p_box_p2.add_run("Principio del Ciclo de Vida Editorial Inmutable (ADR-08):\n")
    r_bp2_title.font.bold = True
    r_bp2_title.font.size = Pt(9.5)
    r_bp2_title.font.color.rgb = AZUL_PRIMARIO
    r_bp2_desc = p_box_p2.add_run(
        "Cada lección atraviesa un ciclo riguroso de estados: Borrador (producción de material) → En Revisión "
        "(auditoría técnica) → Publicada (versión vigente con 100% de criterios WCAG cumplidos). Si es Rechazada, se documentan "
        "las no conformidades en un informe formal y el gestor ejecuta la Subsanación creando un nuevo borrador. "
        "Regla de negocio: Cada lección tiene una sola versión vigente y una sola en trámite a la vez. Lo publicado nunca se sobrescribe."
    )
    r_bp2_desc.font.size = Pt(9.0)

    p_sp = doc.add_paragraph()
    p_sp.paragraph_format.space_before = Pt(4)
    p_sp.paragraph_format.space_after = Pt(4)

    agregar_paso(
        "Parte II", "1", "Acceso al Portal y Panel de Cursos Registrados",
        "El gestor inicia sesión con sus credenciales institucionales. El sistema lo enruta directamente a /gestor/cursos. En esta vista observa la tabla de todos los cursos registrados, indicando si se encuentran 'EN EL CATÁLOGO' o 'FUERA DEL CATÁLOGO', número de horas, cupo asignado, aprendices matriculados y acciones disponibles.",
        "El sistema restringe las modificaciones estructurales (renombrar o eliminar lecciones) si el curso ya cuenta con estudiantes matriculados o si está publicado en el catálogo, protegiendo la ruta académica de los aprendices.",
        "06_gestor_cursos.png",
        "Control de acceso basado en roles (Spring Security hasRole('GESTOR_CONTENIDO')) y trazabilidad de cursos.",
        salto_pagina=False
    )

    agregar_paso(
        "Parte II", "2", "Creación de Cursos y Asignación de Cupos",
        "Al pulsar el botón '+ Crear Nuevo Curso', el gestor completa el formulario con: Título del curso, Descripción detallada, Duración estimada (validada entre 8 y 40 horas según el caso de estudio institucional), Área de conocimiento y Cupo máximo de aprendices (opcional).",
        "El curso se crea inicialmente en estado 'FUERA DEL CATÁLOGO'. No se hará visible a los aprendices hasta que todas sus lecciones cuenten con versión publicada por el revisor técnico y tenga su evaluación final configurada.",
        "07_gestor_crear_curso.png",
        "Validación estricta de negocio: duración mínima de 8 horas y máxima de 40 horas (requisito del caso de estudio)."
    )

    agregar_paso(
        "Parte II", "3", "Estructura y Gestión de Lecciones",
        "En la fila del curso, el gestor selecciona 'Gestionar lecciones'. Puede registrar nuevas lecciones asignando título descriptivo. La plataforma asigna automáticamente el número de orden secuencial (1, 2, 3...) sin huecos ni duplicados para garantizar la navegación continua del estudiante.",
        "Cada lección creada nace automáticamente con su Versión 1 en estado BORRADOR, habilitando de inmediato la carga de materiales accesibles.",
        "08_gestor_lecciones.png",
        "Garantía de navegación continua: ordenamiento numérico estricto para evitar enlaces rotos o lecciones omitidas."
    )

    agregar_paso(
        "Parte II", "4", "Ciclo Editorial Inmutable: Historial de Versiones",
        "Al pulsar 'Ver versiones' en cualquier lección, el gestor visualiza el historial inmutable (ADR-08). Las versiones PUBLICADAS y RETIRADAS son de solo lectura y nunca se sobreescriben. Para realizar correcciones o actualizaciones, el gestor pulsa '+ Crear Nueva Versión en Borrador'.",
        "El sistema genera la versión v2 copiando automáticamente la transcripción, la ruta de audio, las imágenes y la fuente de la versión vigente, evitando recapturar la información desde cero.",
        "09_gestor_versiones.png",
        "ADR-08 (Inmutabilidad editorial): trazabilidad histórica sin sentencias UPDATE en registros de contenido aprobado."
    )

    agregar_paso(
        "Parte II", "5", "Edición de Borrador: Transcripción, Audio e Imágenes con Alt",
        "En el editor del borrador (/gestor/versiones/{id}/editar), el gestor redacta o ajusta la transcripción textual completa, adjunta el archivo de audio nativo (formatos WAV, MP3 u OGG hasta 15 MB validados por el servidor) y carga imágenes de apoyo con su respectivo 'Texto alternativo (alt)' obligatorio.",
        "El backend rechaza la subida de cualquier imagen que no incluya texto alternativo, cumpliendo de forma determinística con la pauta WCAG 1.1.1 en la capa de persistencia.",
        "10_gestor_editar_borrador.png",
        "WCAG 1.1.1 (Contenido no textual): texto alternativo obligatorio validado en el servidor; límite de audio de 15 MB con saneamiento UUID."
    )

    agregar_paso(
        "Parte II", "6", "Declaración de Derechos de Autor (Ley 23 de 1982) y Envío a Revisión",
        "En la parte inferior del borrador, el gestor debe ingresar la Fuente del Material y marcar la casilla de declaración legal: 'Declaro que cuento con la debida autorización de uso...'. Tras completar los requisitos, presiona 'Guardar y enviar esta versión a Revisión Técnica'.",
        "La versión cambia automáticamente de estado BORRADOR a EN_REVISION, registrando la fecha exacta y quedando encolada en la bandeja del Revisor de Accesibilidad.",
        "10b_gestor_licencia_ley23.png",
        "Ley 23 de 1982 sobre Derechos de Autor: trazabilidad legal y control de licenciamiento antes de la publicación técnica."
    )

    doc.add_page_break()

    # ==========================================
    # PARTE III: REVISOR DE ACCESIBILIDAD (PROCESO 2)
    # ==========================================
    h_p3 = doc.add_heading("Parte III / Portal del Revisor: Auditoría Técnica y Cierre Editorial", level=1)
    h_p3.runs[0].font.name = 'Arial'
    h_p3.runs[0].font.color.rgb = AZUL_PRIMARIO

    agregar_paso(
        "Parte III", "1", "Bandeja de Cola de Revisión Técnica",
        "El Revisor de Accesibilidad ingresa con su rol institucional a /revisor/cola. En esta pantalla se presentan todas las versiones enviadas por los gestores que esperan dictamen técnico. Si la versión proviene de una corrección previa, se indica claramente que es una 'Subsanación de versión anterior'.",
        "Se aplica el principio de separación de funciones: un gestor de contenido no puede auditar ni aprobar sus propios contenidos.",
        "11_revisor_cola.png",
        "Separación de responsabilidades y cola de trabajo ordenada por fecha de solicitud.",
        salto_pagina=False
    )

    agregar_paso(
        "Parte III", "2", "Auditoría Técnica con Checklist WCAG 2.1 AA y NTC 5854",
        "Al presionar 'Auditar y Calificar', el revisor evalúa el contenido reproduciendo el audio, leyendo la transcripción y verificando imágenes y licencias. En la sección de evaluación marca los 6 criterios del checklist técnico (audio inteligible, transcripción completa, estructura semántica, sin referencias solo visuales, idioma español y enlaces descriptivos).",
        "Para 'Aprobar y Publicar' se exige el 100% de cumplimiento de los criterios. Al aprobar, la versión pasa a PUBLICADA y la versión previa a RETIRADA. Si no cumple, el revisor redacta el motivo de las no conformidades y pulsa 'Rechazar Versión con este Motivo'.",
        "12_revisor_auditoria.png",
        "NTC 5854 y WCAG 2.1 AA: umbral del 100% en criterios de accesibilidad para autorizar la publicación oficial."
    )

    agregar_paso(
        "Parte III", "3", "Emisión y Consulta de Informes de Revisión Imprimibles",
        "Desde el menú 'Informes de revisión' (/informes-revision), tanto el gestor como el revisor pueden consultar el historial de auditorías técnicas emitidas, con el desglose de criterios cumplidos, fecha y dictamen final.",
        "Al abrir el detalle del informe, la vista cuenta con hoja de estilo CSS adaptada para impresión formal (Ctrl + P), sirviendo como soporte auditable y evidencia física para el Plan de Gestión de Calidad.",
        "13_revisor_informes.png",
        "Evidencia formal de calidad: informes de auditoría inmutables e imprimibles bajo estándar institucional."
    )

    doc.add_page_break()

    # ==========================================
    # PARTE IV: SOPORTE, REPORTE DE BARRERAS Y GLOSARIO
    # ==========================================
    h_p4 = doc.add_heading("Parte IV / Soporte al Usuario, Reporte de Barreras y Glosario Técnico", level=1)
    h_p4.runs[0].font.name = 'Arial'
    h_p4.runs[0].font.color.rgb = AZUL_PRIMARIO

    h_soporte = doc.add_heading("1. Canal Público de Reporte de Barreras de Accesibilidad", level=2)
    h_soporte.runs[0].font.name = 'Arial'
    h_soporte.runs[0].font.size = Pt(13)
    h_soporte.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_sop = doc.add_paragraph()
    p_sop.paragraph_format.line_spacing = 1.15
    p_sop.paragraph_format.space_after = Pt(8)
    p_sop.add_run(
        "En cumplimiento de la Ley Estatutaria 1618 de 2013 y la Resolución 1519 de 2020 de MinTIC, EcoAula dispone de un "
        "canal público y permanente para que cualquier aprendiz, tutor o ciudadano reporte barreras de accesibilidad sin necesidad "
        "de iniciar sesión previa. Si el usuario experimenta incompatibilidad con su lector de pantalla (NVDA, JAWS, VoiceOver), "
        "problemas de contraste, fallas en la navegación por teclado o ausencia de transcripciones, puede acceder a "
    )
    r_url_sop = p_sop.add_run("https://3-237-185-159.sslip.io/reportar-barrera")
    r_url_sop.font.bold = True
    p_sop.add_run(" para registrar la incidencia y permitir su subsanación inmediata por parte del equipo de calidad.")

    h_glo = doc.add_heading("2. Glosario de Accesibilidad Web y Arquitectura", level=2)
    h_glo.runs[0].font.name = 'Arial'
    h_glo.runs[0].font.size = Pt(13)
    h_glo.runs[0].font.color.rgb = AZUL_PRIMARIO

    table_glo = doc.add_table(rows=7, cols=2)
    table_glo.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_glo.autofit = False

    terminos = [
        ("WCAG 2.1 (Nivel AA)", "Pautas de Accesibilidad para el Contenido Web del W3C. Establecen los criterios normativos de perceptibilidad, operabilidad, comprensibilidad y robustez de obligatorio cumplimiento en Colombia."),
        ("NTC 5854", "Norma Técnica Colombiana de accesibilidad a páginas web expedida por el ICONTEC y exigida por el Ministerio de las TIC."),
        ("Texto Alternativo (Alt)", "Descripción textual sintética (máximo 250 caracteres) obligatoria en cada imagen para transmitir su función o significado a personas ciegas (WCAG 1.1.1)."),
        ("Bloqueo Pesimista (ADR-06)", "Mecanismo de concurrencia en base de datos (SELECT ... FOR UPDATE) implementado en el servicio de matrícula para garantizar cero sobrecupos ante solicitudes concurrentes."),
        ("Inmutabilidad Editorial (ADR-08)", "Principio de diseño donde una versión de contenido publicada jamás se sobrescribe mediante sentencias UPDATE; cualquier cambio genera una nueva versión en borrador conservando la trazabilidad histórica."),
        ("Cifrado en Reposo (ADR-09)", "Protección criptográfica AES-256 en modo GCM con vector de inicialización único para la condición visual del estudiante, garantizando conformidad con la Ley 1581 de 2012."),
        ("Lector de Pantalla", "Tecnología de asistencia (NVDA, JAWS, VoiceOver, TalkBack) que verbaliza la estructura semántica y el contenido de la plataforma para permitir la autonomía de usuarios con discapacidad visual.")
    ]

    for i, (term, defn) in enumerate(terminos):
        row = table_glo.rows[i]
        c0, c1 = row.cells[0], row.cells[1]
        c0.width = Inches(2.2)
        c1.width = Inches(4.3)
        set_cell_background(c0, "F0F4F8")
        set_cell_background(c1, "FFFFFF")
        set_cell_margins(c0, 60, 60, 100, 100)
        set_cell_margins(c1, 60, 60, 100, 100)
        
        p0 = c0.paragraphs[0]
        r0 = p0.add_run(term)
        r0.font.name = 'Arial'
        r0.font.bold = True
        r0.font.size = Pt(9.5)
        r0.font.color.rgb = AZUL_PRIMARIO
        
        p1 = c1.paragraphs[0]
        r1 = p1.add_run(defn)
        r1.font.name = 'Arial'
        r1.font.size = Pt(9.5)
        r1.font.color.rgb = GRIS_OSCURO

    h_verif = doc.add_heading("3. Verificación Pública de Autenticidad de Certificados", level=2)
    h_verif.runs[0].font.name = 'Arial'
    h_verif.runs[0].font.size = Pt(13)
    h_verif.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_ver = doc.add_paragraph()
    p_ver.paragraph_format.line_spacing = 1.15
    p_ver.paragraph_format.space_after = Pt(8)
    p_ver.add_run(
        "Cualquier empresa, entidad verificadora o ciudadano puede corroborar la validez de un certificado emitido "
        "por EcoAula sin necesidad de contar con una cuenta en la plataforma:\n"
        "• En la interfaz web: Ingresar a https://3-237-185-159.sslip.io/verificar, digitar el código alfanumérico "
        "(formato EA-XXXX-XXXX-XXXX-XXXX) y pulsar 'Validar autenticidad'.\n"
        "• Desde sistemas externos (API REST): GET /api/v1/certificados/{codigo}/verificacion retorna {'valido': true} o {'valido': false}.\n"
        "• Criterio de Privacidad: La consulta es pública y está protegida contra abusos mediante limitación de tasa (Rate Limiting). "
        "En cumplimiento de la Ley 1581 de 2012, el servicio confirma la validez sin exponer datos personales del aprendiz."
    )

    # ==========================================
    # PARTE V: PROBLEMAS FRECUENTES
    # ==========================================
    doc.add_page_break()
    h_p5 = doc.add_heading("Parte V / Solución de Problemas Frecuentes (Troubleshooting)", level=1)
    h_p5.runs[0].font.name = 'Arial'
    h_p5.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_p5 = doc.add_paragraph("A continuación se resumen las dudas y situaciones operativas más comunes observadas durante el uso de la plataforma y su resolución inmediata:")
    p_p5.paragraph_format.line_spacing = 1.15
    p_p5.paragraph_format.space_after = Pt(8)

    table_faq = doc.add_table(rows=7, cols=3)
    table_faq.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_faq.autofit = False

    faqs = [
        ("Situación Observada", "Causa Probable", "Solución / Qué Hacer"),
        ("No aparece el botón 'Matricularme' en un curso", "El curso agotó sus cupos disponibles o el aprendiz ya se encuentra matriculado en él.", "Si ya está inscrito, pulsar 'Continuar curso'. Si no hay cupos (0 de N disponibles), aguardar ampliación de cupos por coordinación."),
        ("No aparece o no funciona 'Leer en voz alta'", "El navegador no cuenta con paquetes de voces en español o no soporta Web Speech API.", "Utilizar Google Chrome o Microsoft Edge e instalar voces en español en la configuración de accesibilidad del sistema operativo."),
        ("No se visualiza el avance del curso tras cerrar sesión", "El estudiante no pulsó el botón de finalización de la lección antes de salir.", "El progreso se consolida al pulsar 'Completar y continuar'. La sección 'Mi curso' en el catálogo muestra siempre la última lección completada."),
        ("(Gestor) El sistema no permite 'Guardar y enviar a revisión'", "Falta adjuntar el archivo de audio, escribir la transcripción, ingresar la fuente o marcar la casilla legal.", "Verificar que los 4 requisitos obligatorios del borrador estén debidamente diligenciados antes de enviar."),
        ("(Gestor) No aparece el botón 'Crear Nueva Versión en Borrador'", "La lección ya cuenta con una versión en estado BORRADOR o EN_REVISION en trámite.", "Por regla de integridad editorial, debe esperarse el dictamen del revisor o editar el borrador existente."),
        ("(Revisor) El sistema no permite 'Aprobar y Publicar' una versión", "No se han marcado la totalidad de los 6 criterios del checklist de accesibilidad.", "Marcar el 100% de los criterios normativos si la versión cumple, o registrar el motivo de no conformidad y presionar 'Rechazar Versión con este Motivo'.")
    ]

    for i, (col0, col1, col2) in enumerate(faqs):
        row = table_faq.rows[i]
        c0, c1, c2 = row.cells[0], row.cells[1], row.cells[2]
        c0.width, c1.width, c2.width = Inches(1.8), Inches(2.2), Inches(2.5)
        bg = "EBF3EE" if i == 0 else ("F7FAFC" if i % 2 == 1 else "FFFFFF")
        for cell in (c0, c1, c2):
            set_cell_background(cell, bg)
            set_cell_margins(cell, 50, 50, 80, 80)
        for idx, (cell, val) in enumerate([(c0, col0), (c1, col1), (c2, col2)]):
            p = cell.paragraphs[0]
            r = p.add_run(val)
            r.font.name = 'Arial'
            r.font.size = Pt(8.5 if i > 0 else 9.5)
            r.font.bold = (i == 0 or idx == 0)
            if i == 0:
                r.font.color.rgb = VERDE_EXITO
            else:
                r.font.color.rgb = AZUL_PRIMARIO if idx == 0 else GRIS_OSCURO

    # ==========================================
    # ANEXOS
    # ==========================================
    doc.add_page_break()
    h_anx = doc.add_heading("Anexos Técnicos y Operativos", level=1)
    h_anx.runs[0].font.name = 'Arial'
    h_anx.runs[0].font.color.rgb = AZUL_PRIMARIO

    # Anexo 1: Navegación accesible por teclado
    h_anx1 = doc.add_heading("Anexo 1: Protocolo de Navegación Accesible por Teclado", level=2)
    h_anx1.runs[0].font.name = 'Arial'
    h_anx1.runs[0].font.size = Pt(13)
    h_anx1.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_anx1 = doc.add_paragraph("Para verificar la plataforma con tecnologías de asistencia o sin depender del ratón, se dispone de los siguientes atajos estándar:")
    p_anx1.paragraph_format.line_spacing = 1.15
    p_anx1.paragraph_format.space_after = Pt(6)

    table_key = doc.add_table(rows=8, cols=3)
    table_key.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_key.autofit = False

    teclas = [
        ("Tecla / Atajo", "Función en la Plataforma EcoAula", "Criterio WCAG"),
        ("Tab", "Desplaza el foco al siguiente control interactivo (enlace, botón, campo).", "2.1.1 (Teclado)"),
        ("Shift + Tab", "Retrocede el foco al control interactivo anterior.", "2.1.1 (Teclado)"),
        ("Enter", "Activa enlaces, botones y envía formularios validados.", "2.1.1 (Teclado)"),
        ("Espacio", "Alterna casillas de verificación (checkboxes) y pausa/reanuda el audio nativo.", "ADR-05"),
        ("Flechas (↑ ↓ ← →)", "Selecciona opciones de botones de radio y adelanta/retrasa segundos en el reproductor.", "ADR-05"),
        ("H (NVDA / JAWS)", "Salta secuencialmente entre encabezados (h1, h2, h3) para navegación rápida.", "2.4.6 (Encabezados)"),
        ("Tab al cargar página", "Revela el enlace visible de acceso rápido: 'Saltar al contenido principal'.", "2.4.1 (Evitar bloques)")
    ]

    for i, (col0, col1, col2) in enumerate(teclas):
        row = table_key.rows[i]
        c0, c1, c2 = row.cells[0], row.cells[1], row.cells[2]
        c0.width = Inches(1.8)
        c1.width = Inches(3.2)
        c2.width = Inches(1.5)
        bg = "EBF3EE" if i == 0 else ("F7FAFC" if i % 2 == 1 else "FFFFFF")
        for cell in (c0, c1, c2):
            set_cell_background(cell, bg)
            set_cell_margins(cell, 60, 60, 100, 100)
        
        for idx, (cell, val) in enumerate([(c0, col0), (c1, col1), (c2, col2)]):
            p = cell.paragraphs[0]
            r = p.add_run(val)
            r.font.name = 'Arial'
            r.font.size = Pt(9 if i > 0 else 9.5)
            r.font.bold = (i == 0 or idx == 0)
            if i == 0:
                r.font.color.rgb = VERDE_EXITO
            else:
                r.font.color.rgb = AZUL_PRIMARIO if idx == 0 else GRIS_OSCURO

    # Anexo 2: Cuentas del ambiente de demostración
    h_anx2 = doc.add_heading("Anexo 2: Cuentas del Ambiente de Demostración", level=2)
    h_anx2.runs[0].font.name = 'Arial'
    h_anx2.runs[0].font.size = Pt(13)
    h_anx2.runs[0].font.color.rgb = AZUL_PRIMARIO

    p_anx2 = doc.add_paragraph("A continuación se relacionan las cuentas demo preconfiguradas para pruebas y auditoría funcional de los roles del sistema:")
    p_anx2.paragraph_format.line_spacing = 1.15
    p_anx2.paragraph_format.space_after = Pt(6)

    table_users = doc.add_table(rows=5, cols=4)
    table_users.alignment = WD_TABLE_ALIGNMENT.CENTER
    table_users.autofit = False

    cuentas = [
        ("Rol", "Correo Institucional", "Contraseña (Dev / Local)", "Al iniciar sesión llega a"),
        ("Aprendiz", "aprendiz@ecoaula.local", "EcoAula2026!", "/cursos (Catálogo)"),
        ("Gestor de contenido", "gestor@ecoaula.local", "GestorEcoAula2026!", "/gestor/cursos (Panel)"),
        ("Revisor de accesibilidad", "revisor@ecoaula.local", "RevisorEcoAula2026!", "/revisor/cola (Bandeja)"),
        ("Coordinación de matrícula", "coordinador@ecoaula.local", "CoordinadorEcoAula2026!", "/reportes-barrera (Incidencias)")
    ]

    for i, (col0, col1, col2, col3) in enumerate(cuentas):
        row = table_users.rows[i]
        c0, c1, c2, c3 = row.cells[0], row.cells[1], row.cells[2], row.cells[3]
        c0.width = Inches(1.6)
        c1.width = Inches(1.8)
        c2.width = Inches(1.6)
        c3.width = Inches(1.5)
        bg = "EBF3EE" if i == 0 else ("F7FAFC" if i % 2 == 1 else "FFFFFF")
        for cell in (c0, c1, c2, c3):
            set_cell_background(cell, bg)
            set_cell_margins(cell, 60, 60, 80, 80)
        
        for idx, (cell, val) in enumerate([(c0, col0), (c1, col1), (c2, col2), (c3, col3)]):
            p = cell.paragraphs[0]
            r = p.add_run(val)
            r.font.name = 'Arial'
            r.font.size = Pt(8.5 if i > 0 else 9.5)
            r.font.bold = (i == 0 or idx == 0)
            if i == 0:
                r.font.color.rgb = VERDE_EXITO
            else:
                r.font.color.rgb = AZUL_PRIMARIO if idx == 0 else GRIS_OSCURO

    p_nota = doc.add_paragraph()
    p_nota.paragraph_format.line_spacing = 1.15
    p_nota.paragraph_format.space_before = Pt(6)
    p_nota.paragraph_format.space_after = Pt(12)
    r_nota_bold = p_nota.add_run("Nota de Seguridad: ")
    r_nota_bold.font.name = 'Arial'
    r_nota_bold.font.bold = True
    r_nota_bold.font.size = Pt(8.5)
    r_nota_bold.font.color.rgb = GRIS_OSCURO
    r_nota_txt = p_nota.add_run(
        "Las contraseñas de la tabla corresponden exclusivamente a los perfiles de desarrollo y pruebas (dev y dev-h2). "
        "En el entorno de producción cloud (AWS Lightsail), por directiva de hardening de seguridad (DataInitializer.java), "
        "las credenciales se administran de manera aislada mediante variables de entorno seguras en el servidor."
    )
    r_nota_txt.font.name = 'Arial'
    r_nota_txt.font.italic = True
    r_nota_txt.font.size = Pt(8.5)
    r_nota_txt.font.color.rgb = GRIS_OSCURO

    # ==========================================
    # GUARDAR DOCUMENTO
    # ==========================================
    output_docx = os.path.join("docs", "Manual_de_Usuario_EcoAula.docx")
    try:
        doc.save(output_docx)
        print(f"Documento guardado exitosamente en: {output_docx}")
    except PermissionError:
        output_fallback = os.path.join("docs", "Manual_de_Usuario_EcoAula_v2.docx")
        doc.save(output_fallback)
        print(f"AVISO: 'Manual_de_Usuario_EcoAula.docx' está abierto en Word. Se guardó como: {output_fallback}")

if __name__ == "__main__":
    create_manual()
