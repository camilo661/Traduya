// Pruebas del registro con código de 6 dígitos (funciones sendRegistrationCode y confirmRegistration).
// En el emulador el código no se envía por correo: la función lo devuelve como "debugCode".
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { DOCS, auth, callFunction, signInWithGoogle, signUpAnonymous } from './emulator.mjs';

const reason = response => response.error?.details?.reason;
const form = { fullName: 'Sarah Johnson', phone: '+1 (305) 555-0101', lang: 'es' };

test('registro completo: código correcto → ficha creada y cuenta marcada como registrada', async () => {
  const user = await signInWithGoogle();
  const sent = await callFunction('sendRegistrationCode', user, form);
  assert.equal(sent.result.email, user.email);
  assert.match(sent.result.debugCode, /^\d{6}$/);

  const confirmed = await callFunction('confirmRegistration', user, { code: sent.result.debugCode });
  assert.deepEqual(confirmed.result, { registered: true });

  const renewed = await signInWithGoogle({ id: user.id });
  const claims = JSON.parse(Buffer.from(renewed.token.split('.')[1], 'base64url'));
  assert.equal(claims.registered, true);

  // La app de quien recibe (usuario anónimo) puede leer la ficha
  const phone = await signUpAnonymous();
  const caller = await (await fetch(`${DOCS}/callers/${user.uid}`, { headers: auth(phone) })).json();
  assert.equal(caller.fields.fullName.stringValue, 'Sarah Johnson');
  assert.equal(caller.fields.phone.stringValue, '+13055550101');
  assert.equal(caller.fields.email.stringValue, user.email);
});

test('un código equivocado no registra y tras 5 intentos se bloquea', async () => {
  const user = await signInWithGoogle();
  const sent = await callFunction('sendRegistrationCode', user, form);
  const wrong = sent.result.debugCode === '000000' ? '111111' : '000000';

  for (let i = 0; i < 5; i++) {
    assert.equal(reason(await callFunction('confirmRegistration', user, { code: wrong })), 'wrongCode');
  }
  const blocked = await callFunction('confirmRegistration', user, { code: sent.result.debugCode });
  assert.equal(reason(blocked), 'tooManyAttempts');
});

test('pide nombre completo y teléfono con código de país', async () => {
  const user = await signInWithGoogle();
  assert.equal(reason(await callFunction('sendRegistrationCode', user, { ...form, fullName: 'Sarah' })), 'invalidName');
  assert.equal(reason(await callFunction('sendRegistrationCode', user, { ...form, phone: '3055550101' })), 'invalidPhone');
});

test('no deja pedir otro código antes de un minuto', async () => {
  const user = await signInWithGoogle();
  await callFunction('sendRegistrationCode', user, form);
  assert.equal(reason(await callFunction('sendRegistrationCode', user, form)), 'waitBeforeResend');
});

test('solo se registran cuentas de Google con sesión', async () => {
  assert.equal(reason(await callFunction('sendRegistrationCode', null, form)), 'signInRequired');
  const anonymous = await signUpAnonymous();
  assert.equal(reason(await callFunction('sendRegistrationCode', anonymous, form)), 'googleRequired');
  const unverified = await signInWithGoogle({ emailVerified: false });
  assert.equal(reason(await callFunction('sendRegistrationCode', unverified, form)), 'googleRequired');
});

test('quien ya está registrado no puede volver a registrarse', async () => {
  const user = await signInWithGoogle();
  const sent = await callFunction('sendRegistrationCode', user, form);
  await callFunction('confirmRegistration', user, { code: sent.result.debugCode });
  const renewed = await signInWithGoogle({ id: user.id });
  assert.equal(reason(await callFunction('sendRegistrationCode', renewed, form)), 'alreadyRegistered');
});
