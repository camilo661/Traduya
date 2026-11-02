// Validación del teléfono que escribe quien se registra.

/** "+57 300 123-4567" o "0057…" → "+573001234567"; null si no es un número internacional válido (E.164). */
export function normalizePhone(raw) {
  const compact = String(raw ?? '').replace(/[\s().-]/g, '');
  const international = compact.startsWith('00') ? '+' + compact.slice(2) : compact;
  return /^\+[1-9][0-9]{7,14}$/.test(international) ? international : null;
}

/** Nombre completo: al menos nombre y apellido, sin espacios de más. */
export function normalizeFullName(raw) {
  const name = String(raw ?? '').trim().replace(/\s+/g, ' ');
  return name.length >= 3 && name.length <= 80 && name.includes(' ') ? name : null;
}
