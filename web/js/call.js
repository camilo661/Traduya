// La llamada WebRTC con PeerJS: micrófono, conexión y audio de la otra persona.
/* global Peer */
import { ICE_SERVERS } from './config.js';

let peer = null;
let call = null;
let localStream = null;
let remoteAudio = null;
let connected = false;
let ended = true;

/**
 * Llama a [peerId]. [idToken] viaja con la llamada para que Traduya compruebe quién llama.
 * [events] recibe: onStatus(clave del texto), onConnected(), onEnded(clave del texto o null).
 */
export async function startCall(peerId, idToken, events) {
  ended = false;
  // Cada llamada termina una sola vez, con el primer motivo (el cierre posterior no lo tapa)
  const end = messageKey => {
    if (ended) return;
    ended = true;
    finish();
    events.onEnded(messageKey);
  };

  // El audio remoto se prepara dentro del toque del usuario para que iOS permita reproducirlo
  remoteAudio = new Audio();
  remoteAudio.playsInline = true;
  remoteAudio.autoplay = true;

  try {
    localStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true },
    });
  } catch (e) {
    end('micBlocked');
    return;
  }

  peer = new Peer({ config: { iceServers: ICE_SERVERS }, debug: 1 });
  peer.on('error', error => {
    // Con la llamada ya conectada el audio va directo: los avisos del servidor no la cortan
    if (connected) return;
    end(error.type === 'peer-unavailable' ? 'unavailable' : 'connectionError');
  });
  peer.on('disconnected', () => { if (!connected) events.onStatus('connecting'); });
  peer.on('open', () => {
    events.onStatus('ringing');
    call = peer.call(peerId, localStream, { metadata: { idToken } });
    call.on('stream', stream => {
      remoteAudio.srcObject = stream;
      remoteAudio.play().catch(() => {});
      connected = true;
      events.onConnected();
    });
    call.on('close', () => end('ended'));
    call.on('error', () => end('ended'));
    const connection = call.peerConnection;
    if (connection) {
      connection.addEventListener('connectionstatechange', () => {
        if (connection.connectionState === 'failed') end('lost');
      });
    }
  });
}

export function hangUp() {
  ended = true;
  finish();
}

export function setMuted(muted) {
  if (localStream) localStream.getAudioTracks().forEach(track => { track.enabled = !muted; });
}

function finish() {
  connected = false;
  const current = call;
  call = null;
  try { if (current) current.close(); } catch (e) { /* ya cerrada */ }
  if (localStream) localStream.getTracks().forEach(track => track.stop());
  localStream = null;
  if (peer) { peer.destroy(); peer = null; }
  if (remoteAudio) remoteAudio.srcObject = null;
}
