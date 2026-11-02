// Registro de quien llama, gratis y sin servidor:
// 1. Con su cuenta de Google escribe nombre completo y teléfono: se guardan como borrador (pendingCallers).
// 2. Firebase le envía a su Gmail un enlace «Confirmar registro».
// 3. Al abrir el enlace, el borrador pasa a ser su ficha (callers). Las reglas solo lo permiten con esa sesión.
import { getDocument, deleteDocument, setDocument } from './firestore.js';

/** Error con la clave del texto que se muestra a la persona (ver i18n.js). */
export class RegistrationError extends Error {
  constructor(messageKey) {
    super(messageKey);
    this.messageKey = messageKey;
  }
}

/** Mismas reglas que firebase/firestore.rules: nombre y apellido, y teléfono internacional (E.164). */
export function validateProfile({ fullName, phone }) {
  const name = fullName.trim().replace(/\s+/g, ' ');
  if (name.length < 3 || name.length > 80 || !name.includes(' ')) throw new RegistrationError('invalidName');
  const compact = phone.replace(/[\s().-]/g, '');
  if (!/^\+[1-9][0-9]{7,14}$/.test(compact)) throw new RegistrationError('invalidPhone');
  return { fullName: name, phone: compact };
}

/** true si la cuenta ya tiene su ficha de registro. */
export async function isRegistered(uid, idToken) {
  return (await getDocument(`callers/${uid}`, idToken)) !== null;
}

export async function fullNameOf(uid, idToken) {
  return (await getDocument(`callers/${uid}`, idToken))?.fullName ?? null;
}

/** Guarda el borrador para terminar el registro aunque el enlace se abra en otro navegador. */
export async function saveDraft(uid, profile, idToken) {
  await setDocument(`pendingCallers/${uid}`, profile, idToken);
}

/**
 * Crea la ficha con el borrador (o con [profile] si se da). Solo funciona con la sesión del enlace del correo.
 * Devuelve false si no hay datos para registrar.
 */
export async function finishRegistration(user, idToken, profile = null) {
  const data = profile ?? await getDocument(`pendingCallers/${user.uid}`, idToken);
  if (!data) return false;
  await setDocument(`callers/${user.uid}`, {
    fullName: data.fullName, phone: data.phone, email: user.email, registeredAt: new Date(),
  }, idToken);
  await deleteDocument(`pendingCallers/${user.uid}`, idToken).catch(() => {});
  return true;
}
