package org.mtopol.klju

enum class Category(val title: String, val cards: List<String>) {
    SUSPECTS(
        "TKO?",
        listOf("Baulić", "Kihalić", "Kolar", "Gobler", "Milat", "Pipulić"),
    ),
    WEAPONS(
        "ŠTO?",
        listOf("Svijećnjak", "Bodež", "Olovna cijev", "Pištolj", "Konopac", "Ključ"),
    ),
    ROOMS(
        "GDJE?",
        listOf(
            "Plesna dvorana", "Soba za biljar", "Konzervatorij", "Blagovaonica", "Predvorje",
            "Kuhinja", "Knjižnica", "Dnevni boravak", "Radna soba",
        ),
    );

    /** Index of this category's first card in the sheet-wide card numbering. */
    val firstCard: Int
        get() = entries.take(ordinal).sumOf { it.cards.size }

    companion object {
        val CARD_COUNT = entries.sumOf { it.cards.size }
    }
}
