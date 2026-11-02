// Une la pantalla con la cuenta, el registro, el directorio y la llamada.
// Pantallas: bienvenida → cuenta de Google → registro (si falta) → teclado para llamar (solo por internet).
import { AUTO_CALL, LINK_CODE, LINK_NUMBER, USE_EMULATOR } from './config.js';
import { applyTranslations, currentLanguage, setLanguage, t } from './i18n.js';
import { idToken, isRegistered, setAuthLanguage, signInWithGoogle, signOut, watchUser } from './auth.js';
import { RegistrationError, confirm, requestCode } from './registration.js';
import { countries, defaultCountry, dialCodeOf, internationalNumber, splitInternational } from './countries.js';
import { CallError, fetchFullName, resolvePeerId } from './directory.js';
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
  codeSent: null,
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
  $('codeSentText').textContent = text(view.codeSent);
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

watchUser(async user => {
  view.user = user;
  if (!user) {
    view.fullName = null;
    show(view.screen === 'googleCard' ? 'googleCard' : 'welcomeCard');
    return;
  }
  if (await isRegistered()) {
    await enterCallScreen();
    return;
  }
  // Tiene cuenta de Google pero no terminó el registro
  view.registerIntro = view.googleIntent === 'signIn' ? message('notRegisteredYet', { email: user.email }) : null;
  $('fullNameInput').value ||= user.displayName ?? '';
  showProfileStep();
  show('registerCard');
});

async function enterCallScreen() {
  view.fullName = await fetchFullName(view.user.uid, await idToken()).catch(() => null);
  show('callCard');
  if (AUTO_CALL && !inCall) placeCall();
}

// endregion

// region Registro con código

function showProfileStep() {
  $('profileForm').classList.remove('hidden');
  $('codeForm').classList.add('hidden');
}

function setRegisterMessage(key, values) {
  view.registerMessage = message(key, values);
  render();
}

async function sendCode() {
  const phone = internationalNumber($('countrySelect').value, $('phoneInput').value);
  setRegisterMessage(null);
  $('sendCodeBtn').disabled = $('resendBtn').disabled = true;
  try {
    const sent = await requestCode({ fullName: $('fullNameInput').value, phone, lang: currentLanguage() });
    // Solo en el emulador: el código se expone para las pruebas automáticas
    if (USE_EMULATOR) window.traduyaDebugCode = sent.debugCode;
    view.codeSent = message('codeSent', { email: sent.email });
    $('profileForm').classList.add('hidden');
    $('codeForm').classList.remove('hidden');
    $('codeInput').value = '';
    $('codeInput').focus();
    render();
  } catch (error) {
    setRegisterMessage(error instanceof RegistrationError ? error.messageKey : 'connectionError');
  } finally {
    $('sendCodeBtn').disabled = $('resendBtn').disabled = false;
  }
}

$('profileForm').onsubmit = event => { event.preventDefault(); sendCode(); };
$('resendBtn').onclick = sendCode;
$('changeDataBtn').onclick = () => { setRegisterMessage(null); showProfileStep(); };

$('codeForm').onsubmit = async event => {
  event.preventDefault();
  setRegisterMessage(null);
  $('confirmBtn').disabled = true;
  try {
    await confirm($('codeInput').value);
    await isRegistered(true); // pide un token nuevo con la marca de registrado
    view.status = message('registered');
    await enterCallScreen();
  } catch (error) {
    setRegisterMessage(error instanceof RegistrationError ? error.messageKey : 'connectionError');
  } finally {
    $('confirmBtn').disabled = false;
  }
};

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
