// Directorio de números de Traduya: a quién pertenece un número y si está activo para recibir.
import { getDocument } from './firestore.js';

/** Un número está activo si renovó su presencia hace menos de este tiempo (se renueva cada 40 s). */
export const ACTIVE_WINDOW_MS = 100 * 1000;

/** Error con la clave del texto que se muestra a la persona (ver i18n.js). */
export class CallError extends Error {
  constructor(messageKey) {
    super(messageKey);
    this.messageKey = messageKey;
  }
}

/** "+57 300 123-4567" o "0057…" → "+573001234567"; null si no es un número internacional válido. */
export function normalizeNumber(value) {
  const compact = value.replace(/[\s().-]/g, '');
  const international = compact.startsWith('00') ? '+' + compact.slice(2) : compact;
  return /^\+[1-9][0-9]{7,14}$/.test(international) ? international : null;
}

function looksLikeNumber(value) {
  return /^[+0]/.test(value) || /^[0-9\s().-]{8,}$/.test(value);
}

/** Busca un número: { peerId, active } o null si no está en Traduya. */
export async function lookupNumber(number, idToken) {
  const entry = await getDocument(`numbers/${encodeURIComponent(number)}`, idToken);
  if (!entry) return null;
  const active = entry.lastSeen instanceof Date && Date.now() - entry.lastSeen.getTime() < ACTIVE_WINDOW_MS;
  return { peerId: entry.peerId, active };
}

/** Convierte lo que se marcó (número o código antiguo) en el identificador de la conexión. */
export async function resolvePeerId(target, idToken) {
  if (!looksLikeNumber(target)) return 'traduya-' + target.toLowerCase();

  const number = normalizeNumber(target);
  if (!number) throw new CallError('needCountry');

  let entry;
  try {
    entry = await lookupNumber(number, idToken);
  } catch (e) {
    throw new CallError('connectionError');
  }
  if (!entry) throw new CallError('notRegistered');
  return entry.peerId;
}
