// Busca en el directorio de Traduya a quién pertenece un número.
import { getDocument } from './firestore.js';

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

/** Convierte lo que se marcó (número o código antiguo) en el identificador de la conexión. */
export async function resolvePeerId(target, idToken) {
  if (!looksLikeNumber(target)) return 'traduya-' + target.toLowerCase();

  const number = normalizeNumber(target);
  if (!number) throw new CallError('needCountry');

  let entry;
  try {
    entry = await getDocument(`numbers/${encodeURIComponent(number)}`, idToken);
  } catch (e) {
    throw new CallError('connectionError');
  }
  if (!entry) throw new CallError('notRegistered');
  return entry.peerId;
}
