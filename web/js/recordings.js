// Grabaciones de las llamadas de la web: se guardan en este mismo teléfono o computadora (IndexedDB),
// porque guardar archivos en Firebase requiere el plan de pago.

const DB_NAME = 'traduya';
const STORE = 'recordings';

function open() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1);
    request.onupgradeneeded = () => request.result.createObjectStore(STORE);
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function run(mode, action) {
  const db = await open();
  return new Promise((resolve, reject) => {
    const request = action(db.transaction(STORE, mode).objectStore(STORE));
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export const saveRecording = (callId, blob) => run('readwrite', store => store.put(blob, callId)).catch(() => {});

/** La grabación de la llamada, o null si no se hizo en este dispositivo. */
export const getRecording = callId => run('readonly', store => store.get(callId)).then(blob => blob ?? null, () => null);
