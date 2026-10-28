package com.traduya.core.translation.vosk

import com.traduya.core.domain.usecase.Hypothesis
import com.traduya.core.model.Language
import org.json.JSONObject

/** Lee el JSON que devuelve Vosk. */
internal object VoskResults {

    /** {"partial": "hello how are"} */
    fun partial(json: String, language: Language): String =
        clean(JSONObject(json).optString("partial"), language)

    /** {"text": "...", "result": [{"conf": 0.98, "word": "hello"}, ...]} */
    fun final(json: String, language: Language): Hypothesis {
        val obj = JSONObject(json)
        val words = obj.optJSONArray("result")
        var confidence = 0f
        val count = words?.length() ?: 0
        for (i in 0 until count) confidence += words!!.getJSONObject(i).optDouble("conf", 0.0).toFloat()
        return Hypothesis(
            language = language,
            text = clean(obj.optString("text"), language),
            confidence = if (count == 0) 0f else confidence / count,
            wordCount = count,
        )
    }

    /** El modelo chino separa las palabras con espacios: en chino se escriben juntas. */
    private fun clean(text: String, language: Language): String {
        val trimmed = text.trim()
        return if (language == Language.CHINESE) trimmed.replace(" ", "") else trimmed
    }
}
