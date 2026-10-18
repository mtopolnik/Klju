package org.mtopol.klju

enum class Mark { QUESTION, CROSS, CHECK, EYE, CIRCLE }

/** A cell of the sheet: [card] is the sheet-wide card index, [player] the column. */
data class Cell(val card: Int, val player: Int)

data class Sheet(
    val players: List<String>,
    val marks: Map<Cell, Mark> = emptyMap(),
) {
    fun mark(card: Int, player: Int): Mark? = marks[Cell(card, player)]

    fun withMark(card: Int, player: Int, mark: Mark?): Sheet {
        val cell = Cell(card, player)
        return copy(marks = if (mark == null) marks - cell else marks + (cell to mark))
    }

    fun withPlayerName(player: Int, name: String): Sheet =
        copy(players = players.toMutableList().also { it[player] = sanitizeName(name) })

    fun canAddPlayer(): Boolean = players.size < MAX_PLAYERS

    /** The first column belongs to the phone's owner, so only the other players can be removed. */
    fun canRemovePlayer(player: Int): Boolean = player in 1 until players.size && players.size > MIN_PLAYERS

    fun withPlayerAdded(): Sheet {
        if (!canAddPlayer()) return this
        // Named after the new column's position; when a renamed player already uses that
        // name, the first unused ordinal (from that position, then wrapping around) is used.
        val ordinals = DEFAULT_NAMES.drop(1)
        val start = players.size - 1
        val name = (ordinals.drop(start) + ordinals.take(start)).firstOrNull { it !in players }
            ?: DEFAULT_NAMES[players.size]
        return copy(players = players + name)
    }

    /** Drops the player's column; the columns to its right shift left with their marks. */
    fun withPlayerRemoved(player: Int): Sheet {
        if (!canRemovePlayer(player)) return this
        val shifted = marks.mapNotNull { (cell, mark) ->
            when {
                cell.player < player -> cell to mark
                cell.player > player -> cell.copy(player = cell.player - 1) to mark
                else -> null
            }
        }.toMap()
        return Sheet(players.filterIndexed { i, _ -> i != player }, shifted)
    }

    fun cleared(): Sheet = copy(marks = emptyMap())

    companion object {
        const val MIN_PLAYERS = 2
        const val MAX_PLAYERS = 6
        private const val DEFAULT_PLAYERS = 4

        /** Default name per column: the phone's owner first, then the others by ordinal. */
        val DEFAULT_NAMES = listOf("Ja", "Drugi", "Treći", "Četvrti", "Peti", "Šesti")

        fun initial() = Sheet(DEFAULT_NAMES.take(DEFAULT_PLAYERS))

        /** Player names are stored one per line, so a name must stay on a single line. */
        fun sanitizeName(name: String): String = name.replace(Regex("\\s+"), " ").trim()
    }
}

/**
 * Plain-text encoding of a [Sheet] for SharedPreferences. Players are one per
 * line; marks are `card,player,MARK` entries separated by `;`. Decoding skips
 * any entry that doesn't fit the current card list and player count, and falls
 * back to [Sheet.initial] when the player list itself is unusable.
 */
object SheetCodec {
    fun encodePlayers(sheet: Sheet): String = sheet.players.joinToString("\n")

    fun encodeMarks(sheet: Sheet): String =
        sheet.marks.entries.joinToString(";") { (cell, mark) -> "${cell.card},${cell.player},${mark.name}" }

    fun decode(players: String?, marks: String?): Sheet {
        val names = players?.split('\n')
        if (names == null || names.size !in Sheet.MIN_PLAYERS..Sheet.MAX_PLAYERS) return Sheet.initial()
        val decoded = marks.orEmpty().split(';').mapNotNull { entry ->
            val parts = entry.split(',')
            if (parts.size != 3) return@mapNotNull null
            val card = parts[0].toIntOrNull() ?: return@mapNotNull null
            val player = parts[1].toIntOrNull() ?: return@mapNotNull null
            val mark = Mark.entries.firstOrNull { it.name == parts[2] } ?: return@mapNotNull null
            if (card !in 0 until Category.CARD_COUNT || player !in names.indices) return@mapNotNull null
            Cell(card, player) to mark
        }.toMap()
        return Sheet(names, decoded)
    }
}
