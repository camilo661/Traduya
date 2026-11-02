// Une la pantalla con la cuenta, el registro, el directorio y la llamada.
// Pantallas: bienvenida → cuenta de Google → registro (si falta) → teclado para llamar (solo por internet).
import { AUTO_CALL, LINK_CODE, LINK_NUMBER } from './config.js';
import { applyTranslations, currentLanguage, setLanguage, t } from './i18n.js';
import {
  cameFromEmailLink, completeConfirmationLink, currentUser, idToken, isConfirmationLink, sendConfirmationLink,
  setAuthLanguage, signInWithGoogle, signOut, watchUser,
} from './auth.js';
import { RegistrationError, finishRegistration, fullNameOf, isRegistered, saveDraft, validateProfile } from './registration.js';
import { countries, defaultCountry, dialCodeOf, internationalNumber, splitInternational } from './countries.js';
import { CallError, resolvePeerId } from './directory.js';
import { hangUp, setMuted, startCall } from './call.js';
import { createDialpad } from './dialpad.js';

const $ = id => document.getElementById(id);
const SCREENS = ['welcomeCard', 'googleCard', 'registerCard', 'callCard'];

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
  fullName: null,
};
let inCall = false;
let muted = false;
let timer = null;

// region Pantalla e idioma

function show(screen) {
  view.screen = screen;
  SCREENS.forEach(id => $(id).classList.toggle('hidden', id !== screen));
  document.body.classList.toggle('compact', screen === 'callCard');
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
  $('sessionName').textContent = view.user ? t('signedInAs', { name: view.fullName || view.user.email }) : '';
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
    view.fullName = null;
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

async function enterCallScreen() {
  view.fullName = await fullNameOf(view.user.uid, await idToken()).catch(() => null);
  show('callCard');
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
  onChange: () => renderDialPrefix(),
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

$('dialCountry').onchange = renderDialPrefix;

/** Lo que se va a llamar: el número marcado (con su país) o el código de un enlace antiguo. */
function dialTarget() {
  if (dialpad.value.replace(/\D/g, '')) return internationalNumber($('dialCountry').value, dialpad.value);
  return LINK_CODE;
}

// endregion

// region Llamada

function setStatus(key, values = {}) {
  view.status = message(key, values);
  render();
}

function setInCall(active) {
  inCall = active;
  $('callBox').classList.toggle('hidden', active);
  $('muteBox').classList.toggle('hidden', !active);
  $('hangBox').classList.toggle('hidden', !active);
  $('dialer').classList.toggle('hidden', active);
  $('callTarget').classList.toggle('hidden', !active);
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
  const target = dialTarget();
  if (!target) { setStatus('enterTarget'); return; }
  $('callTarget').textContent = target;
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
