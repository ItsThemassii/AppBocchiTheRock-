package com.bocchifan.app.game

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.bocchifan.app.R

/** Número de carriles del juego. */
const val LANES = 5

/** Una nota: [timeMs] es cuándo cruza la línea; si [endMs] no es null es una nota larga. */
data class Note(val timeMs: Int, val lane: Int, val endMs: Int? = null) {
    val isHold: Boolean get() = endMs != null
}

data class Chart(val notes: List<Note>, val durationMs: Int) {
    /** Juicios totales: una por nota y otra extra por el final de cada nota larga. */
    val judgmentCount: Int get() = notes.size + notes.count { it.isHold }
}

enum class Difficulty(@StringRes val label: Int, val color: Color) {
    Easy(R.string.difficulty_easy, Color(0xFF4A78D6)),
    Normal(R.string.difficulty_normal, Color(0xFF3FB27F)),
    Hard(R.string.difficulty_hard, Color(0xFFF2A33A)),
    Expert(R.string.difficulty_expert, Color(0xFFE5578C)),
}

/** Un golpe detectado en el audio. [pitch] 0 = grave, 1 = agudo. [sustainMs]: cuánto dura el sonido. */
data class Onset(val timeMs: Int, val strength: Float, val pitch: Float, val sustainMs: Int)
