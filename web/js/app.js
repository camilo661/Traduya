// Une la pantalla con el inicio de sesión, el directorio y la llamada.
import { AUTO_CALL, LINK_TARGET } from './config.js';
import { applyTranslations, currentLanguage, setLanguage, t } from './i18n.js';
import {
  completeEmailLinkSignIn, idToken, sendEmailLink, setAuthLanguage, signInWithGoogle, signOut, watchUser,
} from './auth.js';
import { CallError, resolvePeerId } from './directory.js';
import { hangUp, setMuted, startCall } from './call.js';

const $ = id => document.getElementById(id);

// Los mensajes se guardan como clave + valores para volver a traducirlos si cambia el idioma
const view = {
  status: LINK_TARGET ? { key: 'callingTo', values: { target: LINK_TARGET } } : { key: 'ready', values: {} },
  loginMessage: null,
  user: null,
};
let inCall = false;
let muted = false;
let timer = null;

// region Idioma

function render() {
  applyTranslations();
  setAuthLanguage(currentLanguage());
  document.querySelectorAll('[data-lang]').forEach(button => {
    button.classList.toggle('active', button.dataset.lang === currentLanguage());
  });
  $('status').textContent = t(view.status.key, view.status.values);
  $('loginMessage').textContent = view.loginMessage ? t(view.loginMessage.key, view.loginMessage.values) : '';
  if (view.user) $('sessionName').textContent = t('signedInAs', { name: view.user.displayName || view.user.email });
}

document.querySelectorAll('[data-lang]').forEach(button => {
  button.onclick = () => { setLanguage(button.dataset.lang); render(); };
});

// endregion

// region Sesión

function setLoginMessage(key, values = {}) {
  view.loginMessage = key ? { key, values } : null;
  render();
}

$('googleBtn').onclick = () => {
  setLoginMessage(null);
  signInWithGoogle().catch(() => setLoginMessage('signInError'));
};

$('emailForm').onsubmit = async event => {
  event.preventDefault();
  const email = $('emailInput').value.trim();
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setLoginMessage('invalidEmail'); return; }
  try {
    await sendEmailLink(email);
    setLoginMessage('linkSent', { email });
  } catch (e) {
    setLoginMessage('signInError');
  }
};

$('signOutBtn').onclick = () => { if (!inCall) signOut(); };

watchUser(user => {
  view.user = user;
  $('loginCard').classList.toggle('hidden', Boolean(user));
  $('callCard').classList.toggle('hidden', !user);
  render();
  if (user && AUTO_CALL && !inCall) placeCall();
});

completeEmailLinkSignIn(() => window.prompt(t('askEmail'))).catch(() => setLoginMessage('signInError'));

// endregion

// region Llamada

function setStatus(key, values = {}) {
  view.status = { key, values };
  render();
}

function setInCall(active) {
  inCall = active;
  $('callBox').classList.toggle('hidden', active);
  $('muteBox').classList.toggle('hidden', !active);
  $('hangBox').classList.toggle('hidden', !active);
  $('targetInput').classList.toggle('hidden', active || Boolean(LINK_TARGET));
  $('signOutBtn').disabled = active;
  if (!active) {
    muted = false;
    $('muteBtn').classList.remove('on');
    clearInterval(timer);
  }
}

function startTimer() {
  const start = Date.now();
  clearInterval(timer);
  timer = setInterval(() => {
    const seconds = Math.floor((Date.now() - start) / 1000);
    $('timer').textContent = `${String(Math.floor(seconds / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  }, 500);
}

async function placeCall() {
  const target = LINK_TARGET || $('targetInput').value.trim();
  if (!target) { setStatus('enterTarget'); return; }
  setInCall(true);
  setStatus('connecting');
  $('timer').textContent = '';
  try {
    const token = await idToken();
    const peerId = await resolvePeerId(target, token);
    await startCall(peerId, token, {
      onStatus: setStatus,
      onConnected: () => { setStatus('connected'); startTimer(); },
      onEnded: key => { setInCall(false); setStatus(key); },
    });
  } catch (error) {
    setInCall(false);
    setStatus(error instanceof CallError ? error.messageKey : 'connectionError');
  }
}

$('callBtn').onclick = placeCall;
$('hangBtn').onclick = () => { hangUp(); setInCall(false); setStatus('ended'); };
$('muteBtn').onclick = () => {
  muted = !muted;
  $('muteBtn').classList.toggle('on', muted);
  setMuted(muted);
};

// endregion

setInCall(false);
render();
