// Códigos de verificación: se generan al azar y solo se guarda su huella (hash).
import { createHash, randomInt, timingSafeEqual } from 'node:crypto';

export function newCode() {
  return String(randomInt(0, 1_000_000)).padStart(6, '0');
}

/** La huella depende del usuario: el mismo código no sirve para otra cuenta. */
export function hashCode(uid, code) {
  return createHash('sha256').update(`${uid}:${code}`).digest('hex');
}

export function matches(uid, code, expectedHash) {
  const actual = Buffer.from(hashCode(uid, code), 'hex');
  const expected = Buffer.from(expectedHash, 'hex');
  return actual.length === expected.length && timingSafeEqual(actual, expected);
}
