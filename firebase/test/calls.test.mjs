// Pruebas de las reglas para recibir llamadas en la web: número propio, presencia, usuarios de la app e historial.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  DOCS, auth, registeredCaller, signInWithGoogle, signUpAnonymous, writeDocument,
} from './emulator.mjs';

const randomPhone = () => '+57310' + Math.floor(1_000_000 + Math.random() * 8_999_999);
const peerId = 'traduya-webweb0000000001';
const entry = owner => ({ owner, peerId, lastSeen: new Date() });

test('una cuenta web ocupa su propio teléfono y renueva su presencia', async () => {
  const phone = randomPhone();
  const web = await registeredCaller('Ana Ruiz', phone);
  assert.equal((await writeDocument(`numbers/${encodeURIComponent(phone)}`, web, entry(web.uid))).status, 200);
  const touch = await fetch(`${DOCS}/numbers/${encodeURIComponent(phone)}?updateMask.fieldPaths=lastSeen`, {
    method: 'PATCH', headers: auth(web),
    body: JSON.stringify({ fields: { lastSeen: { timestampValue: new Date().toISOString() } } }),
  });
  assert.equal(touch.status, 200);
});

test('una cuenta web no puede ocupar el teléfono de otra persona', async () => {
  const web = await registeredCaller('Ana Ruiz', randomPhone());
  assert.equal((await writeDocument(`numbers/${encodeURIComponent(randomPhone())}`, web, entry(web.uid))).status, 403);
});

test('el teléfono con la app se identifica y puede buscar números', async () => {
  const phone = randomPhone();
  const app = await signUpAnonymous();
  assert.equal((await writeDocument(`numbers/${encodeURIComponent(phone)}`, app, entry(app.uid))).status, 200);
  assert.equal((await writeDocument(`appUsers/${app.uid}`, app, { phone })).status, 200);

  const webPhone = randomPhone();
  const web = await registeredCaller('Ana Ruiz', webPhone);
  await writeDocument(`numbers/${encodeURIComponent(webPhone)}`, web, entry(web.uid));
  const found = await fetch(`${DOCS}/numbers/${encodeURIComponent(webPhone)}`, { headers: auth(app) });
  assert.equal(found.status, 200);

  // Quien recibe en la web lee el teléfono de la app que llama
  const card = await (await fetch(`${DOCS}/appUsers/${app.uid}`, { headers: auth(web) })).json();
  assert.equal(card.fields.phone.stringValue, phone);
});

test('un teléfono anónimo sin número registrado no puede buscar números', async () => {
  const stranger = await signUpAnonymous();
  const webPhone = randomPhone();
  const web = await registeredCaller('Ana Ruiz', webPhone);
  await writeDocument(`numbers/${encodeURIComponent(webPhone)}`, web, entry(web.uid));
  assert.equal((await fetch(`${DOCS}/numbers/${encodeURIComponent(webPhone)}`, { headers: auth(stranger) })).status, 403);
  assert.equal((await writeDocument(`appUsers/${stranger.uid}`, stranger, { phone: 'abc' })).status, 403);
});

test('el historial web solo lo ve y lo escribe su dueño', async () => {
  const owner = await signInWithGoogle();
  const other = await signInWithGoogle();
  const call = `callers/${owner.uid}/calls/c1`;
  assert.equal((await writeDocument(call, owner, { peerNumber: '+573001112233', direction: 'in' })).status, 200);
  assert.equal((await writeDocument(`${call}/lines/l1`, owner, { translated: 'Hola' })).status, 200);
  assert.equal((await fetch(`${DOCS}/${call}`, { headers: auth(other) })).status, 403);
  assert.equal((await writeDocument(`${call}/lines/l2`, other, { translated: 'x' })).status, 403);
});
