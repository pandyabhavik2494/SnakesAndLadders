package com.pandyabhavik.snakes.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pandyabhavik.snakes.core.FinishRule
import com.pandyabhavik.snakes.core.Layout
import com.pandyabhavik.snakes.core.Rules

/** The whole app: title, setup, the game and the Studio, cross-fading between each other. */
@Composable
fun App(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sounds = remember { Sounds(context.applicationContext) }
    val haptics = remember { Haptics(context.applicationContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    val feel = remember { Feel(sounds, haptics) }
    feel.soundOn = state.settings.soundOn
    feel.hapticsOn = state.settings.hapticsOn
    val theme = state.settings.theme

    BackHandler(enabled = state.screen != Screen.TITLE) { viewModel.back() }
    Box(Modifier.fillMaxSize().background(theme.wallEdge)) {
        if (!state.loaded) return@Box
        AnimatedContent(state.screen, transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) }, label = "screen") { screen ->
            when (screen) {
                Screen.TITLE -> TitleScreen(state, viewModel, feel)
                Screen.SETUP -> SetupScreen(state, viewModel, feel)
                Screen.GAME -> GameScreen(state, viewModel, feel)
                Screen.STUDIO -> StudioScreen(state, viewModel, feel)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------

@Composable
private fun TitleScreen(state: UiState, viewModel: GameViewModel, feel: Feel) {
    val theme = state.settings.theme
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = -1f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "d",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 560.dp
        BoardView(
            state.game.layout, theme,
            Modifier.fillMaxSize().graphicsLayer { scaleX = 1.08f; scaleY = 1.08f; translationY = drift * 18.dp.toPx() },
            positions = state.game.positions, colors = state.settings.colors, piece = state.settings.piece,
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to theme.wallEdge.copy(alpha = 0.55f), 0.62f to theme.wallEdge.copy(alpha = 0.95f), 1f to theme.wallEdge)))
        Column(
            Modifier.align(if (wide) Alignment.Center else Alignment.BottomCenter).widthIn(max = 440.dp).fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing).padding(horizontal = 22.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                buildAnnotatedString {
                    append("Snakes\n")
                    withStyle(SpanStyle(color = theme.accent, fontWeight = FontWeight.Medium)) { append("& ") }
                    append("Ladders")
                },
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 54.sp, lineHeight = 54.sp), color = theme.ink,
            )
            Spacer(Modifier.height(6.dp))
            if (state.inProgress) {
                val g = state.game
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(theme.glass).border(1.dp, theme.line, RoundedCornerShape(24.dp))
                        .clickable { feel.tap(); viewModel.go(Screen.GAME) }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(width = 44.dp, height = 30.dp)) {
                        drawPiece(Offset(size.height / 2, size.height / 2), size.height * 0.4f, state.settings.colors[0], state.settings.piece)
                        drawPiece(Offset(size.width - size.height / 2, size.height / 2), size.height * 0.4f, state.settings.colors[1], state.settings.piece)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Continue game", style = MaterialTheme.typography.titleMedium, color = theme.ink)
                        Text(
                            "${state.players.name(0)} on ${g.positions[0]} · ${state.players.name(1)} on ${g.positions[1]} · ${state.players.name(g.current)} to roll",
                            style = MaterialTheme.typography.bodySmall, color = theme.dim, maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text("PLAY", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { feel.tap(); viewModel.go(Screen.SETUP) }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.onAccent),
                ) { Text("New game", style = MaterialTheme.typography.labelLarge) }
                OutlinedButton(
                    onClick = { feel.tap(); viewModel.go(Screen.STUDIO) }, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(50),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.line),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.ink),
                ) { Text("Studio", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------

@Composable
private fun SetupScreen(state: UiState, viewModel: GameViewModel, feel: Feel) {
    val theme = state.settings.theme
    var onePlayer by remember { mutableStateOf(state.players.vsPhone) }
    var names by remember { mutableStateOf(state.players.names) }
    var layout by remember { mutableStateOf(state.game.layout) }
    var finish by remember { mutableStateOf(state.game.rules.finish) }
    var sixAgain by remember { mutableStateOf(state.game.rules.sixRollsAgain) }
    var rulesOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Int?>(null) }
    val players = Players(names, onePlayer)

    BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.wallCentre, theme.wallEdge)))) {
        // Board previews are a little shorter when there's less height to spare relative to width.
        val thumbAspect = if (maxHeight < maxWidth * 1.6f) 1f / 1.15f else 1f / 1.5f
        Column(
            Modifier.align(Alignment.TopCenter).widthIn(max = 520.dp).fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.back() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = theme.dim) }
                Text("Who's playing?", style = MaterialTheme.typography.displaySmall, color = theme.ink)
            }
            Segmented(listOf("1 player", "2 players"), if (onePlayer) 0 else 1, { feel.tap(); onePlayer = it == 0 }, theme)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (p in 0..1) {
                    Seat(state, players, p, Modifier.weight(1f), onRename = { renaming = p }, onColor = { c -> feel.tap(); viewModel.setColor(p, c) })
                }
            }
            Label("BOARD", theme)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (l in Layout.entries) {
                    val on = l == layout
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .border(if (on) 2.dp else 1.dp, if (on) theme.accent else theme.line, RoundedCornerShape(16.dp))
                            .clickable { feel.tap(); layout = l }.semantics { selected = on; contentDescription = "${l.label} board" }.padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BoardView(l, theme, Modifier.fillMaxWidth().aspectRatio(thumbAspect).clip(RoundedCornerShape(10.dp)))
                        Spacer(Modifier.height(6.dp))
                        Text(l.label, style = MaterialTheme.typography.bodyMedium, color = if (on) theme.ink else theme.dim)
                    }
                }
            }
            Text(
                when (layout) {
                    Layout.CLASSIC -> "Eight ladders, eight snakes. The board everyone knows."
                    Layout.GENTLE -> "More ladders, fewer snakes. Quick and kind."
                    Layout.WILD -> "Long ladders, longer snakes. Anything can happen."
                },
                style = MaterialTheme.typography.bodySmall, color = theme.dim,
            )
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(theme.ink.copy(alpha = 0.05f))) {
                Row(
                    Modifier.fillMaxWidth().clickable { feel.tap(); rulesOpen = !rulesOpen }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("House rules", style = MaterialTheme.typography.titleMedium, color = theme.ink, modifier = Modifier.weight(1f))
                    Text("${finish.label} · ${if (sixAgain) "six rolls again" else "no extra turn"}", style = MaterialTheme.typography.bodySmall, color = theme.dim)
                }
                AnimatedVisibility(rulesOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        Segmented(FinishRule.entries.map { it.label }, finish.ordinal, { feel.tap(); finish = FinishRule.entries[it] }, theme)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when (finish) {
                                FinishRule.EXACT -> "You need the exact number to land on 100."
                                FinishRule.BOUNCE -> "Too high a roll bounces you back off 100."
                                FinishRule.ANY -> "Any roll that reaches 100 wins."
                            },
                            style = MaterialTheme.typography.bodySmall, color = theme.dim,
                        )
                        Toggle("A six rolls again", null, sixAgain, { sixAgain = it }, theme)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { feel.tap(); viewModel.newGame(layout, Rules(finish, sixAgain), Players(names.map { it.trim() }, onePlayer)) },
                modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.onAccent),
            ) { Text("Start · ${players.name(0)} rolls first", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            Spacer(Modifier.height(8.dp))
        }
    }

    renaming?.let { p ->
        var draft by remember(p) { mutableStateOf(names[p]) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = theme.surface,
            title = { Text("Player ${p + 1}", style = MaterialTheme.typography.headlineSmall, color = theme.ink) },
            text = {
                OutlinedTextField(
                    value = draft, onValueChange = { draft = it.take(14) }, singleLine = true, shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    textStyle = MaterialTheme.typography.titleMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = theme.ink, unfocusedTextColor = theme.ink, focusedBorderColor = theme.accent,
                        unfocusedBorderColor = theme.line, cursorColor = theme.accent,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = { names = names.toMutableList().also { it[p] = draft.trim().ifEmpty { "Player ${p + 1}" } }; renaming = null }) {
                    Text("Done", color = theme.accent, style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
}

@Composable
private fun Seat(state: UiState, players: Players, p: Int, modifier: Modifier, onRename: () -> Unit, onColor: (PieceColor) -> Unit) {
    val theme = state.settings.theme
    val computer = players.isPhone(p)
    Column(
        modifier.clip(RoundedCornerShape(22.dp)).background(theme.ink.copy(alpha = 0.05f)).border(1.dp, theme.line, RoundedCornerShape(22.dp))
            .clickable(enabled = !computer, onClick = onRename).padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(56.dp)) { drawPiece(center, size.minDimension * 0.34f, state.settings.colors[p], state.settings.piece) }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(players.name(p), style = MaterialTheme.typography.titleMedium, color = theme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (!computer) {
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.Edit, contentDescription = "Rename", tint = theme.dim, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (c in PieceColor.entries.take(6)) {
                val on = state.settings.colors[p] == c
                Box(
                    Modifier.size(18.dp).then(if (on) Modifier.border(2.dp, theme.accent, CircleShape) else Modifier).padding(if (on) 3.dp else 1.dp)
                        .clip(CircleShape).background(Brush.radialGradient(listOf(c.light, c.base, c.dark)))
                        .clickable { onColor(c) }.semantics { contentDescription = "${c.label} for ${players.name(p)}"; selected = on },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------

private enum class StudioTab(val label: String) { TABLE("Table"), PIECES("Pieces"), DICE("Dice"), FEEL("Sound & touch") }

/** All the looks in one place, with a live board that changes as you tap. */
@Composable
private fun StudioScreen(state: UiState, viewModel: GameViewModel, feel: Feel) {
    val theme = state.settings.theme
    var tab by remember { mutableStateOf(StudioTab.TABLE) }
    val preview = @Composable { modifier: Modifier ->
        BoardView(
            state.game.layout, theme, modifier,
            positions = listOf(24, 33), colors = state.settings.colors, piece = state.settings.piece, current = 0,
        )
    }
    val panel = @Composable { modifier: Modifier ->
        Column(modifier.background(theme.glass).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Studio", style = MaterialTheme.typography.displaySmall, color = theme.ink, modifier = Modifier.weight(1f))
                TextButton(onClick = { feel.tap(); viewModel.back() }) { Text("Done", color = theme.accent, style = MaterialTheme.typography.labelLarge) }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (t in StudioTab.entries) {
                    val on = t == tab
                    Text(
                        t.label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = if (on) theme.onAccent else theme.dim, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) theme.accent else Color.Transparent)
                            .clickable { feel.tap(); tab = t }.semantics { selected = on }.padding(horizontal = 10.dp, vertical = 9.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            when (tab) {
                StudioTab.TABLE -> SwatchGrid(Theme.entries, state.settings.theme, { it.label }, { feel.tap(); viewModel.setTheme(it) }, theme) { t ->
                    BoardView(state.game.layout, t, Modifier.fillMaxSize())
                }
                StudioTab.PIECES -> SwatchGrid(PieceStyle.entries, state.settings.piece, { it.label }, { feel.tap(); viewModel.setPiece(it) }, theme) { style ->
                    Canvas(Modifier.fillMaxSize()) {
                        val r = size.height * 0.26f
                        drawPiece(Offset(size.width * 0.36f, size.height * 0.52f), r, state.settings.colors[0], style)
                        drawPiece(Offset(size.width * 0.64f, size.height * 0.52f), r, state.settings.colors[1], style)
                    }
                }
                StudioTab.DICE -> SwatchGrid(DiceStyle.entries, state.settings.dice, { it.label }, { feel.tap(); viewModel.setDice(it) }, theme) { dice ->
                    Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = -8f }) {
                        val s = size.height * 0.62f
                        drawDieFace(5, dice, s, Offset((size.width - s) / 2, (size.height - s) / 2))
                    }
                }
                StudioTab.FEEL -> Column {
                    Toggle("Sound", "Dice, wood and marimba", state.settings.soundOn, { viewModel.setSound(it); feel.tap() }, theme)
                    Toggle("Haptics", "Rattle, ticks and swells", state.settings.hapticsOn, { viewModel.setHaptics(it); feel.tap() }, theme)
                    Toggle("Show reachable squares", "A glow on the six squares before each roll", state.settings.hints, viewModel::setHints, theme)
                    Toggle("Shake to roll", "Give the phone a shake to throw the die", state.settings.shake, viewModel::setShake, theme)
                }
            }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(theme.wallEdge)) {
        val insets = WindowInsets.safeDrawing.asPaddingValues()
        if (maxWidth > 560.dp) {
            Row(Modifier.fillMaxSize()) {
                preview(Modifier.weight(1.15f).fillMaxHeight().padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()))
                panel(Modifier.weight(1f).fillMaxHeight().padding(top = insets.calculateTopPadding(), bottom = insets.calculateBottomPadding()))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                preview(Modifier.fillMaxWidth().weight(0.42f).padding(top = insets.calculateTopPadding()))
                panel(Modifier.fillMaxWidth().weight(0.58f).padding(bottom = insets.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun <T> SwatchGrid(options: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit, theme: Theme, art: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in options.chunked(2)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (option in row) {
                    val on = option == selected
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(theme.ink.copy(alpha = 0.04f))
                            .border(if (on) 2.dp else 1.dp, if (on) theme.accent else theme.line, RoundedCornerShape(18.dp))
                            .clickable { onPick(option) }.semantics { contentDescription = label(option); this.selected = on }.padding(8.dp),
                    ) {
                        Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(12.dp))) { art(option) }
                        Spacer(Modifier.height(6.dp))
                        Text(label(option), style = MaterialTheme.typography.bodyMedium, color = if (on) theme.ink else theme.dim, maxLines = 1)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------

@Composable
private fun Label(text: String, theme: Theme) = Text(text, style = MaterialTheme.typography.labelSmall, color = theme.dim)

/** Mutually exclusive choices in one pill. */
@Composable
private fun Segmented(options: List<String>, selected: Int, onPick: (Int) -> Unit, theme: Theme) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(theme.ink.copy(alpha = 0.06f)).padding(4.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Text(
                label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = if (on) theme.onAccent else theme.ink,
                textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) theme.accent else Color.Transparent)
                    .clickable { onPick(i) }.semantics { this.selected = on }.padding(vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun Toggle(label: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit, theme: Theme) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = theme.ink)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = theme.dim)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = theme.onAccent, checkedTrackColor = theme.accent,
                uncheckedThumbColor = theme.dim, uncheckedTrackColor = theme.wallEdge.copy(alpha = 0.4f), uncheckedBorderColor = theme.dim.copy(alpha = 0.5f),
            ),
        )
    }
}
