/*
 * EcoAula: preferencias del perfil de accesibilidad aplicadas en el navegador (ADR-07).
 *
 * Mejora progresiva: el reproductor <audio> nativo (ADR-05) y los formularios funcionan sin este
 * script. Aquí solo se aplica la velocidad preferida, se ofrece la lista de voces del navegador
 * y se habilita la lectura en voz alta con la voz y la velocidad elegidas.
 */
(function () {
    'use strict';

    var sintesis = window.speechSynthesis;

    function velocidadValida(valor) {
        var v = parseFloat(valor);
        return isFinite(v) && v >= 0.5 && v <= 2 ? v : 1;
    }

    function vozPorNombre(nombre) {
        if (!sintesis || !nombre) {
            return null;
        }
        return sintesis.getVoices().filter(function (v) { return v.name === nombre; })[0] || null;
    }

    /**
     * Los botones de voz solo se muestran si el navegador tiene voces instaladas: algunos
     * (por ejemplo Firefox en Linux) exponen la API sin voces y el botón no haría nada.
     * Las voces pueden cargarse de forma asíncrona, por eso se reevalúa con "voiceschanged".
     */
    function mostrarSiHayVoces(boton) {
        var actualizar = function () {
            boton.hidden = sintesis.getVoices().length === 0;
        };
        actualizar();
        sintesis.addEventListener('voiceschanged', actualizar);
    }

    function hablar(texto, nombreVoz, velocidad, alTerminar) {
        sintesis.cancel();
        var frase = new SpeechSynthesisUtterance(texto);
        frase.lang = 'es';
        frase.rate = velocidad;
        var voz = vozPorNombre(nombreVoz);
        if (voz) {
            frase.voice = voz;
            frase.lang = voz.lang;
        }
        frase.onend = alTerminar;
        frase.onerror = alTerminar;
        sintesis.speak(frase);
    }

    // 1. Velocidad de reproducción del audio de las lecciones
    document.querySelectorAll('audio[data-velocidad]').forEach(function (audio) {
        var velocidad = velocidadValida(audio.getAttribute('data-velocidad'));
        var aplicar = function () {
            audio.defaultPlaybackRate = velocidad;
            audio.playbackRate = velocidad;
        };
        aplicar();
        audio.addEventListener('loadedmetadata', aplicar);
    });

    // 1b. Aviso de inicio de reproducción (indicador "Tiempo de acceso al contenido"): una vez por página
    document.querySelectorAll('audio[data-evento-url]').forEach(function (audio) {
        audio.addEventListener('play', function () {
            var token = document.querySelector('input[name="_csrf"]');
            var datos = new URLSearchParams();
            datos.append('curso', audio.getAttribute('data-curso'));
            datos.append('leccion', audio.getAttribute('data-leccion'));
            if (token) {
                datos.append('_csrf', token.value);
            }
            fetch(audio.getAttribute('data-evento-url'), { method: 'POST', body: datos, credentials: 'same-origin' })
                .catch(function () { /* la analítica nunca interrumpe la reproducción */ });
        }, { once: true });
    });

    // 2. Selector de voz (registro y perfil): voces en español disponibles en el navegador
    document.querySelectorAll('select[data-selector-voz]').forEach(function (select) {
        var actual = select.getAttribute('data-voz-actual') || '';
        var predeterminada = select.options[0];

        function poblar() {
            var voces = sintesis ? sintesis.getVoices().filter(function (v) {
                return v.lang && v.lang.toLowerCase().indexOf('es') === 0;
            }) : [];
            if (voces.length === 0) {
                return;
            }
            select.innerHTML = '';
            select.appendChild(predeterminada);
            voces.forEach(function (v) {
                var opcion = document.createElement('option');
                opcion.value = v.name;
                opcion.textContent = v.name + ' (' + v.lang + ')';
                opcion.selected = v.name === actual;
                select.appendChild(opcion);
            });
            // Una voz guardada desde otro navegador se conserva para no perderla al guardar el perfil aquí
            if (actual && !voces.some(function (v) { return v.name === actual; })) {
                var guardada = document.createElement('option');
                guardada.value = actual;
                guardada.textContent = actual + ' (no disponible en este navegador)';
                guardada.selected = true;
                select.appendChild(guardada);
            }
        }

        poblar();
        if (sintesis) {
            sintesis.addEventListener('voiceschanged', poblar);
        }
    });

    // 3. Probar la voz y la velocidad elegidas en el formulario
    document.querySelectorAll('[data-probar-voz]').forEach(function (boton) {
        if (!sintesis) {
            return;
        }
        mostrarSiHayVoces(boton);
        boton.addEventListener('click', function () {
            var voz = document.getElementById(boton.getAttribute('data-probar-voz'));
            var velocidad = document.getElementById(boton.getAttribute('data-velocidad-desde'));
            hablar('Hola. Así sonará la lectura en voz alta en EcoAula.',
                voz ? voz.value : '', velocidadValida(velocidad ? velocidad.value : 1), function () {});
        });
    });

    // 4. Leer en voz alta un texto de la página (por ejemplo, la transcripción de la lección)
    document.querySelectorAll('[data-leer-en-voz]').forEach(function (boton) {
        if (!sintesis) {
            return;
        }
        var objetivo = document.getElementById(boton.getAttribute('data-leer-en-voz'));
        var estado = document.getElementById(boton.getAttribute('data-estado'));
        var textoInicial = boton.textContent;
        mostrarSiHayVoces(boton);

        function terminar() {
            boton.textContent = textoInicial;
            boton.removeAttribute('data-leyendo');
            if (estado) {
                estado.textContent = 'Lectura en voz alta detenida.';
            }
        }

        boton.addEventListener('click', function () {
            if (boton.hasAttribute('data-leyendo')) {
                sintesis.cancel();
                terminar();
                return;
            }
            // El nombre del botón indica la acción disponible; el estado se anuncia en la región aria-live
            boton.textContent = 'Detener lectura en voz alta';
            boton.setAttribute('data-leyendo', '');
            if (estado) {
                estado.textContent = 'Leyendo en voz alta.';
            }
            hablar(objetivo ? objetivo.textContent : '', boton.getAttribute('data-voz'),
                velocidadValida(boton.getAttribute('data-velocidad')), terminar);
        });
    });
})();
