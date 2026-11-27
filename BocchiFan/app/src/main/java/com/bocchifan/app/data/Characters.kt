package com.bocchifan.app.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import org.json.JSONArray
import org.json.JSONObject

/** Texto en español e inglés. */
data class Localized(val es: String, val en: String) {
    fun get(english: Boolean): String = if (english && en.isNotBlank()) en else es
}

/** Lista de textos en español e inglés (párrafos, curiosidades). */
data class LocalizedList(val es: List<String>, val en: List<String>) {
    fun get(english: Boolean): List<String> = if (english && en.isNotEmpty()) en else es

    companion object {
        val Empty = LocalizedList(emptyList(), emptyList())
    }
}

enum class CharacterGroup { Kessoku, Secondary }

data class Character(
    val id: String,
    val group: CharacterGroup,
    val color: Color,
    val name: String,
    val nameJp: String?,
    val nickname: Localized?,
    val role: Localized,
    val age: Localized?,
    val birthday: Localized?,
    val height: String?,
    val bloodType: String?,
    val school: Localized?,
    val band: String?,
    val voice: String?,
    val bio: LocalizedList,
    val facts: LocalizedList,
) {
    /** Inicial del apodo/id ("B" de Bocchi, "K" de Kita), como en el menú. */
    val initial: String get() = id.take(1).uppercase()
}

/** true si la app se muestra en inglés. */
@Composable
@ReadOnlyComposable
fun isEnglish(): Boolean = LocalConfiguration.current.locales[0].language == "en"

@Composable
@ReadOnlyComposable
fun Localized.text(): String = get(isEnglish())

@Composable
@ReadOnlyComposable
fun LocalizedList.texts(): List<String> = get(isEnglish())

/**
 * Lee las fichas desde `contenido/datos/personajes.json` (assets/datos/personajes.json).
 * Si el archivo falta o tiene un error, devuelve una lista vacía (y lo deja en el log).
 */
object CharacterRepository {
    private const val PATH = "datos/personajes.json"
    private const val TAG = "CharacterRepository"

    @Volatile
    private var cache: List<Character>? = null

    fun all(context: Context): List<Character> = cache ?: synchronized(this) {
        cache ?: load(context).also { cache = it }
    }

    fun byId(context: Context, id: String): Character? = all(context).firstOrNull { it.id == id }

    private fun load(context: Context): List<Character> = try {
        val json = context.assets.open(PATH).bufferedReader(Charsets.UTF_8).use { it.readText() }
        parse(json)
    } catch (e: Exception) {
        Log.w(TAG, "No se pudo leer $PATH", e)
        emptyList()
    }

    internal fun parse(json: String): List<Character> {
        val array = JSONObject(json).getJSONArray("personajes")
        return (0 until array.length()).mapNotNull { i ->
            val o = array.getJSONObject(i)
            try {
                Character(
                    id = o.getString("id"),
                    group = if (o.optString("grupo") == "kessoku") CharacterGroup.Kessoku else CharacterGroup.Secondary,
                    color = parseColor(o.optString("color")),
                    name = o.getString("nombre"),
                    nameJp = o.optStringOrNull("nombreJp"),
                    nickname = o.localized("apodo"),
                    role = o.localized("rol") ?: Localized("", ""),
                    age = o.localized("edad"),
                    birthday = o.localized("cumpleanos"),
                    height = o.optStringOrNull("estatura"),
                    bloodType = o.optStringOrNull("sangre"),
                    school = o.localized("escuela"),
                    band = o.optStringOrNull("banda"),
                    voice = o.optStringOrNull("seiyu"),
                    bio = o.localizedList("bio"),
                    facts = o.localizedList("curiosidades"),
                )
            } catch (e: Exception) {
                Log.w(TAG, "Personaje $i inválido", e)
                null
            }
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) optString(key).takeIf { it.isNotBlank() } else null

    /** Acepta "texto" (igual en ambos idiomas) o {"es": "...", "en": "..."}. */
    private fun JSONObject.localized(key: String): Localized? {
        if (!has(key) || isNull(key)) return null
        return when (val v = get(key)) {
            is JSONObject -> Localized(v.optString("es"), v.optString("en"))
            else -> v.toString().takeIf { it.isNotBlank() }?.let { Localized(it, it) }
        }
    }

    /** Acepta ["..."] o {"es": ["..."], "en": ["..."]}. */
    private fun JSONObject.localizedList(key: String): LocalizedList {
        if (!has(key) || isNull(key)) return LocalizedList.Empty
        return when (val v = get(key)) {
            is JSONArray -> v.strings().let { LocalizedList(it, it) }
            is JSONObject -> LocalizedList(
                v.optJSONArray("es")?.strings().orEmpty(),
                v.optJSONArray("en")?.strings().orEmpty(),
            )
            else -> LocalizedList.Empty
        }
    }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { optString(it) }.filter { it.isNotBlank() }

    private fun parseColor(hex: String): Color = try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color(0xFFF4A7C3)
    }
}
