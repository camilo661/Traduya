// El "teléfono" de la página: recibe llamadas en el número propio, llama a otros números,
// y traduce en vivo lo que dice la otra persona (sus subtítulos) al idioma de la página.
import { identifyCaller } from './verify.js';
import { goOffline, goOnline } from './presence.js';
import { startPeer, stopPeer } from './peer.js';
import { acceptCall, attachIncomingData, hangUp as endCall, placeCall, sendCaption, setMuted } from './call.js';
import { CallError, resolvePeerId } from './directory.js';
import { startCaptions, stopCaptions } from './captions.js';
import { translate } from './translate.js';
import { addLine, finishCallRecord, startCallRecord } from './history.js';
import { saveRecording } from './recordings.js';

const CONNECTION_ERRORS = ['network', 'server-error', 'socket-error', 'socket-closed', 'unavailable-id'];

let config = null;   // { uid, profile, getToken, lang(), ui }
let incoming = null; // { media, caller }
let active = null;   // llamada en curso

/**
 * Empieza a recibir llamadas en el teléfono de [profile].
 * [ui] recibe: onPresence('online' | 'offline' | 'taken'), onIncoming(quien), onIncomingCancelled(),
 * onCallStarted(quien), onStatus(clave), onConnected(), onLanguages(de, a), onLive(texto), onLine(frase),
 * onEnded(clave), onHistoryChanged().
 */
export async function startPhone(options) {
  stopPhone();
  config = options;
  const peerId = await goOnline(config.uid, config.profile.phone, config.getToken);
  if (!peerId) config.ui.onPresence('taken');
  startPeer(peerId, {
    onReady: () => config?.ui.onPresence(peerId ? 'online' : 'taken'),
    onOffline: () => config?.ui.onPresence('offline'),
    onIncoming,
    onData: attachIncomingData,
    onError: type => { if (CONNECTION_ERRORS.includes(type)) config?.ui.onPresence('offline'); },
  });
}

export function stopPhone() {
  if (active) endCall();
  goOffline();
  stopPeer();
  config = null;
  incoming = null;
}

// region Llamadas entrantes

async function onIncoming(media) {
  if (!config || active || incoming) { media.close(); return; }
  // Solo suena si quien llama tiene una sesión válida y está registrado (web) o tiene la app
  const caller = await identifyCaller(media.metadata?.idToken, await config.getToken());
  if (!caller || !config) { media.close(); return; }
  incoming = { media, caller };
  media.on('close', () => {
    if (incoming?.media !== media) return;
    incoming = null;
    config?.ui.onIncomingCancelled();
  });
  config.ui.onIncoming(caller);
}

export function answer() {
  if (!incoming) return;
  const { media, caller } = incoming;
  incoming = null;
  begin(caller, 'in');
  acceptCall(media, config.lang(), callEvents());
}

export function decline() {
  if (!incoming) return;
  const { media } = incoming;
  incoming = null;
  media.close();
}

// endregion

// region Llamadas salientes

/** Llama al número marcado. Lanza CallError si no está en Traduya o no hay conexión. */
export async function call(target) {
  if (!config || active) return;
  const token = await config.getToken();
  const peerId = await resolvePeerId(target, token);
  begin({ name: target, phone: target, kind: null }, 'out');
  placeCall(peerId, token, config.lang(), callEvents());
}

// endregion

// region Llamada en curso

function begin(peer, direction) {
  active = {
    peer, direction, peerLang: null, remoteKind: peer.kind, startedAt: null, historyId: null,
    historyReady: null, liveTurn: 0,
  };
  config.ui.onCallStarted(peer, direction);
}

function callEvents() {
  const current = active;
  return {
    onStatus: key => config?.ui.onStatus(key),
    onConnected: () => {
      current.startedAt = Date.now();
      current.historyReady = config.getToken()
        .then(token => startCallRecord(config.uid, token, {
          direction: current.direction, peerName: current.peer.name, peerPhone: current.peer.phone,
          myLanguage: config.lang(),
        }))
        .then(id => { current.historyId = id; return id; })
        .catch(() => null);
      config.ui.onConnected();
      // Si ya sabemos que la otra persona usa la web, le enviamos los subtítulos de nuestra voz
      if (current.remoteKind === 'web') startOwnCaptions();
    },
    onHello: ({ kind, lang }) => {
      current.remoteKind = kind;
      current.peerLang = lang;
      config?.ui.onLanguages(lang, config.lang());
      if (kind === 'web') startOwnCaptions();
    },
    onCaption: caption => showCaption(current, caption),
    onRecording: blob => current.historyReady?.then(id => { if (id) saveRecording(id, blob); }),
    onEnded: async key => {
      stopCaptions();
      if (active === current) active = null;
      config?.ui.onEnded(key);
      const id = await current.historyReady;
      if (id && config) {
        const durationSeconds = Math.round((Date.now() - current.startedAt) / 1000);
        await finishCallRecord(config.uid, await config.getToken(), id, { durationSeconds, peerLanguage: current.peerLang })
          .catch(() => {});
        config?.ui.onHistoryChanged();
      }
    },
  };
}

function startOwnCaptions() {
  startCaptions(config.lang(), caption => sendCaption(caption));
}

/** Traduce lo que dijo la otra persona al idioma de la página y lo guarda en el historial. */
async function showCaption(current, { text, final, lang }) {
  if (!config) return;
  const to = config.lang();
  const from = lang || current.peerLang || to;
  if (!current.peerLang) {
    current.peerLang = from;
    config.ui.onLanguages(from, to);
  }
  // La frase en curso se traduce con cada cambio; si llega una más nueva, la anterior se descarta
  const turn = ++current.liveTurn;
  const translated = await translate(text, from, to);
  if (!config || active !== current) return;
  if (!final) {
    if (turn === current.liveTurn) config.ui.onLive(translated);
    return;
  }
  config.ui.onLine({ translated, original: from !== to ? text : '' });
  const id = await current.historyReady;
  if (id) addLine(config.uid, await config.getToken(), id, { original: text, translated, from }).catch(() => {});
}

export function hangUp() {
  endCall();
}

export { setMuted };

export { CallError };

// endregion
