// Ayudas para las pruebas contra los emuladores de Firebase (auth y firestore).
export const PROJECT = 'demo-traduya';
const AUTH = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
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

/** Abre el enlace «Confirmar registro» que Firebase envía al correo (el emulador lo guarda en vez de enviarlo). */
export async function signInWithEmailLink(email) {
  await fetch(`${AUTH}/accounts:sendOobCode?key=fake`, {
    method: 'POST', headers: JSON_HEADERS,
    body: JSON.stringify({ requestType: 'EMAIL_SIGNIN', email, continueUrl: 'http://127.0.0.1:5180/', canHandleCodeInApp: true }),
  });
  const codes = await (await fetch(`http://127.0.0.1:9099/emulator/v1/projects/${PROJECT}/oobCodes`)).json();
  const { oobCode } = codes.oobCodes.filter(code => code.email === email).pop();
  const res = await fetch(`${AUTH}/accounts:signInWithEmailLink?key=fake`, {
    method: 'POST', headers: JSON_HEADERS, body: JSON.stringify({ email, oobCode }),
  });
  const json = await res.json();
  return { uid: json.localId, token: json.idToken, email };
}

/** Escribe un documento con la API REST (PATCH crea o reemplaza). */
export function writeDocument(path, user, fields) {
  const body = { fields: {} };
  Object.entries(fields).forEach(([key, value]) => {
    body.fields[key] = value instanceof Date ? { timestampValue: value.toISOString() } : { stringValue: value };
  });
  return fetch(`${DOCS}/${path}`, { method: 'PATCH', headers: auth(user), body: JSON.stringify(body) });
}

/** Registro completo como lo hace la página: Google → enlace del correo → ficha. Devuelve la sesión de Google. */
export async function registeredCaller(fullName = 'Sarah Johnson', phone = '+13055550101') {
  const google = await signInWithGoogle();
  const link = await signInWithEmailLink(google.email);
  await writeDocument(`callers/${link.uid}`, link, { fullName, phone, email: link.email, registeredAt: new Date() });
  return google;
}
