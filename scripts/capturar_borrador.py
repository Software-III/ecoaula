from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=r'C:\Program Files\Google\Chrome\Application\chrome.exe', headless=True)
    page = browser.new_page(viewport={'width': 1280, 'height': 820}, ignore_https_errors=True, device_scale_factor=1.25)
    page.goto('https://3-237-185-159.sslip.io/login')
    page.fill('#correo', 'gestor@ecoaula.local')
    page.fill('#password', 'b0a57909b52d0c01G!')
    page.click("button:has-text('Entrar a mi cuenta')")
    page.wait_for_load_state('networkidle')
    page.goto('https://3-237-185-159.sslip.io/gestor/lecciones/1/versiones')
    page.locator("a:has-text('Editar'), a:has-text('Editar borrador')").first.click()
    page.wait_for_load_state('networkidle')
    page.screenshot(path='docs/manual_capturas/10_gestor_editar_borrador.png')
    
    # Scroll down to capture Images and Ley 23 section
    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
    page.wait_for_timeout(500)
    page.screenshot(path='docs/manual_capturas/10b_gestor_licencia_ley23.png')
    browser.close()
print('Draft editor and Ley 23 captured successfully!')
