package org.mtopol.klju

import org.junit.Test
import kotlin.test.assertEquals

class SheetTest {

    @Test
    fun removingPlayerShiftsLaterColumnsLeft() {
        val sheet = Sheet(listOf("A", "B", "C"))
            .withMark(0, 0, Mark.CHECK)
            .withMark(1, 1, Mark.CROSS)
            .withMark(2, 2, Mark.EYE)

        val removed = sheet.withPlayerRemoved(1)

        assertEquals(listOf("A", "C"), removed.players)
        assertEquals(mapOf(Cell(0, 0) to Mark.CHECK, Cell(2, 1) to Mark.EYE), removed.marks)
    }

    @Test
    fun firstColumnCannotBeRemoved() {
        val sheet = Sheet(listOf("Ja", "B", "C"))
        assertEquals(sheet, sheet.withPlayerRemoved(0))
    }

    @Test
    fun initialSheetStartsWithSelf() {
        assertEquals(listOf("Ja", "Drugi", "Treći", "Četvrti"), Sheet.initial().players)
    }

    @Test
    fun playerCountStaysWithinBounds() {
        val two = Sheet(listOf("A", "B"))
        assertEquals(two, two.withPlayerRemoved(0))

        val six = (1..10).fold(two) { s, _ -> s.withPlayerAdded() }
        assertEquals(Sheet.MAX_PLAYERS, six.players.size)
    }

    @Test
    fun addedPlayerIsNamedByColumn() {
        assertEquals("Treći", Sheet(listOf("Ja", "Ana")).withPlayerAdded().players.last())
        // After Drugi was removed, Treći sits in column 2; the next free ordinal fills the gap.
        assertEquals("Četvrti", Sheet(listOf("Ja", "Treći")).withPlayerAdded().players.last())
        assertEquals("Drugi", Sheet(listOf("Ja", "Treći", "Četvrti", "Peti", "Šesti")).withPlayerAdded().players.last())
    }

    @Test
    fun namesAreTrimmedToOneLine() {
        val sheet = Sheet(listOf("A", "B")).withPlayerName(0, "  Ana\n Marić ")
        assertEquals("Ana Marić", sheet.players[0])
    }

    @Test
    fun codecRoundTrips() {
        val sheet = Sheet(listOf("Ana", "Ivo", "Mia"))
            .withMark(0, 0, Mark.CHECK)
            .withMark(20, 2, Mark.QUESTION)
            .withMark(7, 1, Mark.CIRCLE)

        val decoded = SheetCodec.decode(SheetCodec.encodePlayers(sheet), SheetCodec.encodeMarks(sheet))

        assertEquals(sheet, decoded)
    }

    @Test
    fun codecDropsEntriesOutsideTheSheet() {
        val decoded = SheetCodec.decode("Ana\nIvo", "0,0,CHECK;99,0,CROSS;1,5,EYE;bogus;2,1,NOPE")
        assertEquals(mapOf(Cell(0, 0) to Mark.CHECK), decoded.marks)
    }

    @Test
    fun codecFallsBackToInitialSheet() {
        assertEquals(Sheet.initial(), SheetCodec.decode(null, null))
        assertEquals(Sheet.initial(), SheetCodec.decode("Solo", ""))
    }
}
