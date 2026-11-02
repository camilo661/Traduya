// Ajustes del registro de quien llama.
import { defineSecret } from 'firebase-functions/params';

/** Misma región que la base de datos (São Paulo), para responder rápido. */
export const REGION = 'southamerica-east1';

export const CODE_TTL_MS = 10 * 60 * 1000;
export const MAX_ATTEMPTS = 5;
export const RESEND_COOLDOWN_MS = 60 * 1000;
export const MAX_CODES_PER_DAY = 5;

/** Cuenta de Gmail que envía los códigos y su contraseña de aplicación (Secret Manager). */
export const GMAIL_USER = defineSecret('GMAIL_USER');
export const GMAIL_APP_PASSWORD = defineSecret('GMAIL_APP_PASSWORD');

/** true en el emulador local: el código no se envía por correo, se devuelve para las pruebas. */
export const IS_EMULATOR = process.env.FUNCTIONS_EMULATOR === 'true';
