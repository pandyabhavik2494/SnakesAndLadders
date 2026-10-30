package com.pandyabhavik.snakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandyabhavik.snakes.core.FinishRule
import com.pandyabhavik.snakes.core.Game
import com.pandyabhavik.snakes.core.Layout
import com.pandyabhavik.snakes.core.Rules

/** Sound, touch and the whole look of the table. Changes apply at once, behind the sheet, and are remembered. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: Settings,
    players: Players,
    onSound: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onTheme: (Theme) -> Unit,
    onPiece: (PieceStyle) -> Unit,
    onColor: (Int, PieceColor) -> Unit,
    onDice: (DiceStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val theme = settings.theme
    Sheet(theme, onDismiss) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, color = theme.ink)
        Spacer(Modifier.height(10.dp))
        ToggleRow("Sound", settings.soundOn, onSound, theme)
        ToggleRow("Haptics", settings.hapticsOn, onHaptics, theme)

        Section("THEME", theme)
        SwatchRow(Theme.entries, settings.theme, { it.label }, onTheme, theme, wide = true) { t, w, h -> drawThemeSwatch(t, w, h) }

        Section("PIECES", theme)
        SwatchRow(PieceStyle.entries, settings.piece, { it.label }, onPiece, theme) { style, w, h ->
            drawPiece(Offset(w / 2, h * 0.55f), h * 0.28f, settings.colors[0], style)
        }

        for (p in 0..1) {
            Section("${players.name(p).uppercase()}'S COLOUR", theme)
            SwatchRow(PieceColor.entries, settings.colors[p], { it.label }, { onColor(p, it) }, theme) { color, w, h ->
                drawPiece(Offset(w / 2, h * 0.55f), h * 0.28f, color, settings.piece)
            }
        }

        Section("DICE", theme)
        SwatchRow(DiceStyle.entries, settings.dice, { it.label }, onDice, theme) { dice, w, h ->
            val s = h * 0.62f
            translate((w - s) / 2, (h - s) / 2) { drawDieAt(s, dice) }
        }
    }
}

private fun DrawScope.drawDieAt(s: Float, dice: DiceStyle) {
    drawRoundRect(Brush.linearGradient(listOf(dice.light, dice.dark), start = Offset.Zero, end = Offset(s, s)), size = Size(s, s), cornerRadius = CornerRadius(s * 0.22f))
    drawRoundRect(dice.edge, size = Size(s, s), cornerRadius = CornerRadius(s * 0.22f), style = Stroke(1.5f))
    for ((i, j) in listOf(-1 to -1, 0 to 0, 1 to 1)) drawCircle(dice.pip, s * 0.09f, Offset(s / 2 + i * s * 0.26f, s / 2 + j * s * 0.26f))
}

/** A tiny board corner in the theme: two tiles, a ladder rail and a curl of snake. */
private fun DrawScope.drawThemeSwatch(t: Theme, w: Float, h: Float) {
    val r = CornerRadius(h * 0.2f)
    drawRoundRect(Brush.radialGradient(listOf(t.wallCentre, t.wallEdge), center = Offset(w * 0.3f, h * 0.3f), radius = w), size = Size(w, h), cornerRadius = r)
    val tile = h * 0.32f
    val x0 = w * 0.16f
    val y0 = h * 0.18f
    for (i in 0..1) for (j in 0..1) drawRect(if ((i + j) % 2 == 0) t.tileA else t.tileB, topLeft = Offset(x0 + i * tile, y0 + j * tile), size = Size(tile, tile))
    drawRect(t.frame, topLeft = Offset(x0, y0), size = Size(tile * 2, tile * 2), style = Stroke(1.5f))
    drawLine(t.ladderLight, Offset(x0 + tile * 0.4f, y0 + tile * 1.8f), Offset(x0 + tile * 0.4f, y0 + tile * 0.2f), h * 0.05f)
    drawLine(t.ladderLight, Offset(x0 + tile * 0.75f, y0 + tile * 1.8f), Offset(x0 + tile * 0.75f, y0 + tile * 0.2f), h * 0.05f)
    for (k in 1..3) drawLine(t.rung, Offset(x0 + tile * 0.4f, y0 + tile * 0.45f * k), Offset(x0 + tile * 0.75f, y0 + tile * 0.45f * k), h * 0.035f)
    val snake = Curve(Offset(w * 0.78f, h * 0.22f), Offset(w * 0.45f, h * 0.35f), Offset(w * 0.95f, h * 0.6f), Offset(w * 0.6f, h * 0.85f))
    for (i in 30 downTo 0) {
        val f = i / 30f
        drawCircle(Brush.radialGradient(listOf(t.snakeLight, t.snakeDark)), h * (0.085f * (1 - f) + 0.02f), snake.at(f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Sheet(theme: Theme, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.surface,
        contentColor = theme.ink,
        scrimColor = theme.wallEdge.copy(alpha = 0.55f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
        ) { content() }
    }
}

/** Who's playing, on which board, by which rules. */
@Composable
fun NewGameSheet(
    theme: Theme,
    current: Game,
    players: Players,
    onStart: (Layout, Rules, Players) -> Unit,
    onDismiss: () -> Unit,
) {
    var vsPhone by remember { mutableStateOf(players.vsPhone) }
    var name1 by remember { mutableStateOf(players.names[0]) }
    var name2 by remember { mutableStateOf(players.names[1]) }
    var layout by remember { mutableStateOf(current.layout) }
    var finish by remember { mutableStateOf(current.rules.finish) }
    var sixAgain by remember { mutableStateOf(current.rules.sixRollsAgain) }

    Sheet(theme, onDismiss) {
        Text("New game", style = MaterialTheme.typography.headlineSmall, color = theme.ink)

        Section("PLAYERS", theme)
        Segmented(listOf("Two players", "Versus phone"), if (vsPhone) 1 else 0, { vsPhone = it == 1 }, theme)
        Spacer(Modifier.height(12.dp))
        NameField("Player 1", name1, { name1 = it.take(16) }, theme)
        if (!vsPhone) {
            Spacer(Modifier.height(8.dp))
            NameField("Player 2", name2, { name2 = it.take(16) }, theme)
        }

        Section("BOARD", theme)
        Segmented(Layout.entries.map { it.label }, layout.ordinal, { layout = Layout.entries[it] }, theme)
        Spacer(Modifier.height(6.dp))
        Text(
            when (layout) {
                Layout.CLASSIC -> "Eight ladders, eight snakes. The board everyone knows."
                Layout.GENTLE -> "More ladders, fewer snakes. Quick and kind."
                Layout.WILD -> "Long ladders, longer snakes. Anything can happen."
            },
            style = MaterialTheme.typography.bodySmall, color = theme.dim,
        )

        Section("FINISHING", theme)
        Segmented(FinishRule.entries.map { it.label }, finish.ordinal, { finish = FinishRule.entries[it] }, theme)
        Spacer(Modifier.height(6.dp))
        Text(
            when (finish) {
                FinishRule.EXACT -> "You need the exact number to land on 100."
                FinishRule.BOUNCE -> "Too high a roll bounces you back off 100."
                FinishRule.ANY -> "Any roll that reaches 100 wins."
            },
            style = MaterialTheme.typography.bodySmall, color = theme.dim,
        )
        Spacer(Modifier.height(6.dp))
        ToggleRow("A six rolls again", sixAgain, { sixAgain = it }, theme)

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onStart(layout, Rules(finish, sixAgain), Players(listOf(name1.trim(), name2.trim()), vsPhone)) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.onAccent),
        ) { Text("Start game", style = MaterialTheme.typography.labelLarge) }
    }
}

@Composable
private fun NameField(label: String, value: String, onChange: (String) -> Unit, theme: Theme) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
        textStyle = MaterialTheme.typography.titleMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = theme.ink, unfocusedTextColor = theme.ink,
            focusedBorderColor = theme.accent, unfocusedBorderColor = theme.ink.copy(alpha = 0.18f),
            focusedLabelColor = theme.accent, unfocusedLabelColor = theme.dim, cursorColor = theme.accent,
        ),
    )
}

/** A row of mutually exclusive choices in one pill. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit, theme: Theme) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(theme.ink.copy(alpha = 0.06f))
            .padding(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (on) theme.onAccent else theme.ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) theme.accent else Color.Transparent)
                    .clickable { onPick(i) }
                    .semantics { this.selected = on }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun Section(title: String, theme: Theme) {
    Spacer(Modifier.height(22.dp))
    Text(title, style = MaterialTheme.typography.labelSmall, color = theme.dim)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, theme: Theme) {
    Row(Modifier.fillMaxWidth().height(50.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = theme.ink)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.onAccent,
                checkedTrackColor = theme.accent,
                uncheckedThumbColor = theme.dim,
                uncheckedTrackColor = theme.wallEdge.copy(alpha = 0.4f),
                uncheckedBorderColor = theme.dim.copy(alpha = 0.5f),
            ),
        )
    }
}

/** Swatches in a row that scrolls sideways when it doesn't fit; the chosen one is ringed in the accent. */
@Composable
private fun <T> SwatchRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onPick: (T) -> Unit,
    theme: Theme,
    wide: Boolean = false,
    draw: DrawScope.(T, Float, Float) -> Unit,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (option in options) {
            val chosen = option == selected
            Column(
                Modifier
                    .widthIn(min = if (wide) 84.dp else 60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onPick(option) }
                    .semantics { contentDescription = label(option); this.selected = chosen }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(
                    Modifier
                        .size(if (wide) 76.dp else 52.dp, 52.dp)
                        .border(if (chosen) 2.dp else 1.dp, if (chosen) theme.accent else theme.ink.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                        .padding(3.dp),
                ) { draw(option, size.width, size.height) }
                Spacer(Modifier.height(6.dp))
                Text(
                    label(option),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = if (chosen) theme.ink else theme.dim,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.width(if (wide) 84.dp else 60.dp),
                )
            }
        }
    }
}
