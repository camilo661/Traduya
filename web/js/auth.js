// Inicio de sesión de quien llama: Google o enlace por correo (sin contraseña), con Firebase Authentication.
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

const auth = getAuth(initializeApp(FIREBASE_CONFIG));
if (USE_EMULATOR) connectAuthEmulator(auth, AUTH_EMULATOR_URL, { disableWarnings: true });

const PENDING_EMAIL_KEY = 'traduya.pendingEmail';
/** Parámetros de la página que se conservan al volver del enlace del correo. */
const KEPT_PARAMS = ['n', 'to', 'emulator', 'autocall'];

/** Avisa cada vez que cambia la sesión (usuario o null). */
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

export async function sendEmailLink(email) {
  await sendSignInLinkToEmail(auth, email, { url: pageUrl(), handleCodeInApp: true });
  remember(PENDING_EMAIL_KEY, email);
}

/**
 * Si la página se abrió desde el enlace del correo, termina de iniciar sesión.
 * [askEmail] se usa cuando el enlace se abre en otro navegador y no sabemos el correo.
 */
export async function completeEmailLinkSignIn(askEmail) {
  if (!isSignInWithEmailLink(auth, location.href)) return;
  const email = recall(PENDING_EMAIL_KEY) || askEmail();
  if (!email) return;
  await signInWithEmailLink(auth, email, location.href);
  forget(PENDING_EMAIL_KEY);
  history.replaceState(null, '', pageUrl());
}

export const signOut = () => firebaseSignOut(auth);

/** Token de la sesión actual: lo comprueba la app de quien recibe la llamada. */
export const idToken = () => auth.currentUser.getIdToken();

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
