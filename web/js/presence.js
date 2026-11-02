// Presencia: mientras la página está abierta con la sesión iniciada, este número recibe llamadas.
// Se ocupa el número en el directorio y se renueva "lastSeen" cada 40 s (luz verde para quien llama).
import { getDocument, setDocument, updateDocument } from './firestore.js';

const HEARTBEAT_MS = 40 * 1000;
let heartbeat = null;

function newPeerId() {
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789';
  const random = crypto.getRandomValues(new Uint8Array(16));
  return 'traduya-' + Array.from(random, byte => alphabet[byte % alphabet.length]).join('');
}

/**
 * Ocupa [phone] en el directorio para esta cuenta y empieza a renovar la presencia.
 * Devuelve el identificador de la conexión, o null si el número ya lo usa otro teléfono (por ejemplo, la app).
 */
export async function goOnline(uid, phone, getToken) {
  const path = `numbers/${encodeURIComponent(phone)}`;
  const token = await getToken();
  let peerId;
  try {
    const existing = await getDocument(path, token);
    if (existing && existing.owner !== uid) return null;
    peerId = existing?.peerId ?? newPeerId();
    await setDocument(path, { owner: uid, peerId, lastSeen: new Date() }, token);
  } catch (e) {
    return null;
  }
  clearInterval(heartbeat);
  heartbeat = setInterval(async () => {
    try { await updateDocument(path, { lastSeen: new Date() }, await getToken()); } catch (e) { /* se reintenta */ }
  }, HEARTBEAT_MS);
  return peerId;
}

export function goOffline() {
  clearInterval(heartbeat);
  heartbeat = null;
}
