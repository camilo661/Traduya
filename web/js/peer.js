// Conexión WebRTC permanente (PeerJS): recibe llamadas en el número propio y hace las llamadas salientes.
/* global Peer */
import { ICE_SERVERS } from './config.js';

const RETRY_ERRORS = ['unavailable-id', 'network', 'server-error', 'socket-error', 'socket-closed'];
let peer = null;
let handlers = {};

/**
 * Conecta con el identificador [peerId] (o uno al azar si es null, solo para llamar).
 * [events]: onReady(), onOffline(), onIncoming(llamada), onData(conexión de datos), onError(tipo).
 */
export function startPeer(peerId, events) {
  stopPeer();
  handlers = events;
  connect(peerId);
}

function connect(peerId) {
  const options = { config: { iceServers: ICE_SERVERS }, debug: 1 };
  const current = peerId ? new Peer(peerId, options) : new Peer(options);
  peer = current;
  current.on('open', () => handlers.onReady?.());
  current.on('call', call => handlers.onIncoming?.(call));
  current.on('connection', connection => handlers.onData?.(connection));
  current.on('disconnected', () => {
    if (peer !== current) return;
    handlers.onOffline?.();
    setTimeout(() => { try { current.reconnect(); } catch (e) { /* se reintenta al fallar */ } }, 2000);
  });
  current.on('error', error => {
    if (peer !== current) return;
    handlers.onError?.(error.type);
    // El servidor aún recuerda la sesión anterior o se cortó la red: se vuelve a conectar
    if (RETRY_ERRORS.includes(error.type)) {
      setTimeout(() => { if (peer === current) { current.destroy(); connect(peerId); } }, 5000);
    }
  });
}

/** La conexión lista para usar; espera hasta 10 s a que se abra. */
export function readyPeer() {
  return new Promise((resolve, reject) => {
    if (!peer) { reject(new Error('sin conexión')); return; }
    if (peer.open) { resolve(peer); return; }
    const timer = setTimeout(() => reject(new Error('sin conexión')), 10000);
    peer.once('open', () => { clearTimeout(timer); resolve(peer); });
  });
}

export const isOnline = () => Boolean(peer?.open);

export function stopPeer() {
  const current = peer;
  peer = null;
  if (current) current.destroy();
}
