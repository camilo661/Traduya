// Registro de quien llama desde la página web:
// 1) sendRegistrationCode: guarda nombre y teléfono y envía un código de 6 dígitos a su Gmail.
// 2) confirmRegistration: comprueba el código, crea su ficha en "callers" y lo marca como registrado.
import { initializeApp } from 'firebase-admin/app';
import { getAuth } from 'firebase-admin/auth';
import { FieldValue, getFirestore } from 'firebase-admin/firestore';
import { HttpsError, onCall } from 'firebase-functions/v2/https';
import { matches, hashCode, newCode } from './codes.js';
import {
  CODE_TTL_MS, GMAIL_APP_PASSWORD, GMAIL_USER, IS_EMULATOR, MAX_ATTEMPTS, MAX_CODES_PER_DAY, REGION,
  RESEND_COOLDOWN_MS,
} from './config.js';
import { sendCodeEmail } from './mailer.js';
import { normalizeFullName, normalizePhone } from './phone.js';

initializeApp();
const db = getFirestore();
const LANGUAGES = ['es', 'en', 'zh'];

/** El error lleva en "details.reason" la clave del mensaje que muestra la página. */
const fail = (code, reason) => new HttpsError(code, reason, { reason });

/** Solo cuentas de Google con el correo verificado pueden registrarse. */
function googleUser(request) {
  const token = request.auth?.token;
  if (!token) throw fail('unauthenticated', 'signInRequired');
  if (token.firebase?.sign_in_provider !== 'google.com' || !token.email || token.email_verified !== true) {
    throw fail('permission-denied', 'googleRequired');
  }
  return { uid: request.auth.uid, email: token.email, registered: token.registered === true };
}

export const sendRegistrationCode = onCall(
  { region: REGION, secrets: [GMAIL_USER, GMAIL_APP_PASSWORD] },
  async request => {
    const user = googleUser(request);
    if (user.registered) throw fail('already-exists', 'alreadyRegistered');
    const fullName = normalizeFullName(request.data?.fullName);
    if (!fullName) throw fail('invalid-argument', 'invalidName');
    const phone = normalizePhone(request.data?.phone);
    if (!phone) throw fail('invalid-argument', 'invalidPhone');
    const lang = LANGUAGES.includes(request.data?.lang) ? request.data.lang : 'es';

    const code = newCode();
    const now = Date.now();
    const today = new Date(now).toISOString().slice(0, 10);
    const ref = db.collection('registrations').doc(user.uid);
    await db.runTransaction(async tx => {
      const previous = (await tx.get(ref)).data();
      if (previous && now - previous.sentAt < RESEND_COOLDOWN_MS) throw fail('resource-exhausted', 'waitBeforeResend');
      const sentToday = previous?.day === today ? previous.sentToday : 0;
      if (sentToday >= MAX_CODES_PER_DAY) throw fail('resource-exhausted', 'tooManyCodes');
      tx.set(ref, {
        fullName, phone, email: user.email, codeHash: hashCode(user.uid, code),
        expiresAt: now + CODE_TTL_MS, attempts: 0, sentAt: now, day: today, sentToday: sentToday + 1,
      });
    });

    if (IS_EMULATOR) return { email: user.email, debugCode: code };
    try {
      await sendCodeEmail({ to: user.email, name: fullName.split(' ')[0], code, lang });
    } catch (error) {
      console.error('No se pudo enviar el código', error);
      throw fail('unavailable', 'emailFailed');
    }
    return { email: user.email };
  },
);

export const confirmRegistration = onCall({ region: REGION }, async request => {
  const user = googleUser(request);
  if (user.registered) return { registered: true };
  const code = String(request.data?.code ?? '').replace(/\D/g, '');
  const ref = db.collection('registrations').doc(user.uid);

  // Los intentos fallidos se guardan, por eso el resultado se devuelve en vez de lanzar un error aquí dentro
  const outcome = await db.runTransaction(async tx => {
    const pending = (await tx.get(ref)).data();
    if (!pending) return 'noPendingCode';
    if (Date.now() > pending.expiresAt) return 'codeExpired';
    if (pending.attempts >= MAX_ATTEMPTS) return 'tooManyAttempts';
    if (code.length !== 6 || !matches(user.uid, code, pending.codeHash)) {
      tx.update(ref, { attempts: pending.attempts + 1 });
      return 'wrongCode';
    }
    tx.set(db.collection('callers').doc(user.uid), {
      fullName: pending.fullName,
      phone: pending.phone,
      email: pending.email,
      registeredAt: FieldValue.serverTimestamp(),
    });
    tx.delete(ref);
    return 'ok';
  });
  if (outcome !== 'ok') throw fail('failed-precondition', outcome);

  // La marca viaja en el token: las reglas y la app de quien recibe la comprueban
  await getAuth().setCustomUserClaims(user.uid, { registered: true });
  return { registered: true };
});
