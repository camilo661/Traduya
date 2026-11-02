// Sesión de quien llama: se registra e inicia sesión con su cuenta de Google (Firebase Authentication).
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import {
  GoogleAuthProvider,
  connectAuthEmulator,
  getAuth,
  onAuthStateChanged,
  signInWithPopup,
  signInWithRedirect,
  signOut as firebaseSignOut,
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';
import { AUTH_EMULATOR_URL, FIREBASE_CONFIG, USE_EMULATOR } from './config.js';

export const firebaseApp = initializeApp(FIREBASE_CONFIG);
const auth = getAuth(firebaseApp);
if (USE_EMULATOR) connectAuthEmulator(auth, AUTH_EMULATOR_URL, { disableWarnings: true });

/** Avisa cada vez que cambia la sesión (usuario de Google o null). */
export function watchUser(callback) {
  onAuthStateChanged(auth, callback);
}

export function setAuthLanguage(language) {
  auth.languageCode = language;
}

export async function signInWithGoogle() {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: 'select_account' });
  try {
    await signInWithPopup(auth, provider);
  } catch (error) {
    // Safari puede bloquear la ventana emergente: entonces se usa la redirección
    if (error.code === 'auth/popup-blocked' || error.code === 'auth/operation-not-supported-in-this-environment') {
      await signInWithRedirect(auth, provider);
      return;
    }
    if (error.code !== 'auth/popup-closed-by-user' && error.code !== 'auth/cancelled-popup-request') throw error;
  }
}

export const signOut = () => firebaseSignOut(auth);

/** Token de la sesión actual: lo comprueba la app de quien recibe la llamada. */
export const idToken = () => auth.currentUser.getIdToken();

/** true si la cuenta terminó el registro (código verificado). [refresh] pide un token nuevo al servidor. */
export async function isRegistered(refresh = false) {
  const result = await auth.currentUser.getIdTokenResult(refresh);
  return result.claims.registered === true;
}
