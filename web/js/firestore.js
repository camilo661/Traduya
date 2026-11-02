// Cliente mínimo de la API REST de Firestore: leer, listar, crear, actualizar y borrar con la sesión de quien llama.
import { FIRESTORE_DOCUMENTS_URL } from './config.js';

const headers = idToken => ({ 'Content-Type': 'application/json', Authorization: `Bearer ${idToken}` });

/** Convierte { campo: valor } al formato de Firestore (texto, número entero o fecha). */
function toFields(data) {
  const fields = {};
  Object.entries(data).forEach(([key, value]) => {
    if (value instanceof Date) fields[key] = { timestampValue: value.toISOString() };
    else if (typeof value === 'number') fields[key] = { integerValue: String(Math.round(value)) };
    else fields[key] = { stringValue: String(value) };
  });
  return { fields };
}

function fromFields(document) {
  const data = { id: document.name?.split('/').pop() };
  Object.entries(document.fields ?? {}).forEach(([key, value]) => {
    if ('integerValue' in value) data[key] = Number(value.integerValue);
    else if ('timestampValue' in value) data[key] = new Date(value.timestampValue);
    else data[key] = value.stringValue ?? null;
  });
  return data;
}

async function check(response) {
  if (!response.ok && response.status !== 404) throw new Error(`firestore ${response.status}`);
  return response;
}

/** El documento como objeto, o null si no existe. Lanza un error si no hay conexión o permiso. */
export async function getDocument(path, idToken) {
  const response = await check(await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}`, { headers: headers(idToken) }));
  return response.status === 404 ? null : fromFields(await response.json());
}

/** Documentos de una colección, ordenados por [orderBy] (por ejemplo "startedAt desc"). */
export async function listDocuments(path, idToken, { orderBy = '', pageSize = 50 } = {}) {
  // El espacio de "startedAt desc" debe ir como %20: Firestore no acepta el "+" de URLSearchParams
  const query = `pageSize=${pageSize}${orderBy ? `&orderBy=${encodeURIComponent(orderBy)}` : ''}`;
  const response = await check(await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}?${query}`, { headers: headers(idToken) }));
  if (response.status === 404) return [];
  return ((await response.json()).documents ?? []).map(fromFields);
}

/** Crea o reemplaza el documento [path] con [data]. */
export async function setDocument(path, data, idToken) {
  await check(await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}`, {
    method: 'PATCH', headers: headers(idToken), body: JSON.stringify(toFields(data)),
  }));
}

/** Cambia solo los campos de [data] y deja el resto del documento como está. */
export async function updateDocument(path, data, idToken) {
  const mask = new URLSearchParams();
  Object.keys(data).forEach(field => mask.append('updateMask.fieldPaths', field));
  await check(await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}?${mask}`, {
    method: 'PATCH', headers: headers(idToken), body: JSON.stringify(toFields(data)),
  }));
}

export async function deleteDocument(path, idToken) {
  await check(await fetch(`${FIRESTORE_DOCUMENTS_URL}/${path}`, { method: 'DELETE', headers: headers(idToken) }));
}
