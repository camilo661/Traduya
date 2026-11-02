// Comprueba quién llama: valida su sesión con Firebase y lee su ficha (web) o su número (app).
import { FIREBASE_CONFIG, USE_EMULATOR, AUTH_EMULATOR_URL } from './config.js';
import { getDocument } from './firestore.js';

const LOOKUP_URL = (USE_EMULATOR ? `${AUTH_EMULATOR_URL}/identitytoolkit.googleapis.com` : 'https://identitytoolkit.googleapis.com')
  + `/v1/accounts:lookup?key=${FIREBASE_CONFIG.apiKey}`;

/**
 * { name, phone, kind } de quien llama, o null si su sesión no es válida o no está registrado.
 * kind: 'web' (registrado en la página) o 'app' (teléfono con Traduya).
 */
export async function identifyCaller(callerToken, myToken) {
  if (!callerToken) return null;
  try {
    const response = await fetch(LOOKUP_URL, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ idToken: callerToken }),
    });
    const uid = (await response.json()).users?.[0]?.localId;
    if (!response.ok || !uid) return null;
    const web = await getDocument(`callers/${uid}`, myToken);
    if (web) return { name: web.fullName, phone: web.phone, kind: 'web' };
    const app = await getDocument(`appUsers/${uid}`, myToken);
    if (app) return { name: app.phone, phone: app.phone, kind: 'app' };
  } catch (e) { /* sin conexión: se rechaza */ }
  return null;
}
