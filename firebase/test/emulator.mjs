// Ayudas para las pruebas contra los emuladores de Firebase (auth, firestore y functions).
export const PROJECT = 'demo-traduya';
export const REGION = 'southamerica-east1';
const AUTH = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
const FUNCTIONS = `http://127.0.0.1:5001/${PROJECT}/${REGION}`;
export const DOCS = `http://127.0.0.1:8080/v1/projects/${PROJECT}/databases/(default)/documents`;
export const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const auth = user => ({ ...JSON_HEADERS, Authorization: `Bearer ${user.token}` });

/** Usuario anónimo, como el que usa la app en el teléfono. */
export async function signUpAnonymous() {
  const res = await fetch(`${AUTH}/accounts:signUp?key=fake`, {
    method: 'POST', headers: JSON_HEADERS, body: JSON.stringify({ returnSecureToken: true }),
  });
  const json = await res.json();
  return { uid: json.localId, token: json.idToken };
}

/** Usuario de Google simulado (el emulador acepta tokens sin firmar). Repetirlo con el mismo id renueva el token. */
export async function signInWithGoogle({ id = Math.random().toString(36).slice(2), emailVerified = true } = {}) {
  const idToken = JSON.stringify({ sub: 'g' + id, email: `caller-${id}@example.com`, email_verified: emailVerified });
  const res = await fetch(`${AUTH}/accounts:signInWithIdp?key=fake`, {
    method: 'POST',
    headers: JSON_HEADERS,
    body: JSON.stringify({
      postBody: `providerId=google.com&id_token=${encodeURIComponent(idToken)}`,
      requestUri: 'http://localhost', returnSecureToken: true,
    }),
  });
  const json = await res.json();
  return { id, uid: json.localId, token: json.idToken, email: json.email };
}

/** Marca a un usuario como registrado sin pasar por el código (solo para probar las reglas). */
export async function markRegistered(user) {
  await fetch(`http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/projects/${PROJECT}/accounts:update`, {
    method: 'POST',
    headers: { ...JSON_HEADERS, Authorization: 'Bearer owner' },
    body: JSON.stringify({ localId: user.uid, customAttributes: JSON.stringify({ registered: true }) }),
  });
  return signInWithGoogle({ id: user.id });
}

/** Llama a una función "callable" como lo hace el SDK web. Devuelve { result } o { error }. */
export async function callFunction(name, user, data) {
  const res = await fetch(`${FUNCTIONS}/${name}`, {
    method: 'POST', headers: user ? auth(user) : JSON_HEADERS, body: JSON.stringify({ data }),
  });
  return res.json();
}
