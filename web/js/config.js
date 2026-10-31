// Configuración de la página para llamar.
// La clave web de Firebase es pública por diseño: la seguridad está en firebase/firestore.rules.

const params = new URLSearchParams(location.search);
const isLocal = ['localhost', '127.0.0.1'].includes(location.hostname);

/** Solo en local: ?emulator=1 usa los emuladores de Firebase (pruebas automáticas). */
export const USE_EMULATOR = isLocal && params.get('emulator') === '1';

/** Solo en local: ?autocall=1 llama en cuanto hay sesión (pruebas automáticas). */
export const AUTO_CALL = isLocal && params.get('autocall') === '1';

export const FIREBASE_CONFIG = {
  apiKey: 'AIzaSyCbl2-QPKzk3211sC1X-k1-Axj9r2Xp2bA',
  authDomain: 'traduya-66592.firebaseapp.com',
  projectId: USE_EMULATOR ? 'demo-traduya' : 'traduya-66592',
};

export const AUTH_EMULATOR_URL = 'http://127.0.0.1:9099';

export const FIRESTORE_DOCUMENTS_URL = (USE_EMULATOR ? 'http://127.0.0.1:8080' : 'https://firestore.googleapis.com')
  + `/v1/projects/${FIREBASE_CONFIG.projectId}/databases/(default)/documents`;

/** A quién se llama, si viene en el enlace: ?n=+57… (número) o ?to=abc123 (código). */
export const LINK_TARGET = (params.get('n') || params.get('to') || '').trim();

/** Servidores para que la llamada atraviese routers y redes móviles. */
export const ICE_SERVERS = [
  { urls: 'stun:stun.l.google.com:19302' },
  { urls: 'stun:stun1.l.google.com:19302' },
  { urls: 'turn:openrelay.metered.ca:80', username: 'openrelayproject', credential: 'openrelayproject' },
  { urls: 'turn:openrelay.metered.ca:443', username: 'openrelayproject', credential: 'openrelayproject' },
  { urls: 'turn:openrelay.metered.ca:443?transport=tcp', username: 'openrelayproject', credential: 'openrelayproject' },
];
