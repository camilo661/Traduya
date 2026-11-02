// Une la pantalla con la cuenta, el registro y el teléfono (phone.js).
// Pantallas: bienvenida → cuenta de Google → registro (si falta) → teclado e historial,
// y además llamada entrante y llamada en curso con la traducción en vivo.
import { AUTO_CALL, LINK_CODE, LINK_NUMBER } from './config.js';
import { applyTranslations, currentLanguage, setLanguage, t } from './i18n.js';
import {
  cameFromEmailLink, completeConfirmationLink, currentUser, idToken, isConfirmationLink, sendConfirmationLink,
  setAuthLanguage, signInWithGoogle, signOut, watchUser,
} from './auth.js';
import { RegistrationError, finishRegistration, isRegistered, profileOf, saveDraft, validateProfile } from './registration.js';
import { countries, defaultCountry, dialCodeOf, internationalNumber, splitInternational } from './countries.js';
import { CallError, lookupNumber, normalizeNumber } from './directory.js';
import { answer, call, decline, hangUp, setMuted, startPhone, stopPhone } from './phone.js';
import { createDialpad } from './dialpad.js';
import { listCalls, listLines } from './history.js';
import { getRecording } from './recordings.js';
import * as callView from './ui/call-view.js';
import * as historyView from './ui/history-view.js';

const $ = id => document.getElementById(id);
const SCREENS = ['welcomeCard', 'googleCard', 'registerCard', 'callCard', 'incomingCard', 'inCallCard'];

// Los mensajes se guardan como clave + valores para volver a traducirlos si cambia el idioma
const message = (key, values = {}) => (key ? { key, values } : null);
const view = {
  screen: null,
  googleIntent: 'register', // 'register' o 'signIn'
  googleMessage: null,
  registerIntro: null,
  registerMessage: null,
  linkSent: null,
  status: LINK_CODE ? message('callingTo', { target: LINK_CODE }) : message('ready'),
  user: null,
  profile: null,
  presence: null,        // 'online' | 'offline' | 'taken'
  target: null,          // estado del número marcado: 'on' | 'off' | 'unknown'
  hint: null,
  peer: null,            // con quién es la llamada en curso: { name, phone }
  languages: null,       // { from, to } de la llamada en curso
};
let inCall = false;
let muted = false;

// region Pantalla e idioma

function show(screen) {
  view.screen = screen;
  SCREENS.forEach(id => $(id).classList.toggle('hidden', id !== screen));
  document.body.classList.toggle('compact', ['callCard', 'incomingCard', 'inCallCard'].includes(screen));
  document.body.classList.toggle('calling', ['incomingCard', 'inCallCard'].includes(screen));
  render();
}

const text = msg => (msg ? t(msg.key, msg.values) : '');

function render() {
  applyTranslations();
  setAuthLanguage(currentLanguage());
  document.querySelectorAll('[data-lang]').forEach(button => {
    button.classList.toggle('active', button.dataset.lang === currentLanguage());
  });
  const registering = view.googleIntent === 'register';
  $('googleTitle').textContent = t(registering ? 'registerTitle' : 'signInTitle');
  $('googleHint').textContent = t(registering ? 'registerGoogleHint' : 'signInHint');
  $('googleMessage').textContent = text(view.googleMessage);
  $('registerIntro').textContent = text(view.registerIntro);
  $('registerMessage').textContent = text(view.registerMessage);
  $('linkSentText').textContent = text(view.linkSent);
  $('status').textContent = text(view.status);
  $('sessionName').textContent = view.user ? t('signedInAs', { name: view.profile?.fullName || view.user.email }) : '';
  renderPresence();
  renderTarget();
  callView.setHint(text(view.hint));
  callView.setLanguages(view.languages
    ? t('translating', { from: t(`lang_${view.languages.from}`), to: t(`lang_${view.languages.to}`) }) : '');
  renderCountries($('countrySelect'));
  renderCountries($('dialCountry'));
  renderDialPrefix();
}

function renderCountries(select) {
  const selected = select.value || defaultCountry();
  select.innerHTML = '';
  countries(currentLanguage()).forEach(({ iso, dial, name }) => {
    select.add(new Option(`${name} (+${dial})`, iso, false, iso === selected));
  });
}

document.querySelectorAll('[data-lang]').forEach(button => {
  button.onclick = () => { setLanguage(button.dataset.lang); render(); };
});

// endregion

// region Cuenta de Google

function chooseGoogle(intent) {
  view.googleIntent = intent;
  view.googleMessage = null;
  show('googleCard');
}

$('registerBtn').onclick = () => chooseGoogle('register');
$('signInBtn').onclick = () => chooseGoogle('signIn');
$('backBtn').onclick = () => show('welcomeCard');
$('googleBtn').onclick = () => {
  view.googleMessage = null;
  signInWithGoogle().catch(() => { view.googleMessage = message('signInError'); render(); });
};
$('otherAccountBtn').onclick = () => signOut();
$('signOutBtn').onclick = () => { if (!inCall) signOut(); };

/** Decide la pantalla según la sesión. Se ejecuta en orden, una vez por cada cambio. */
let handling = Promise.resolve();
const onUser = user => { handling = handling.then(() => handleUser(user)).catch(() => {}); };

async function handleUser(user) {
  view.user = user;
  if (!user) {
    stopPhone();
    view.profile = null;
    view.presence = null;
    show(view.screen === 'googleCard' ? 'googleCard' : 'welcomeCard');
    return;
  }
  const token = await idToken();
  if (await isRegistered(user.uid, token).catch(() => false)) {
    await enterCallScreen();
    return;
  }
  // Volvió con el enlace del correo: el borrador pasa a ser su ficha
  if (await cameFromEmailLink() && await finishRegistration(user, token).catch(() => false)) {
    view.status = message('registered');
    await enterCallScreen();
    return;
  }
  // Tiene cuenta de Google pero no terminó el registro
  view.registerIntro = view.googleIntent === 'signIn' ? message('notRegisteredYet', { email: user.email }) : null;
  $('fullNameInput').value ||= user.displayName ?? '';
  showProfileStep();
  show('registerCard');
}

watchUser(onUser);

/** Con el registro completo: abre el teclado y empieza a recibir llamadas en su número. */
async function enterCallScreen() {
  view.profile = await profileOf(view.user.uid, await idToken()).catch(() => null);
  show('callCard');
  if (view.profile) {
    await startPhone({
      uid: view.user.uid, profile: view.profile, getToken: idToken, lang: currentLanguage, ui: phoneUi,
    });
  }
  if (AUTO_CALL && !inCall) placeCall();
}

// Si la página se abrió desde el enlace «Confirmar registro» del correo, se termina de entrar
if (isConfirmationLink()) {
  view.registerIntro = message('confirming');
  completeConfirmationLink(() => window.prompt(t('askEmail')))
    // Si ya había sesión de Google con el mismo correo, Firebase no avisa del cambio: se revisa aquí
    .then(done => { if (done) onUser(currentUser()); })
    .catch(error => {
      view.registerMessage = message(error.code === 'auth/too-many-requests' ? 'tooManyRequests' : 'linkInvalid');
      render();
    });
}

// endregion

// region Registro con enlace de confirmación

function showProfileStep() {
  $('profileForm').classList.remove('hidden');
  $('linkSentBox').classList.add('hidden');
}

function setRegisterMessage(key, values) {
  view.registerMessage = message(key, values);
  render();
}

/**
 * Con la sesión de Google: guarda el borrador y envía el enlace al Gmail.
 * Con la sesión del enlace (si se abrió en otro navegador sin borrador): crea la ficha directamente.
 */
async function submitProfile() {
  setRegisterMessage(null);
  $('sendLinkBtn').disabled = $('resendBtn').disabled = true;
  try {
    const profile = validateProfile({
      fullName: $('fullNameInput').value,
      phone: internationalNumber($('countrySelect').value, $('phoneInput').value),
    });
    const token = await idToken();
    if (await cameFromEmailLink()) {
      await finishRegistration(view.user, token, profile);
      view.status = message('registered');
      await enterCallScreen();
      return;
    }
    await saveDraft(view.user.uid, profile, token);
    await sendConfirmationLink(view.user.email);
    view.linkSent = message('linkSent', { email: view.user.email });
    $('profileForm').classList.add('hidden');
    $('linkSentBox').classList.remove('hidden');
    render();
  } catch (error) {
    if (error instanceof RegistrationError) setRegisterMessage(error.messageKey);
    else setRegisterMessage(error.code === 'auth/too-many-requests' ? 'tooManyRequests' : 'connectionError');
  } finally {
    $('sendLinkBtn').disabled = $('resendBtn').disabled = false;
  }
}

$('profileForm').onsubmit = event => { event.preventDefault(); submitProfile(); };
$('resendBtn').onclick = submitProfile;
$('changeDataBtn').onclick = () => { setRegisterMessage(null); showProfileStep(); };

// endregion

// region Teclado para marcar

const dialpad = createDialpad({
  keypad: $('keypad'),
  display: $('dialNumber'),
  deleteButton: $('dialDelete'),
  onChange: () => { renderDialPrefix(); checkTarget(); },
  onEnter: () => { if (!inCall) placeCall(); },
});
// El número del enlace se reparte entre el selector de país y el teclado
if (LINK_NUMBER) {
  const parts = splitInternational(LINK_NUMBER);
  if (parts) {
    renderCountries($('dialCountry'));
    $('dialCountry').value = parts.iso;
    dialpad.set(parts.local);
  } else {
    dialpad.set(LINK_NUMBER.replace(/[^0-9+]/g, ''));
  }
}

/** El prefijo del país se muestra delante, salvo que el número ya empiece por "+". */
function renderDialPrefix() {
  const ownPrefix = dialpad.value.startsWith('+');
  $('dialPrefix').textContent = ownPrefix ? '' : `+${dialCodeOf($('dialCountry').value)}`;
  $('dialCountry').disabled = ownPrefix;
}

$('dialCountry').onchange = () => { renderDialPrefix(); checkTarget(); };

/** Luz del número marcado: verde si está activo, roja si no, gris si no está en Traduya. */
let targetCheck = 0;
function checkTarget() {
  const number = dialpad.value.replace(/\D/g, '').length >= 7 ? normalizeNumber(dialTarget()) : null;
  const turn = ++targetCheck;
  view.target = null;
  renderTarget();
  if (!number || !view.user) return;
  setTimeout(async () => {
    if (turn !== targetCheck) return;
    const entry = await lookupNumber(number, await idToken()).catch(() => undefined);
    if (turn !== targetCheck || entry === undefined) return;
    view.target = entry ? (entry.active ? 'on' : 'off') : 'unknown';
    renderTarget();
  }, 450);
}

function renderTarget() {
  $('targetStatus').classList.toggle('hidden', !view.target);
  if (!view.target) return;
  $('targetStatus').querySelector('.dot').className = `dot ${view.target === 'unknown' ? '' : view.target}`;
  $('targetStatusText').textContent = t({ on: 'targetActive', off: 'targetInactive', unknown: 'targetUnknown' }[view.target]);
}

function renderPresence() {
  const pill = $('presence');
  pill.classList.toggle('hidden', !view.presence);
  if (!view.presence) return;
  pill.querySelector('.dot').className = `dot ${view.presence === 'online' ? 'on' : 'off'}`;
  $('presenceText').textContent = view.presence === 'online'
    ? t('online', { phone: view.profile?.phone ?? '' })
    : t(view.presence === 'taken' ? 'numberTaken' : 'offline');
}

/** Lo que se va a llamar: el número marcado (con su país) o el código de un enlace antiguo. */
function dialTarget() {
  if (dialpad.value.replace(/\D/g, '')) return internationalNumber($('dialCountry').value, dialpad.value);
  return LINK_CODE;
}

// endregion

// region Pestañas Teclado / Historial

document.querySelectorAll('[data-tab]').forEach(button => {
  button.onclick = () => {
    document.querySelectorAll('[data-tab]').forEach(tab => {
      tab.classList.toggle('active', tab === button);
      $(tab.dataset.tab).classList.toggle('hidden', tab !== button);
    });
    if (button.dataset.tab === 'historyTab') loadHistory();
  };
});

async function loadHistory() {
  if (!view.user) return;
  const calls = await listCalls(view.user.uid, await idToken()).catch(() => []);
  historyView.renderList(calls, currentLanguage(), t, openCall);
}

async function openCall(entry) {
  const token = await idToken();
  const [lines, recording] = await Promise.all([
    listLines(view.user.uid, token, entry.id).catch(() => []),
    getRecording(entry.id),
  ]);
  historyView.renderDetail(entry, lines, recording, currentLanguage(), t);
}

$('historyBackBtn').onclick = () => historyView.showList();

// endregion

// region Llamadas (entrantes y salientes)

function setStatus(key, values = {}) {
  view.status = message(key, values);
  render();
}

/** Lo que el teléfono (phone.js) le pide a la pantalla. */
const phoneUi = {
  onPresence: state => { view.presence = state; render(); },
  onIncoming: caller => {
    $('incomingAvatar').textContent = (caller.name || '?').charAt(0).toUpperCase();
    $('incomingName').textContent = caller.name;
    $('incomingPhone').textContent = caller.name !== caller.phone ? caller.phone : '';
    navigator.vibrate?.([400, 200, 400]);
    show('incomingCard');
  },
  onIncomingCancelled: () => show('callCard'),
  onCallStarted: peer => {
    inCall = true;
    muted = false;
    $('muteBtn').classList.remove('on');
    view.languages = null;
    view.hint = message('connecting');
    view.peer = peer;
    callView.showPeer(peer);
    show('inCallCard');
  },
  onStatus: key => { view.hint = message(key); render(); },
  onConnected: () => {
    view.hint = message('listening');
    callView.startTimer(view.peer?.phone);
    render();
  },
  onLanguages: (from, to) => { view.languages = { from, to }; render(); },
  onLive: translated => callView.setLive(translated),
  onLine: line => callView.addLine(line),
  onEnded: key => {
    inCall = false;
    callView.stopTimer();
    view.languages = null;
    view.hint = null;
    setStatus(key);
    show('callCard');
  },
  onHistoryChanged: () => { if (!$('historyTab').classList.contains('hidden')) loadHistory(); },
};

async function placeCall() {
  const target = dialTarget();
  if (!target) { setStatus('enterTarget'); return; }
  setStatus('connecting');
  try {
    await call(target);
  } catch (error) {
    setStatus(error instanceof CallError ? error.messageKey : 'connectionError');
  }
}

$('callBtn').onclick = placeCall;
$('answerBtn').onclick = () => answer();
$('declineBtn').onclick = () => { decline(); show('callCard'); };
$('hangBtn').onclick = () => hangUp();
$('muteBtn').onclick = () => {
  muted = !muted;
  $('muteBtn').classList.toggle('on', muted);
  setMuted(muted);
};

// endregion

render();
