// Historial en la página: la lista de llamadas y el detalle con la traducción y la grabación.
const $ = id => document.getElementById(id);

function formatDate(date, lang) {
  return date instanceof Date
    ? date.toLocaleString(lang === 'zh' ? 'zh-CN' : lang, { dateStyle: 'medium', timeStyle: 'short' })
    : '';
}

const formatDuration = seconds => `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, '0')}`;

/** [calls] del historial; [onOpen](llamada) al tocar una. [t] traduce los textos. */
export function renderList(calls, lang, t, onOpen) {
  const list = $('historyList');
  list.innerHTML = '';
  $('historyEmpty').classList.toggle('hidden', calls.length > 0);
  calls.forEach(call => {
    const item = document.createElement('li');
    const arrow = document.createElement('span');
    arrow.className = 'arrow';
    arrow.textContent = call.direction === 'in' ? '↙' : '↗';
    const body = document.createElement('div');
    body.className = 'grow';
    const who = document.createElement('div');
    who.className = 'who';
    who.textContent = call.peerName || call.peerPhone;
    const when = document.createElement('div');
    when.className = 'when';
    when.textContent = `${t(call.direction === 'in' ? 'incomingShort' : 'outgoingShort')} · ${formatDate(call.startedAt, lang)} · ${formatDuration(call.durationSeconds ?? 0)}`;
    body.append(who, when);
    item.append(arrow, body);
    item.onclick = () => onOpen(call);
    list.append(item);
  });
  showList();
}

export function showList() {
  $('historyList').classList.remove('hidden');
  $('historyDetail').classList.add('hidden');
}

/** Detalle: quién, cuándo, la grabación si está en este dispositivo y las frases traducidas. */
export function renderDetail(call, lines, recording, lang, t) {
  $('historyList').classList.add('hidden');
  $('historyEmpty').classList.add('hidden');
  $('historyDetail').classList.remove('hidden');
  $('detailTitle').textContent = call.peerName || call.peerPhone;
  $('detailMeta').textContent = [call.peerPhone !== call.peerName ? call.peerPhone : '', formatDate(call.startedAt, lang),
    formatDuration(call.durationSeconds ?? 0)].filter(Boolean).join(' · ');
  const audio = $('detailAudio');
  if (audio.src) URL.revokeObjectURL(audio.src);
  audio.classList.toggle('hidden', !recording);
  $('detailNoAudio').classList.toggle('hidden', Boolean(recording));
  audio.src = recording ? URL.createObjectURL(recording) : '';
  const list = $('detailLines');
  list.innerHTML = '';
  lines.forEach(line => {
    const item = document.createElement('li');
    const main = document.createElement('div');
    main.className = 'translated';
    main.textContent = line.translated;
    item.append(main);
    if (line.original && line.original !== line.translated) {
      const source = document.createElement('div');
      source.className = 'original';
      source.textContent = line.original;
      item.append(source);
    }
    list.append(item);
  });
}
