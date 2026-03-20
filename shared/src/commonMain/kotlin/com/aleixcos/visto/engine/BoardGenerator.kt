package com.aleixcos.visto.engine

import com.aleixcos.visto.domain.BoardItem
import com.aleixcos.visto.domain.GameBoard
import kotlin.random.Random

object BoardGenerator {

    val EMOJI_POOL = listOf(
        "🐶","🐱","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯",
        "🦁","🐮","🐸","🐵","🐔","🐧","🐦","🦆","🦅","🦉",
        "🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞",
        "🐜","🦗","🦖","🦕","🐢","🦎","🐍","🦒","🦘","🦬",
        "🐘","🦏","🦛","🐪","🐫","🦙","🐃","🐂","🐄","🐎",
        "🐖","🐏","🐑","🐐","🦌","🐕","🐩","🦮","🐈","🐓",
        "🦃","🦤","🦚","🦜","🦩","🦢","🐇","🦔","🐿","🦦",
        "🦥","🦨","🦡","🐉","🌵","🎄","🌴","🍀","🌺","🌸",
        "🌼","🌻","🌞","⭐","🌈","⚡","🔥","❄️","🌊","🍎",
        "🍊","🍋","🍇","🍓","🍒","🍑","🥝","🍕","🍔","🌮"
    )

    // Genera tablero fijo de cols x rows, todos visibles en pantalla
    fun generate(seed: Long, cols: Int = 5, rows: Int = 8): GameBoard {
        val random = Random(seed)
        val itemCount = cols * rows  // 40 exactos
        val selected = EMOJI_POOL.shuffled(random).take(itemCount)

        val cellW = 1f / cols
        val cellH = 1f / rows

        val items = selected.mapIndexed { index, emoji ->
            val col = index % cols
            val row = index / cols
            BoardItem(
                id = index,
                imageKey = emoji,
                x = (col + 0.5f) * cellW,
                y = (row + 0.5f) * cellH
            )
        }
        return GameBoard(items = items)
    }

    // Devuelve un emoji aleatorio que no esté ya en el tablero
    fun randomEmoji(seed: Long, excludeKeys: Set<String>): String {
        val available = EMOJI_POOL.filter { it !in excludeKeys }
        return if (available.isEmpty()) EMOJI_POOL.random(Random(seed))
        else available.random(Random(seed))
    }
}