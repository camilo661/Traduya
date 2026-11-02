// Pruebas de las reglas del registro gratuito: borrador con Google → enlace del correo → ficha.
import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  DOCS, auth, registeredCaller, signInWithEmailLink, signInWithGoogle, signUpAnonymous, writeDocument,
} from './emulator.mjs';

const profile = { fullName: 'Sarah Johnson', phone: '+13055550101' };
const card = user => ({ ...profile, email: user.email, registeredAt: new Date() });

test('registro completo: borrador con Google y ficha con la sesión del enlace del correo', async () => {
  const google = await signInWithGoogle();
  assert.equal((await writeDocument(`pendingCallers/${google.uid}`, google, profile)).status, 200);

  const link = await signInWithEmailLink(google.email);
  assert.equal(link.uid, google.uid, 'el enlace entra a la misma cuenta');
  const draft = await (await fetch(`${DOCS}/pendingCallers/${link.uid}`, { headers: auth(link) })).json();
  assert.equal(draft.fields.fullName.stringValue, 'Sarah Johnson');
  assert.equal((await writeDocument(`callers/${link.uid}`, link, card(link))).status, 200);

  // La app de quien recibe (usuario anónimo) puede leer la ficha
  const phone = await signUpAnonymous();
  const caller = await (await fetch(`${DOCS}/callers/${google.uid}`, { headers: auth(phone) })).json();
  assert.equal(caller.fields.phone.stringValue, '+13055550101');
});

test('sin abrir el enlace del correo no se puede crear la ficha', async () => {
  const google = await signInWithGoogle();
  assert.equal((await writeDocument(`callers/${google.uid}`, google, card(google))).status, 403);
  const anonymous = await signUpAnonymous();
  assert.equal((await writeDocument(`callers/${anonymous.uid}`, anonymous, card({ email: 'x@example.com' }))).status, 403);
});

test('la ficha exige nombre completo, teléfono con país y el mismo correo de la sesión', async () => {
  const link = await signInWithEmailLink((await signInWithGoogle()).email);
  assert.equal((await writeDocument(`callers/${link.uid}`, link, { ...card(link), fullName: 'Sarah' })).status, 403);
  assert.equal((await writeDocument(`callers/${link.uid}`, link, { ...card(link), phone: '3055550101' })).status, 403);
  assert.equal((await writeDocument(`callers/${link.uid}`, link, { ...card(link), email: 'otro@example.com' })).status, 403);
});

test('nadie crea fichas a nombre de otra cuenta ni cambia una ficha ya creada', async () => {
  const victim = await registeredCaller();
  const intruder = await signInWithEmailLink((await signInWithGoogle()).email);
  assert.equal((await writeDocument(`callers/${victim.uid}`, intruder, card(intruder))).status, 403);
  const owner = await signInWithEmailLink(victim.email);
  assert.equal((await writeDocument(`callers/${victim.uid}`, owner, { ...card(owner), fullName: 'Otra Persona' })).status, 403);
});

test('el borrador solo lo ve y lo escribe su dueño', async () => {
  const owner = await signInWithGoogle();
  const other = await signInWithGoogle();
  await writeDocument(`pendingCallers/${owner.uid}`, owner, profile);
  assert.equal((await fetch(`${DOCS}/pendingCallers/${owner.uid}`, { headers: auth(other) })).status, 403);
  assert.equal((await writeDocument(`pendingCallers/${owner.uid}`, other, profile)).status, 403);
  assert.equal((await writeDocument(`pendingCallers/${owner.uid}`, owner, { ...profile, phone: 'abc' })).status, 403);
});
