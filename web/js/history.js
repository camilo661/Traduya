// Historial de llamadas de la web, guardado en la cuenta (Firestore): quién, cuándo, cuánto duró
// y todo lo que dijo la otra persona, traducido.
import { listDocuments, setDocument, updateDocument } from './firestore.js';

const newId = () => `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 8)}`;

/** Empieza una llamada en el historial y devuelve su identificador. */
export async function startCallRecord(uid, token, { direction, peerName, peerPhone, myLanguage }) {
  const id = newId();
  await setDocument(`callers/${uid}/calls/${id}`, {
    direction, peerName, peerPhone, myLanguage, startedAt: new Date(), durationSeconds: 0,
  }, token);
  return id;
}

export async function finishCallRecord(uid, token, id, { durationSeconds, peerLanguage }) {
  await updateDocument(`callers/${uid}/calls/${id}`, { durationSeconds, peerLanguage: peerLanguage ?? '' }, token);
}

/** Guarda una frase de la otra persona: original y traducida. */
export async function addLine(uid, token, callId, { original, translated, from }) {
  await setDocument(`callers/${uid}/calls/${callId}/lines/${newId()}`, {
    original, translated, from, at: new Date(),
  }, token);
}

export const listCalls = (uid, token) => listDocuments(`callers/${uid}/calls`, token, { orderBy: 'startedAt desc' });

export const listLines = (uid, token, callId) =>
  listDocuments(`callers/${uid}/calls/${callId}/lines`, token, { orderBy: 'at', pageSize: 300 });
