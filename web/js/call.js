// Una llamada (saliente o entrante): micrófono, audio de la otra persona, subtítulos y grabación.
// Los subtítulos viajan por un canal de datos aparte: cada lado envía el texto de su propia voz.
import { readyPeer } from './peer.js';

/** Si nadie contesta en este tiempo, la llamada se termina. */
const RING_TIMEOUT_MS = 45 * 1000;

let state = null;

function newState(events, lang) {
  return {
    events, lang, media: null, data: null, localStream: null, remoteAudio: null,
    recorder: null, recordedChunks: [], audioContext: null, connected: false, ended: false,
  };
}

async function microphone() {
  return navigator.mediaDevices.getUserMedia({
    audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
  });
}

/**
 * Llama a [peerId]. [idToken] viaja con la llamada para que el otro lado compruebe quién llama.
 * [events]: onStatus(clave), onConnected(), onEnded(clave), onHello({ kind, lang }), onCaption({ text, final, lang }),
 * onRecording(archivo).
 */
export async function placeCall(peerId, idToken, lang, events) {
  state = newState(events, lang);
  const current = state;
  // El audio remoto se prepara dentro del toque del usuario para que iOS permita reproducirlo
  prepareRemoteAudio(current);
  try {
    current.localStream = await microphone();
  } catch (e) {
    end(current, 'micBlocked');
    return;
  }
  let peer;
  try {
    peer = await readyPeer();
  } catch (e) {
    end(current, 'connectionError');
    return;
  }
  const onPeerError = type => { if (!current.connected) end(current, type === 'peer-unavailable' ? 'unavailable' : 'connectionError'); };
  peer.on('error', onPeerError);
  current.detach = () => peer.off('error', onPeerError);

  events.onStatus('ringing');
  current.media = peer.call(peerId, current.localStream, { metadata: { idToken, kind: 'web', lang } });
  setTimeout(() => { if (!current.connected) end(current, 'noAnswer'); }, RING_TIMEOUT_MS);
  watchMedia(current, () => attachData(peer.connect(peerId, { reliable: true }), current));
}

/** Contesta una llamada entrante ([mediaCall] de PeerJS). */
export async function acceptCall(mediaCall, lang, events) {
  state = newState(events, lang);
  const current = state;
  prepareRemoteAudio(current);
  try {
    current.localStream = await microphone();
  } catch (e) {
    mediaCall.close();
    end(current, 'micBlocked');
    return;
  }
  current.media = mediaCall;
  mediaCall.answer(current.localStream);
  watchMedia(current, () => {});
}

/** Canal de datos que abre quien llama: se une a la llamada en curso. */
export function attachIncomingData(connection) {
  if (state && !state.ended) attachData(connection, state);
  else connection.close();
}

export function sendCaption(caption) {
  if (state?.data?.open) state.data.send({ type: 'caption', ...caption, lang: state.lang });
}

/** El usuario cambió de idioma durante la llamada: se avisa al otro lado para que traduzca desde el nuevo. */
export function setCallLanguage(lang) {
  if (!state || state.lang === lang) return;
  state.lang = lang;
  if (state.data?.open) state.data.send({ type: 'hello', kind: 'web', lang });
}

export function hangUp() {
  if (state) end(state, 'ended');
}

export function setMuted(muted) {
  state?.localStream?.getAudioTracks().forEach(track => { track.enabled = !muted; });
}

function prepareRemoteAudio(current) {
  current.remoteAudio = new Audio();
  current.remoteAudio.playsInline = true;
  current.remoteAudio.autoplay = true;
}

function watchMedia(current, onStream) {
  const media = current.media;
  let streamed = false;
  media.on('stream', stream => {
    if (streamed) return; // 'stream' puede llegar dos veces
    streamed = true;
    current.remoteAudio.srcObject = stream;
    current.remoteAudio.play().catch(() => {});
    current.connected = true;
    startRecording(current, stream);
    onStream();
    current.events.onConnected();
  });
  media.on('close', () => end(current, 'ended'));
  media.on('error', () => end(current, 'ended'));
  const connection = media.peerConnection;
  if (connection) {
    connection.addEventListener('connectionstatechange', () => {
      if (connection.connectionState === 'failed') end(current, 'lost');
    });
  }
}

function attachData(connection, current) {
  current.data = connection;
  connection.on('open', () => connection.send({ type: 'hello', kind: 'web', lang: current.lang }));
  connection.on('data', message => {
    if (current.ended || !message || typeof message !== 'object') return;
    if (message.type === 'hello') current.events.onHello?.({ kind: message.kind, lang: message.lang });
    if (message.type === 'caption' && message.text) current.events.onCaption?.(message);
  });
  // Si el otro lado cuelga, su canal se cierra: también se termina la llamada aquí
  connection.on('close', () => { if (current.connected) end(current, 'ended'); });
}

/** Graba ambas voces mezcladas en este dispositivo. */
function startRecording(current, remoteStream) {
  if (!window.MediaRecorder) return;
  try {
    const context = new AudioContext();
    const mix = context.createMediaStreamDestination();
    context.createMediaStreamSource(remoteStream).connect(mix);
    context.createMediaStreamSource(current.localStream).connect(mix);
    const type = ['audio/webm;codecs=opus', 'audio/mp4', 'audio/webm'].find(t => MediaRecorder.isTypeSupported(t));
    current.recorder = new MediaRecorder(mix.stream, type ? { mimeType: type } : undefined);
    current.recorder.ondataavailable = event => { if (event.data.size) current.recordedChunks.push(event.data); };
    current.recorder.onstop = () => {
      context.close();
      if (current.recordedChunks.length) {
        current.events.onRecording?.(new Blob(current.recordedChunks, { type: current.recorder.mimeType }));
      }
    };
    current.recorder.start(1000);
    current.audioContext = context;
  } catch (e) { /* sin grabación en este navegador */ }
}

/** Cada llamada termina una sola vez, con el primer motivo (el cierre posterior no lo tapa). */
function end(current, messageKey) {
  if (current.ended) return;
  current.ended = true;
  current.detach?.();
  try { if (current.recorder?.state === 'recording') current.recorder.stop(); } catch (e) { /* ya detenida */ }
  try { current.data?.close(); } catch (e) { /* ya cerrado */ }
  try { current.media?.close(); } catch (e) { /* ya cerrada */ }
  current.localStream?.getTracks().forEach(track => track.stop());
  if (current.remoteAudio) current.remoteAudio.srcObject = null;
  if (state === current) state = null;
  current.events.onEnded(messageKey);
}
