// Sesión de quien llama: entra con su cuenta de Google (Firebase Authentication) y confirma su registro
// con el enlace que Firebase envía gratis a su Gmail.
import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import {
  GoogleAuthProvider,
  connectAuthEmulator,
  getAuth,
  isSignInWithEmailLink,
  onAuthStateChanged,
  sendSignInLinkToEmail,
  signInWithEmailLink,
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

/** El usuario actual (o null). */
export const currentUser = () => auth.currentUser;

/**
 * true si la sesión viene del enlace del correo: demuestra que la persona abrió su Gmail.
 * Solo con esa sesión las reglas dejan crear la ficha de registro.
 */
export async function cameFromEmailLink() {
  const result = await auth.currentUser.getIdTokenResult();
  return result.signInProvider === 'password';
}

// region Enlace de confirmación

const PENDING_EMAIL_KEY = 'traduya.pendingEmail';
/** Parámetros de la página que se conservan al volver del enlace del correo. */
const KEPT_PARAMS = ['n', 'to', 'emulator'];

export async function sendConfirmationLink(email) {
  await sendSignInLinkToEmail(auth, email, { url: pageUrl(), handleCodeInApp: true });
  remember(PENDING_EMAIL_KEY, email);
}

export const isConfirmationLink = () => isSignInWithEmailLink(auth, location.href);

/**
 * Termina de entrar con el enlace del correo. [askEmail] se usa si el enlace se abrió en otro navegador
 * y no sabemos el correo. Devuelve false si la persona no escribió el correo.
 */
export async function completeConfirmationLink(askEmail) {
  const email = recall(PENDING_EMAIL_KEY) || askEmail();
  if (!email) return false;
  await signInWithEmailLink(auth, email.trim(), location.href);
  forget(PENDING_EMAIL_KEY);
  history.replaceState(null, '', pageUrl());
  return true;
}

/** La dirección actual sin los parámetros que añade el enlace del correo. */
function pageUrl() {
  const current = new URLSearchParams(location.search);
  const kept = new URLSearchParams();
  KEPT_PARAMS.forEach(name => { if (current.has(name)) kept.set(name, current.get(name)); });
  const query = kept.toString();
  return location.origin + location.pathname + (query ? `?${query}` : '');
}

function remember(key, value) {
  try { localStorage.setItem(key, value); } catch (e) { /* almacenamiento no disponible */ }
}

function recall(key) {
  try { return localStorage.getItem(key); } catch (e) { return null; }
}

function forget(key) {
  try { localStorage.removeItem(key); } catch (e) { /* almacenamiento no disponible */ }
}

// endregion
