// Países para el teléfono de registro, con su prefijo internacional.
// Los nombres los traduce el navegador (Intl.DisplayNames) al idioma de la página.

const DIAL_CODES = {
  AR: '54', AU: '61', BO: '591', BR: '55', CA: '1', CL: '56', CN: '86', CO: '57', CR: '506', CU: '53',
  DE: '49', DO: '1', EC: '593', ES: '34', FR: '33', GB: '44', GT: '502', HK: '852', HN: '504', IN: '91',
  IT: '39', JP: '81', KR: '82', MX: '52', NI: '505', PA: '507', PE: '51', PH: '63', PR: '1', PT: '351',
  PY: '595', SV: '503', TW: '886', US: '1', UY: '598', VE: '58',
};

/** Países ordenados por su nombre en [language]: { iso, dial, name }. */
export function countries(language) {
  const names = new Intl.DisplayNames([language], { type: 'region' });
  return Object.entries(DIAL_CODES)
    .map(([iso, dial]) => ({ iso, dial, name: names.of(iso) }))
    .sort((a, b) => a.name.localeCompare(b.name, language));
}

/** País del navegador ("es-CO" → "CO"), o Estados Unidos si no se sabe. */
export function defaultCountry() {
  const region = (navigator.language || '').split('-')[1]?.toUpperCase();
  return region && DIAL_CODES[region] ? region : 'US';
}

/** Une el prefijo del país con el número local; si ya empieza por "+", se usa tal cual. */
export function internationalNumber(iso, localNumber) {
  const typed = localNumber.trim();
  return typed.startsWith('+') ? typed : `+${DIAL_CODES[iso]}${typed.replace(/^0+/, '')}`;
}
