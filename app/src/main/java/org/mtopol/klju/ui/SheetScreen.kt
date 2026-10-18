package org.mtopol.klju.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import org.mtopol.klju.Category
import org.mtopol.klju.KljuViewModel
import org.mtopol.klju.Mark
import org.mtopol.klju.R
import org.mtopol.klju.Sheet

private val NameColumnWidth = 132.dp
private val HeaderHeight = 48.dp
private val MinRowHeight = 32.dp
private val MaxRowHeight = 56.dp
private const val CategoryToRowRatio = 0.75f

/** Row height at which text and icons are drawn at their base size. */
private val BaseRowHeight = 36.dp
private const val MaxScale = 1.3f

/**
 * Sizes that follow the screen height. Rows stretch so the whole sheet fills the screen,
 * and once they are taller than [BaseRowHeight], text, icons and the card-name column
 * grow with them, up to [MaxScale].
 */
private class SheetMetrics(val rowHeight: Dp, val categoryHeight: Dp) {
    val scale = (rowHeight / BaseRowHeight).coerceIn(1f, MaxScale)
    val nameColumnWidth = NameColumnWidth * scale
}

@Composable
fun SheetScreen(vm: KljuViewModel) {
    val sheet by vm.sheet.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }
    var editedPlayer by remember { mutableStateOf<Int?>(null) }

    Surface(Modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            val metrics = sheetMetrics(maxHeight)
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, density.fontScale * metrics.scale),
            ) {
                SheetContent(
                    sheet = sheet,
                    metrics = metrics,
                    onAddPlayer = {
                        // The new player lands in the column after the current last one.
                        editedPlayer = sheet.players.size
                        vm.addPlayer()
                    },
                    onClear = { confirmClear = true },
                    onPlayerClick = { editedPlayer = it },
                    onMark = vm::setMark,
                )
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_sheet_title)) },
            text = { Text(stringResource(R.string.clear_sheet_text)) },
            confirmButton = {
                TextButton(onClick = { vm.clear(); confirmClear = false }) {
                    Text(stringResource(R.string.clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    editedPlayer?.let { player ->
        if (player !in sheet.players.indices) return@let
        PlayerDialog(
            name = sheet.players[player],
            canRemove = sheet.canRemovePlayer(player),
            onRename = { vm.renamePlayer(player, it); editedPlayer = null },
            onRemove = { vm.removePlayer(player); editedPlayer = null },
            onDismiss = { editedPlayer = null },
        )
    }
}

/**
 * Fits every category header and card row into what's left of [screenHeight] after the
 * system bars and the player header, keeping rows within tap-friendly bounds. Works in
 * whole pixels so the rows never overshoot by rounding.
 */
@Composable
private fun sheetMetrics(screenHeight: Dp): SheetMetrics {
    val density = LocalDensity.current
    val barsPx = WindowInsets.statusBars.getTop(density) + WindowInsets.navigationBars.getBottom(density)
    return with(density) {
        val availablePx = screenHeight.roundToPx() - barsPx - HeaderHeight.roundToPx()
        val units = Category.CARD_COUNT + Category.entries.size * CategoryToRowRatio
        val rowHeight = (availablePx / units).toInt().toDp().coerceIn(MinRowHeight, MaxRowHeight)
        val categoryHeight = (rowHeight.toPx() * CategoryToRowRatio).toInt().toDp()
        SheetMetrics(rowHeight, categoryHeight)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SheetContent(
    sheet: Sheet,
    metrics: SheetMetrics,
    onAddPlayer: () -> Unit,
    onClear: () -> Unit,
    onPlayerClick: (Int) -> Unit,
    onMark: (card: Int, player: Int, Mark?) -> Unit,
) {
    Column {
        PlayerHeader(
            players = sheet.players,
            nameColumnWidth = metrics.nameColumnWidth,
            canAddPlayer = sheet.canAddPlayer(),
            onAddPlayer = onAddPlayer,
            onClear = onClear,
            onPlayerClick = onPlayerClick,
        )
        LazyColumn(Modifier.weight(1f).navigationBarsPadding()) {
            Category.entries.forEach { category ->
                stickyHeader(key = category.name) { CategoryHeader(category.title, metrics.categoryHeight) }
                itemsIndexed(category.cards, key = { _, card -> card }) { i, card ->
                    CardRow(
                        card = card,
                        metrics = metrics,
                        striped = i % 2 == 1,
                        playerCount = sheet.players.size,
                        markOf = { player -> sheet.mark(category.firstCard + i, player) },
                        onMark = { player, mark -> onMark(category.firstCard + i, player, mark) },
                    )
                }
            }
        }
    }
}

/**
 * The burgundy band at the top: the sheet's actions sit above the card-name column,
 * and each player's name heads their column.
 */
@Composable
private fun PlayerHeader(
    players: List<String>,
    nameColumnWidth: Dp,
    canAddPlayer: Boolean,
    onAddPlayer: () -> Unit,
    onClear: () -> Unit,
    onPlayerClick: (Int) -> Unit,
) {
    val rule = Color.White.copy(alpha = 0.3f)
    val iconColors = IconButtonDefaults.iconButtonColors(
        contentColor = Color.White,
        disabledContentColor = Color.White.copy(alpha = 0.38f),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(Burgundy)
            .statusBarsPadding()
            .height(HeaderHeight),
    ) {
        Row(
            Modifier.width(nameColumnWidth).fillMaxHeight().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onAddPlayer, enabled = canAddPlayer, colors = iconColors) {
                Icon(Icons.Filled.PersonAdd, stringResource(R.string.add_player))
            }
            IconButton(onClick = onClear, colors = iconColors) {
                Icon(Icons.Filled.DeleteSweep, stringResource(R.string.clear_sheet))
            }
        }
        players.forEachIndexed { i, name ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .leftRule(rule)
                    .clickable { onPlayerClick(i) }
                    .padding(horizontal = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun CategoryHeader(title: String, height: Dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun CardRow(
    card: String,
    metrics: SheetMetrics,
    striped: Boolean,
    playerCount: Int,
    markOf: (Int) -> Mark?,
    onMark: (Int, Mark?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(metrics.rowHeight)
            .background(if (striped) colors.surfaceContainerLow else colors.surface)
            .bottomRule(colors.outlineVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            card,
            Modifier.width(metrics.nameColumnWidth).padding(start = 12.dp, end = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        repeat(playerCount) { player ->
            MarkCell(markOf(player), metrics.scale) { onMark(player, it) }
        }
    }
}

/** A sheet cell; tapping it opens a picker with every symbol plus an eraser. */
@Composable
private fun RowScope.MarkCell(mark: Mark?, scale: Float, onSelect: (Mark?) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Box(
        Modifier
            .weight(1f)
            .fillMaxHeight()
            .leftRule(MaterialTheme.colorScheme.outlineVariant)
            .clickable { picking = true },
        contentAlignment = Alignment.Center,
    ) {
        mark?.let { MarkIcon(it, scale) }
        if (picking) {
            MarkPicker(mark, scale, onSelect = { onSelect(it); picking = false }, onDismiss = { picking = false })
        }
    }
}

@Composable
private fun MarkPicker(mark: Mark?, scale: Float, onSelect: (Mark?) -> Unit, onDismiss: () -> Unit) {
    val gap = with(LocalDensity.current) { 4.dp.roundToPx() }
    Popup(
        popupPositionProvider = remember(gap) { AboveAnchor(gap) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Row(
                Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Mark.entries.forEach { option ->
                    IconButton(
                        onClick = { onSelect(option) },
                        modifier = if (option == mark) {
                            Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                        } else {
                            Modifier
                        },
                    ) {
                        MarkIcon(option, scale)
                    }
                }
                if (mark != null) {
                    IconButton(onClick = { onSelect(null) }) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.erase_mark))
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkIcon(mark: Mark, scale: Float) {
    val (icon: ImageVector, label: Int, size) = when (mark) {
        Mark.CHECK -> Triple(Icons.Filled.Check, R.string.mark_check, 24.dp)
        Mark.CROSS -> Triple(Icons.Filled.Close, R.string.mark_cross, 24.dp)
        Mark.EYE -> Triple(Icons.Filled.Visibility, R.string.mark_eye, 22.dp)
        Mark.CIRCLE -> Triple(Icons.Filled.Circle, R.string.mark_circle, 16.dp)
        Mark.QUESTION -> Triple(Icons.Filled.QuestionMark, R.string.mark_question, 22.dp)
    }
    Icon(icon, stringResource(label), Modifier.size(size * scale), tint = mark.color())
}

@Composable
private fun PlayerDialog(
    name: String,
    canRemove: Boolean,
    onRename: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(TextFieldValue(name, TextRange(0, name.length))) }
    val focus = remember { FocusRequester() }
    val confirm = { if (Sheet.sanitizeName(text.text).isNotEmpty()) onRename(text.text) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.player_name)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { confirm() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                if (canRemove) {
                    TextButton(
                        onClick = onRemove,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.remove_player), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = Sheet.sanitizeName(text.text).isNotEmpty()) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
    LaunchedEffect(Unit) { focus.requestFocus() }
}

/**
 * Places a popup just above its anchor, horizontally centered on it and kept inside the window.
 * When there is no room above, the popup goes just below the anchor.
 */
private class AboveAnchor(private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.center.x - popupContentSize.width / 2)
            .coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val above = anchorBounds.top - gap - popupContentSize.height
        return IntOffset(x, if (above >= 0) above else anchorBounds.bottom + gap)
    }
}

/** Draws the horizontal grid line along a row's bottom edge. */
private fun Modifier.bottomRule(color: Color) = drawBehind {
    val y = size.height - 0.5.dp.toPx()
    drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
}

/** Draws the vertical grid line on a cell's leading edge. */
private fun Modifier.leftRule(color: Color) = drawBehind {
    drawLine(color, Offset(0f, 0f), Offset(0f, size.height), strokeWidth = 1.dp.toPx())
}
