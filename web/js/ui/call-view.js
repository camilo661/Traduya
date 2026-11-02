// Pantalla de llamada en curso: quién es, el tiempo y la traducción en vivo de lo que dice la otra persona.
const $ = id => document.getElementById(id);
let timer = null;

const initial = name => (name || '?').trim().charAt(0).toUpperCase();

export function showPeer({ name, phone }) {
  $('callAvatar').textContent = initial(name);
  $('callName').textContent = name || phone;
  $('callMeta').textContent = name && name !== phone ? phone : '';
  $('callLines').innerHTML = '';
  $('liveText').textContent = '';
}

export function setHint(text) {
  $('callHint').textContent = text;
}

export function setLanguages(text) {
  $('langPill').textContent = text;
  $('langPill').classList.toggle('hidden', !text);
}

/** Frase en curso (se reemplaza mientras la otra persona habla). */
export function setLive(text) {
  $('liveText').textContent = text;
  scrollToEnd();
}

/** Frase terminada: la traducción grande y el original pequeño debajo. */
export function addLine({ translated, original }) {
  const item = document.createElement('li');
  const main = document.createElement('div');
  main.className = 'translated';
  main.textContent = translated;
  item.append(main);
  if (original && original !== translated) {
    const source = document.createElement('div');
    source.className = 'original';
    source.textContent = original;
    item.append(source);
  }
  $('callLines').append(item);
  $('liveText').textContent = '';
  $('callHint').textContent = '';
  scrollToEnd();
}

export function startTimer(phone) {
  const start = Date.now();
  clearInterval(timer);
  timer = setInterval(() => {
    const seconds = Math.floor((Date.now() - start) / 1000);
    const clock = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
    $('callMeta').textContent = phone && phone !== $('callName').textContent ? `${phone} · ${clock}` : clock;
  }, 500);
  return start;
}

export function stopTimer() {
  clearInterval(timer);
}

function scrollToEnd() {
  const area = $('liveText').parentElement;
  area.scrollTop = area.scrollHeight;
}
