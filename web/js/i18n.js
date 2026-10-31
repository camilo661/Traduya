// Textos de la página en español, inglés y chino.
// El idioma se elige según el navegador y se puede cambiar con los botones de arriba.

const STRINGS = {
  es: {
    subtitle: 'Habla normal. La persona a la que llamas lee tus palabras traducidas al español, en vivo.',
    loginTitle: 'Inicia sesión para llamar',
    loginWhy: 'Así la persona que recibe la llamada sabe quién la llama.',
    google: 'Continuar con Google',
    or: 'o',
    emailPlaceholder: 'Tu correo',
    sendLink: 'Enviarme un enlace para entrar',
    linkSent: 'Listo. Abre en este mismo teléfono el enlace que enviamos a {email}. Revisa también el correo no deseado.',
    askEmail: 'Escribe tu correo para terminar de entrar',
    invalidEmail: 'Escribe un correo válido.',
    signInError: 'No se pudo iniciar sesión. Inténtalo de nuevo.',
    signedInAs: 'Sesión: {name}',
    signOut: 'Cerrar sesión',
    howTitle: 'Cómo llamar',
    step1: 'Escribe el número de la persona con el código del país (por ejemplo +57 300 123 4567) o su código.',
    step2: 'Pulsa Llamar y permite el micrófono.',
    step3: 'Habla normal: la otra persona lee la traducción en español.',
    targetPlaceholder: 'Número o código',
    callingTo: 'Vas a llamar a {target}',
    ready: 'Listo para llamar',
    call: 'Llamar',
    mute: 'Silenciar',
    hangUp: 'Colgar',
    connecting: 'Conectando…',
    ringing: 'Llamando…',
    connected: 'Conectado',
    ended: 'Llamada terminada',
    lost: 'Se perdió la conexión',
    unavailable: 'No disponible. Pídele que abra «Llamada traducida» en Traduya.',
    connectionError: 'Error de conexión',
    micBlocked: 'Micrófono bloqueado. Permítelo en los ajustes del navegador.',
    enterTarget: 'Escribe el número o el código.',
    needCountry: 'Incluye el código del país, por ejemplo +57…',
    notRegistered: 'Ese número no está registrado en Traduya.',
    notice: 'Esta llamada se traduce y se graba con Traduya.',
    browserNote: 'Funciona en Safari y Chrome.',
  },
  en: {
    subtitle: 'Talk normally. The person you call reads your words translated into Spanish, live.',
    loginTitle: 'Sign in to call',
    loginWhy: 'So the person you call knows who is calling.',
    google: 'Continue with Google',
    or: 'or',
    emailPlaceholder: 'Your email',
    sendLink: 'Email me a sign-in link',
    linkSent: 'Done. Open the link we sent to {email} on this same phone. Check your spam folder too.',
    askEmail: 'Enter your email to finish signing in',
    invalidEmail: 'Enter a valid email.',
    signInError: 'Could not sign in. Please try again.',
    signedInAs: 'Signed in: {name}',
    signOut: 'Sign out',
    howTitle: 'How to call',
    step1: 'Enter the person\'s number with the country code (e.g. +57 300 123 4567) or their code.',
    step2: 'Tap Call and allow the microphone.',
    step3: 'Talk normally: they read the Spanish translation.',
    targetPlaceholder: 'Number or code',
    callingTo: 'You are calling {target}',
    ready: 'Ready to call',
    call: 'Call',
    mute: 'Mute',
    hangUp: 'Hang up',
    connecting: 'Connecting…',
    ringing: 'Ringing…',
    connected: 'Connected',
    ended: 'Call ended',
    lost: 'Connection lost',
    unavailable: 'Not available. Ask them to open "Llamada traducida" in Traduya.',
    connectionError: 'Connection error',
    micBlocked: 'Microphone blocked. Allow it in your browser settings.',
    enterTarget: 'Enter the number or code.',
    needCountry: 'Include the country code, e.g. +57…',
    notRegistered: 'This number is not registered on Traduya.',
    notice: 'This call is translated and recorded by Traduya.',
    browserNote: 'Works in Safari and Chrome.',
  },
  zh: {
    subtitle: '正常说话。对方会实时看到你的话被翻译成西班牙语。',
    loginTitle: '登录后即可拨打',
    loginWhy: '这样对方就知道是谁打来的。',
    google: '使用 Google 继续',
    or: '或',
    emailPlaceholder: '你的邮箱',
    sendLink: '给我发送登录链接',
    linkSent: '已发送。请在这部手机上打开发送到 {email} 的链接，也请检查垃圾邮件。',
    askEmail: '请输入你的邮箱以完成登录',
    invalidEmail: '请输入有效的邮箱。',
    signInError: '登录失败，请重试。',
    signedInAs: '已登录：{name}',
    signOut: '退出登录',
    howTitle: '如何拨打',
    step1: '输入对方的号码（含国家代码，例如 +57 300 123 4567）或代码。',
    step2: '点击拨打并允许使用麦克风。',
    step3: '正常说话，对方会看到西班牙语翻译。',
    targetPlaceholder: '号码或代码',
    callingTo: '你将拨打 {target}',
    ready: '准备拨打',
    call: '拨打',
    mute: '静音',
    hangUp: '挂断',
    connecting: '连接中…',
    ringing: '呼叫中…',
    connected: '已接通',
    ended: '通话结束',
    lost: '连接中断',
    unavailable: '对方未在线，请让对方打开 Traduya 的「Llamada traducida」。',
    connectionError: '连接错误',
    micBlocked: '麦克风被阻止，请在浏览器设置中允许。',
    enterTarget: '请输入号码或代码。',
    needCountry: '请包含国家代码，例如 +57…',
    notRegistered: '该号码未在 Traduya 注册。',
    notice: '本通话将由 Traduya 翻译并录音。',
    browserNote: '支持 Safari 和 Chrome。',
  },
};

export const LANGUAGES = ['es', 'en', 'zh'];
const STORAGE_KEY = 'traduya.lang';

function initialLanguage() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (LANGUAGES.includes(saved)) return saved;
  } catch (e) { /* almacenamiento no disponible */ }
  const browser = (navigator.language || 'en').slice(0, 2).toLowerCase();
  return LANGUAGES.includes(browser) ? browser : 'en';
}

let language = initialLanguage();

export const currentLanguage = () => language;

/** Texto traducido; {nombre} se reemplaza con los valores dados. */
export function t(key, values = {}) {
  const text = STRINGS[language][key] ?? STRINGS.en[key] ?? key;
  return text.replace(/\{(\w+)\}/g, (_, name) => values[name] ?? '');
}

/** Rellena los elementos con data-i18n (texto) y data-i18n-placeholder. */
export function applyTranslations(root = document) {
  document.documentElement.lang = language === 'zh' ? 'zh-CN' : language;
  root.querySelectorAll('[data-i18n]').forEach(el => { el.textContent = t(el.dataset.i18n); });
  root.querySelectorAll('[data-i18n-placeholder]').forEach(el => { el.placeholder = t(el.dataset.i18nPlaceholder); });
}

export function setLanguage(next) {
  if (!LANGUAGES.includes(next)) return;
  language = next;
  try { localStorage.setItem(STORAGE_KEY, next); } catch (e) { /* almacenamiento no disponible */ }
  applyTranslations();
}
