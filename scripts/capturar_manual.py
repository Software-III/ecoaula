import os
from playwright.sync_api import sync_playwright

BASE_URL = "https://3-237-185-159.sslip.io"
OUTPUT_DIR = "docs/manual_capturas"
os.makedirs(OUTPUT_DIR, exist_ok=True)

PASS_APRENDIZ = "3d215e81dc8f616eA!"
PASS_GESTOR = "b0a57909b52d0c01G!"
PASS_REVISOR = "fd96edd756d85b31R!"

def login(page, correo, password):
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.fill("#correo", correo)
    page.fill("#password", password)
    page.click("button[type='submit']")
    page.wait_for_load_state("networkidle")

with sync_playwright() as p:
    browser = p.chromium.launch(
        executable_path=r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        headless=True
    )

    # ==========================================
    # APRENDIZ (PROCESO 1)
    # ==========================================
    ctx_aprendiz = browser.new_context(viewport={"width": 1280, "height": 900}, ignore_https_errors=True)
    page = ctx_aprendiz.new_page()

    print("[1/13] 01_login_portal.png...")
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "01_login_portal.png"), full_page=True)

    print("[2/13] 02_registro_formulario.png...")
    page.goto(f"{BASE_URL}/registro")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "02_registro_formulario.png"), full_page=True)

    print("[3/13] 03_perfil_accesibilidad.png...")
    login(page, "aprendiz@ecoaula.local", PASS_APRENDIZ)
    page.goto(f"{BASE_URL}/perfil")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "03_perfil_accesibilidad.png"), full_page=True)

    print("[4/13] 04_catalogo_cursos.png...")
    page.goto(f"{BASE_URL}/cursos")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "04_catalogo_cursos.png"), full_page=True)

    print("[5/13] 05_aula_leccion.png...")
    page.goto(f"{BASE_URL}/cursos/1/lecciones/1")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "05_aula_leccion.png"), full_page=True)

    ctx_aprendiz.close()

    # ==========================================
    # GESTOR (PROCESO 2)
    # ==========================================
    ctx_gestor = browser.new_context(viewport={"width": 1280, "height": 900}, ignore_https_errors=True)
    page = ctx_gestor.new_page()
    login(page, "gestor@ecoaula.local", PASS_GESTOR)

    print("[6/13] 06_gestor_cursos.png...")
    page.goto(f"{BASE_URL}/gestor/cursos")
    page.wait_for_load_state("networkidle")
    curso_1 = page.locator("li.tarjeta-curso").first
    box = curso_1.bounding_box()
    if box:
        h_target = int(box["y"] + box["height"] + 50)
        page.set_viewport_size({"width": 1280, "height": h_target})
        page.screenshot(path=os.path.join(OUTPUT_DIR, "06_gestor_cursos.png"))
    else:
        page.screenshot(path=os.path.join(OUTPUT_DIR, "06_gestor_cursos.png"), full_page=True)

    print("[7/13] 07_gestor_crear_curso.png...")
    page.set_viewport_size({"width": 1280, "height": 900})
    page.goto(f"{BASE_URL}/gestor/cursos/nuevo")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "07_gestor_crear_curso.png"), full_page=True)

    print("[8/13] 08_gestor_lecciones.png...")
    page.goto(f"{BASE_URL}/gestor/cursos/1/lecciones")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "08_gestor_lecciones.png"), full_page=True)

    print("[9/13] 09_gestor_versiones.png...")
    page.goto(f"{BASE_URL}/gestor/lecciones/1/versiones")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "09_gestor_versiones.png"), full_page=True)

    print("[10/13] 10_gestor_editar_borrador.png...")
    btn_editar = page.locator("a:has-text('Editar'), a:has-text('Editar borrador')").first
    if btn_editar.count() > 0:
        btn_editar.click()
    else:
        page.goto(f"{BASE_URL}/gestor/versiones/1/editar")
    page.wait_for_load_state("networkidle")

    # Medir la sección de transcripción y botones intermedios
    btn_guardar_borrador = page.locator("button:has-text('Guardar Cambios del Borrador')").first
    box_guardar = btn_guardar_borrador.bounding_box()
    if box_guardar:
        h_editor_top = int(box_guardar["y"] + box_guardar["height"] + 60)
        page.set_viewport_size({"width": 1280, "height": h_editor_top})
        page.screenshot(path=os.path.join(OUTPUT_DIR, "10_gestor_editar_borrador.png"))
    else:
        page.set_viewport_size({"width": 1280, "height": 1350})
        page.screenshot(path=os.path.join(OUTPUT_DIR, "10_gestor_editar_borrador.png"))

    print("[10b/13] 10b_gestor_licencia_ley23.png...")
    # Para 10b, queremos capturar desde "Material fuente y derechos de autor" hasta "Guardar y enviar a Revisión Técnica"
    sec_ley = page.locator("fieldset:has-text('Material fuente'), legend:has-text('Material fuente')").first
    btn_enviar = page.locator("button:has-text('Guardar y enviar'), button:has-text('Revisión Técnica')").first
    box_ley = sec_ley.bounding_box()
    box_enviar = btn_enviar.bounding_box()

    if box_ley and box_enviar:
        y_start = int(box_ley["y"] - 30)
        y_end = int(box_enviar["y"] + box_enviar["height"] + 50)
        h_needed = y_end - y_start
        page.set_viewport_size({"width": 1280, "height": h_needed})
        page.evaluate(f"window.scrollTo(0, {y_start})")
        page.wait_for_timeout(300)
        page.screenshot(path=os.path.join(OUTPUT_DIR, "10b_gestor_licencia_ley23.png"))
    else:
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
        page.wait_for_timeout(300)
        page.screenshot(path=os.path.join(OUTPUT_DIR, "10b_gestor_licencia_ley23.png"))

    ctx_gestor.close()

    # ==========================================
    # REVISOR (PROCESO 2)
    # ==========================================
    ctx_revisor = browser.new_context(viewport={"width": 1280, "height": 900}, ignore_https_errors=True)
    page = ctx_revisor.new_page()
    login(page, "revisor@ecoaula.local", PASS_REVISOR)

    print("[11/13] 11_revisor_cola.png...")
    page.goto(f"{BASE_URL}/revisor/cola")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "11_revisor_cola.png"), full_page=True)

    print("[12/13] 12_revisor_auditoria.png...")
    btn_auditar = page.locator("a:has-text('Auditar y Calificar')").first
    if btn_auditar.count() > 0:
        btn_auditar.click()
        page.wait_for_load_state("networkidle")
        
        # Queremos capturar el checklist completo con todos los 6 criterios,
        # el cuadro de motivo de rechazo Y los botones finales de decisión
        sec_chk = page.locator("h2:has-text('5. Lista de Chequeo'), h3:has-text('5. Lista de Chequeo')").first
        btn_aprobar = page.locator("button:has-text('Aprobar y Publicar')").first
        box_chk = sec_chk.bounding_box()
        box_aprobar = btn_aprobar.bounding_box()

        if box_chk and box_aprobar:
            y_start = int(box_chk["y"] - 30)
            y_end = int(box_aprobar["y"] + box_aprobar["height"] + 60)
            h_needed = y_end - y_start
            page.set_viewport_size({"width": 1280, "height": h_needed})
            page.evaluate(f"window.scrollTo(0, {y_start})")
            page.wait_for_timeout(300)
            page.screenshot(path=os.path.join(OUTPUT_DIR, "12_revisor_auditoria.png"))
        else:
            page.screenshot(path=os.path.join(OUTPUT_DIR, "12_revisor_auditoria.png"), full_page=True)

    print("[13/13] 13_revisor_informes.png...")
    page.set_viewport_size({"width": 1280, "height": 900})
    page.goto(f"{BASE_URL}/informes-revision")
    page.wait_for_load_state("networkidle")
    page.screenshot(path=os.path.join(OUTPUT_DIR, "13_revisor_informes.png"), full_page=True)

    ctx_revisor.close()
    browser.close()

print("¡Todas las capturas se generaron e integraron exitosamente sin cortes!")
