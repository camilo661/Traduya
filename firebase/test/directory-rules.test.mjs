// Pruebas de las reglas del directorio de números y de las fichas de quien llama.
// Uso (desde la raíz): firebase emulators:exec --only auth,firestore --project demo-traduya "node --test firebase/test/"
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DOCS, JSON_HEADERS, auth, registeredCaller, signInWithGoogle, signUpAnonymous } from './emulator.mjs';

function entry(owner, peerId) {
  return JSON.stringify({ fields: { owner: { stringValue: owner }, peerId: { stringValue: peerId } } });
}

const randomNumber = () => '+57300' + Math.floor(1_000_000 + Math.random() * 8_999_999);
const docUrl = number => `${DOCS}/numbers/${encodeURIComponent(number)}`;

function create(user, number, peerId = 'traduya-abcdefgh12345678', owner = user.uid) {
  return fetch(`${DOCS}/numbers?documentId=${encodeURIComponent(number)}`, {
    method: 'POST', headers: auth(user), body: entry(owner, peerId),
  });
}

test('un teléfono registra un número libre y quien se registró en la página lo puede buscar', async () => {
  const user = await signUpAnonymous();
  const number = randomNumber();
  assert.equal((await create(user, number)).status, 200);

  const caller = await registeredCaller();
  const found = await (await fetch(docUrl(number), { headers: auth(caller) })).json();
  assert.equal(found.fields.peerId.stringValue, 'traduya-abcdefgh12345678');
});

test('sin registrarse no se puede buscar un número, aunque haya iniciado sesión con Google', async () => {
  const owner = await signUpAnonymous();
  const number = randomNumber();
  await create(owner, number);

  assert.equal((await fetch(docUrl(number))).status, 403);
  assert.equal((await fetch(docUrl(number), { headers: auth(owner) })).status, 403);
  const notRegistered = await signInWithGoogle();
  assert.equal((await fetch(docUrl(number), { headers: auth(notRegistered) })).status, 403);
});

test('nadie puede quitarle el número a quien lo registró', async () => {
  const owner = await signUpAnonymous();
  const intruder = await signUpAnonymous();
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
  const owner = await signUpAnonymous();
  const number = randomNumber();
  await create(owner, number);

  const update = await fetch(docUrl(number), {
    method: 'PATCH', headers: auth(owner), body: entry(owner.uid, 'traduya-0000aaaa1111bbbb'),
  });
  assert.equal(update.status, 200);
  assert.equal((await fetch(docUrl(number), { method: 'DELETE', headers: auth(owner) })).status, 200);
  const caller = await registeredCaller();
  assert.equal((await fetch(docUrl(number), { headers: auth(caller) })).status, 404);
});

test('rechaza registros a nombre de otro, sin sesión o con datos inválidos', async () => {
  const user = await signUpAnonymous();
  assert.equal((await create(user, randomNumber(), undefined, 'otro-usuario')).status, 403);
  assert.equal((await create(user, '3001234567')).status, 403);
  assert.equal((await create(user, randomNumber(), 'cualquier-cosa')).status, 403);

  const anonymous = await fetch(`${DOCS}/numbers?documentId=${encodeURIComponent(randomNumber())}`, {
    method: 'POST', headers: JSON_HEADERS, body: entry('x', 'traduya-abcdefgh12345678'),
  });
  assert.equal(anonymous.status, 403);
});

test('no se puede descargar la lista de todos los números', async () => {
  const caller = await registeredCaller();
  assert.equal((await fetch(`${DOCS}/numbers`, { headers: auth(caller) })).status, 403);
});
