// Subtítulos de mi propia voz con el reconocimiento de voz del navegador (Safari y Chrome).
// Se envían a la otra persona, que los traduce a su idioma. Solo hacen falta si la otra persona usa la web:
// la app Android ya transcribe por su cuenta el audio que recibe.

const RECOGNITION_LANG = { es: 'es-ES', en: 'en-US', zh: 'zh-CN' };
const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;

let recognition = null;
let active = false;

export const captionsSupported = () => Boolean(Recognition);

/** Empieza a escuchar en [lang] y llama a [onCaption]({ text, final }) con cada frase. */
export function startCaptions(lang, onCaption) {
  if (!Recognition || active) return;
  active = true;
  recognition = new Recognition();
  recognition.lang = RECOGNITION_LANG[lang] ?? 'en-US';
  recognition.continuous = true;
  recognition.interimResults = true;
  recognition.onresult = event => {
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const result = event.results[i];
      const text = result[0].transcript.trim();
      if (text) onCaption({ text, final: result.isFinal });
    }
  };
  // El navegador corta el reconocimiento tras un silencio: se reanuda mientras dure la llamada
  recognition.onend = () => { if (active) setTimeout(() => { try { recognition.start(); } catch (e) { /* ya activo */ } }, 300); };
  try { recognition.start(); } catch (e) { active = false; }
}

export function stopCaptions() {
  active = false;
  try { recognition?.stop(); } catch (e) { /* ya detenido */ }
  recognition = null;
}
