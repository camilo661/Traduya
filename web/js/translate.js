// Traducción de los subtítulos que llegan: el traductor del navegador si existe (Chrome),
// y si no, MyMemory (gratis, con un límite diario de texto).

const cache = new Map();
const MYMEMORY_LANG = { es: 'es', en: 'en', zh: 'zh-CN' };

async function browserTranslator(from, to) {
  if (!('Translator' in self)) return null;
  try {
    if ((await self.Translator.availability({ sourceLanguage: from, targetLanguage: to })) === 'unavailable') return null;
    return await self.Translator.create({ sourceLanguage: from, targetLanguage: to });
  } catch (e) {
    return null;
  }
}

const translators = new Map();

/** [text] de [from] a [to] ('es', 'en' o 'zh'). Si falla, devuelve el texto original. */
export async function translate(text, from, to) {
  if (!text || from === to) return text;
  const key = `${from}|${to}|${text}`;
  if (cache.has(key)) return cache.get(key);

  let result = null;
  const pair = `${from}|${to}`;
  if (!translators.has(pair)) translators.set(pair, browserTranslator(from, to));
  const translator = await translators.get(pair);
  if (translator) {
    try { result = await translator.translate(text); } catch (e) { result = null; }
  }
  if (!result) {
    try {
      const query = new URLSearchParams({ q: text, langpair: `${MYMEMORY_LANG[from]}|${MYMEMORY_LANG[to]}` });
      const response = await fetch(`https://api.mymemory.translated.net/get?${query}`);
      const json = await response.json();
      if (json.responseStatus === 200 && !json.quotaFinished) result = json.responseData.translatedText;
    } catch (e) { /* sin conexión */ }
  }
  result = result || text;
  cache.set(key, result);
  return result;
}
