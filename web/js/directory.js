// Busca en el directorio de Traduya a quién pertenece un número de teléfono.
import { FIRESTORE_DOCUMENTS_URL } from './config.js';

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

/** Convierte lo que escribió la persona (número o código) en el identificador de la conexión. */
export async function resolvePeerId(target, idToken) {
  if (!looksLikeNumber(target)) return 'traduya-' + target.toLowerCase();

  const number = normalizeNumber(target);
  if (!number) throw new CallError('needCountry');

  const response = await fetch(`${FIRESTORE_DOCUMENTS_URL}/numbers/${encodeURIComponent(number)}`, {
    headers: { Authorization: `Bearer ${idToken}` },
  });
  if (response.status === 404) throw new CallError('notRegistered');
  if (!response.ok) throw new CallError('connectionError');
  return (await response.json()).fields.peerId.stringValue;
}
