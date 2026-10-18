package org.mtopol.klju

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.updateAndGet

/** Holds the sheet and saves every change, so a game survives the app being closed. */
class KljuViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("sheet", Context.MODE_PRIVATE)

    private val _sheet = MutableStateFlow(
        SheetCodec.decode(prefs.getString(KEY_PLAYERS, null), prefs.getString(KEY_MARKS, null)),
    )
    val sheet: StateFlow<Sheet> = _sheet.asStateFlow()

    fun setMark(card: Int, player: Int, mark: Mark?) = update { it.withMark(card, player, mark) }

    fun renamePlayer(player: Int, name: String) = update { it.withPlayerName(player, name) }

    fun addPlayer() = update { it.withPlayerAdded() }

    fun removePlayer(player: Int) = update { it.withPlayerRemoved(player) }

    fun clear() = update { it.cleared() }

    private fun update(transform: (Sheet) -> Sheet) {
        val sheet = _sheet.updateAndGet(transform)
        prefs.edit {
            putString(KEY_PLAYERS, SheetCodec.encodePlayers(sheet))
            putString(KEY_MARKS, SheetCodec.encodeMarks(sheet))
        }
    }

    private companion object {
        const val KEY_PLAYERS = "players"
        const val KEY_MARKS = "marks"
    }
}
