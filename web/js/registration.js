// Registro de quien llama: nombre completo + teléfono, y un código de 6 dígitos que llega a su Gmail.
import {
  connectFunctionsEmulator, getFunctions, httpsCallable,
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-functions.js';
import { firebaseApp } from './auth.js';
import { FUNCTIONS_REGION, USE_EMULATOR } from './config.js';

const functions = getFunctions(firebaseApp, FUNCTIONS_REGION);
if (USE_EMULATOR) connectFunctionsEmulator(functions, '127.0.0.1', 5001);

const sendCode = httpsCallable(functions, 'sendRegistrationCode');
const confirmCode = httpsCallable(functions, 'confirmRegistration');

/** Error con la clave del texto que se muestra a la persona (ver i18n.js). */
export class RegistrationError extends Error {
  constructor(messageKey) {
    super(messageKey);
    this.messageKey = messageKey;
  }
}

function asRegistrationError(error) {
  return new RegistrationError(error?.details?.reason ?? 'connectionError');
}

/** Envía el código al Gmail de la cuenta. Devuelve el correo al que se envió (y el código en el emulador). */
export async function requestCode({ fullName, phone, lang }) {
  try {
    return (await sendCode({ fullName, phone, lang })).data;
  } catch (error) {
    throw asRegistrationError(error);
  }
}

export async function confirm(code) {
  try {
    await confirmCode({ code });
  } catch (error) {
    throw asRegistrationError(error);
  }
}
