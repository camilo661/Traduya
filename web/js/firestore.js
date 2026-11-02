// Cliente mínimo de la API REST de Firestore: leer, crear y borrar documentos con la sesión de quien llama.
import { FIRESTORE_DOCUMENTS_URL } from './config.js';

const headers = idToken => ({ 'Content-Type': 'application/json', Authorization: `Bearer ${idToken}` });

/** Convierte { campo: 'texto' } al formato de Firestore ({ fields: { campo: { stringValue } } }). */
function toFields(data) {
  const fields = {};
  Object.entries(data).forEach(([key, value]) => {
    fields[key] = value instanceof Date ? { timestampValue: value.toISOString() } : { stringValue: String(value) };
  });
  return { fields };
}

function fromFields(document) {
  const data = {};
  Object.entries(document.fields ?? {}).forEach(([key, value]) => {
    data[key] = value.stringValue ?? value.timestampValue ?? null;
  });
  return data;
}

/** El documento como objeto, o null si no existe. Lanza un error si no hay conexión o permiso. */
export async function getDocument(path, idToken) {
  const response = await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}`, { headers: headers(idToken) });
  if (response.status === 404) return null;
  if (!response.ok) throw new Error(`firestore ${response.status}`);
  return fromFields(await response.json());
}

/** Crea o reemplaza el documento [path] con [data]. */
export async function setDocument(path, data, idToken) {
  const response = await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}`, {
    method: 'PATCH', headers: headers(idToken), body: JSON.stringify(toFields(data)),
  });
  if (!response.ok) throw new Error(`firestore ${response.status}`);
}

export async function deleteDocument(path, idToken) {
  const response = await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}`, { method: 'DELETE', headers: headers(idToken) });
  if (!response.ok && response.status !== 404) throw new Error(`firestore ${response.status}`);
}
