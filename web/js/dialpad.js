// Teclado para marcar, como el de un teléfono: 0-9, * y #.
// Mantener presionado el 0 escribe "+"; mantener presionado borrar lo borra todo.

const KEYS = [
  ['1', ''], ['2', 'ABC'], ['3', 'DEF'],
  ['4', 'GHI'], ['5', 'JKL'], ['6', 'MNO'],
  ['7', 'PQRS'], ['8', 'TUV'], ['9', 'WXYZ'],
  ['*', ''], ['0', '+'], ['#', ''],
];
const MAX_LENGTH = 16;
const LONG_PRESS_MS = 550;
const LONG_NUMBER = 9;

/** "3001112233" → "300 111 2233": grupos de 3 y los últimos 4 juntos, para leerlo fácil. */
function grouped(value) {
  const sign = value.startsWith('+') ? '+' : '';
  const digits = value.slice(sign.length);
  if (digits.length <= 4) return sign + digits;
  const head = digits.slice(0, -4).match(/.{1,3}/g).join(' ');
  return `${sign}${head} ${digits.slice(-4)}`;
}

/** Llama a [onShort] con un toque y a [onLong] si se mantiene presionado. */
function pressable(button, onShort, onLong) {
  let timer = null;
  let longPressed = false;
  button.addEventListener('pointerdown', () => {
    longPressed = false;
    timer = setTimeout(() => { longPressed = true; onLong(); }, LONG_PRESS_MS);
  });
  const cancel = () => clearTimeout(timer);
  button.addEventListener('pointerup', cancel);
  button.addEventListener('pointerleave', cancel);
  button.addEventListener('click', () => { if (!longPressed) onShort(); });
}

/**
 * Crea el teclado dentro de [keypad] y muestra lo marcado en [display].
 * [onEnter] se llama con la tecla Enter del teclado físico.
 */
export function createDialpad({ keypad, display, deleteButton, onChange, onEnter }) {
  let value = '';

  function set(next) {
    value = next.slice(0, MAX_LENGTH);
    display.textContent = grouped(value);
    display.classList.toggle('long', value.length > LONG_NUMBER);
    onChange?.(value);
  }

  KEYS.forEach(([digit, letters]) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'key';
    button.setAttribute('aria-label', digit);
    const main = document.createElement('span');
    main.className = 'digit';
    main.textContent = digit;
    const sub = document.createElement('span');
    sub.className = 'letters';
    sub.textContent = letters;
    button.append(main, sub);
    if (digit === '0') {
      pressable(button, () => set(value + '0'), () => set(value + '+'));
    } else {
      button.addEventListener('click', () => set(value + digit));
    }
    keypad.append(button);
  });

  pressable(deleteButton, () => set(value.slice(0, -1)), () => set(''));

  // En computadora también se puede escribir con el teclado
  document.addEventListener('keydown', event => {
    if (keypad.offsetParent === null || event.target instanceof HTMLInputElement) return;
    if (/^[0-9*#+]$/.test(event.key)) set(value + event.key);
    else if (event.key === 'Backspace') set(value.slice(0, -1));
    else if (event.key === 'Enter') onEnter?.();
  });

  return {
    get value() { return value; },
    set,
  };
}
