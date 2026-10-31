// Pruebas de las reglas del directorio de números contra el emulador de Firebase.
// Uso: firebase emulators:exec --only auth,firestore --project demo-traduya "node --test test/"
import { test } from 'node:test';
import assert from 'node:assert/strict';

const PROJECT = 'demo-traduya';
const AUTH = 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1';
const DOCS = `http://127.0.0.1:8080/v1/projects/${PROJECT}/databases/(default)/documents`;
const JSON_HEADERS = { 'Content-Type': 'application/json' };

async function signUp() {
  const res = await fetch(`${AUTH}/accounts:signUp?key=fake`, {
    method: 'POST', headers: JSON_HEADERS, body: JSON.stringify({ returnSecureToken: true }),
  });
  const json = await res.json();
  return { uid: json.localId, token: json.idToken };
}

function entry(owner, peerId) {
  return JSON.stringify({ fields: { owner: { stringValue: owner }, peerId: { stringValue: peerId } } });
}

const randomNumber = () => '+57300' + Math.floor(1_000_000 + Math.random() * 8_999_999);
const docUrl = number => `${DOCS}/numbers/${encodeURIComponent(number)}`;
const auth = user => ({ ...JSON_HEADERS, Authorization: `Bearer ${user.token}` });

function create(user, number, peerId = 'traduya-abcdefgh12345678', owner = user.uid) {
  return fetch(`${DOCS}/numbers?documentId=${encodeURIComponent(number)}`, {
    method: 'POST', headers: auth(user), body: entry(owner, peerId),
  });
}

test('un usuario registra un número libre y cualquiera lo puede buscar', async () => {
  const user = await signUp();
  const number = randomNumber();
  assert.equal((await create(user, number)).status, 200);

  const found = await (await fetch(docUrl(number))).json();
  assert.equal(found.fields.peerId.stringValue, 'traduya-abcdefgh12345678');
});

test('nadie puede quitarle el número a quien lo registró', async () => {
  const owner = await signUp();
  const intruder = await signUp();
  const number = randomNumber();
  await create(owner, number);

  assert.equal((await create(intruder, number)).status, 409);
  const update = await fetch(docUrl(number), {
    method: 'PATCH', headers: auth(intruder), body: entry(intruder.uid, 'traduya-zzzzzzzzzzzzzzzz'),
  });
  assert.equal(update.status, 403);
  assert.equal((await fetch(docUrl(number), { method: 'DELETE', headers: auth(intruder) })).status, 403);
});

test('el dueño puede cambiar y borrar su número', async () => {
  const owner = await signUp();
  const number = randomNumber();
  await create(owner, number);

  const update = await fetch(docUrl(number), {
    method: 'PATCH', headers: auth(owner), body: entry(owner.uid, 'traduya-0000aaaa1111bbbb'),
  });
  assert.equal(update.status, 200);
  assert.equal((await fetch(docUrl(number), { method: 'DELETE', headers: auth(owner) })).status, 200);
  assert.equal((await fetch(docUrl(number))).status, 404);
});

test('rechaza registros a nombre de otro, sin sesión o con datos inválidos', async () => {
  const user = await signUp();
  assert.equal((await create(user, randomNumber(), undefined, 'otro-usuario')).status, 403);
  assert.equal((await create(user, '3001234567')).status, 403);
  assert.equal((await create(user, randomNumber(), 'cualquier-cosa')).status, 403);

  const anonymous = await fetch(`${DOCS}/numbers?documentId=${encodeURIComponent(randomNumber())}`, {
    method: 'POST', headers: JSON_HEADERS, body: entry('x', 'traduya-abcdefgh12345678'),
  });
  assert.equal(anonymous.status, 403);
});

test('no se puede descargar la lista de todos los números', async () => {
  const user = await signUp();
  assert.equal((await fetch(`${DOCS}/numbers`, { headers: auth(user) })).status, 403);
});
