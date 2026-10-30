package com.pandyabhavik.snakes.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pandyabhavik.snakes.core.Turn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Where the pieces are drawn right now, in board units. Normally they sit on their squares; during a
 * roll the animation walks the moving piece square by square, up ladders and down snakes.
 */
private class Motion {
    val pos = mutableStateListOf(squareCentre(1), squareCentre(1))
    val lift = mutableStateListOf(0f, 0f)
    var face by mutableIntStateOf(6)
    var spin by mutableFloatStateOf(0f)
    var caption by mutableStateOf<String?>(null)
}

@Composable
fun GameScreen(viewModel: GameViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sounds = remember { Sounds(context.applicationContext) }
    val haptics = remember { Haptics(context.applicationContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    val motion = remember { Motion() }
    val theme = state.settings.theme

    val feel = remember { Feel(sounds, haptics) }
    feel.soundOn = state.settings.soundOn
    feel.hapticsOn = state.settings.hapticsOn

    // Every roll (and every new game) runs this once: dice, hops, ladder or snake, then hand back.
    LaunchedEffect(state.turnId, state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        val turn = state.turn
        val game = state.game
        if (turn == null || !state.busy) {
            for (p in 0..1) { motion.pos[p] = squareCentre(game.positions[p]); motion.lift[p] = 0f }
            motion.face = game.lastRoll ?: 6
            motion.spin = 0f
            motion.caption = null
            return@LaunchedEffect
        }
        motion.pos[1 - turn.player] = squareCentre(game.positions[1 - turn.player])
        motion.pos[turn.player] = squareCentre(turn.from)
        playTurn(turn, motion, feel, state.players)
        motion.caption = null
        viewModel.turnShown(state.turnId)
    }

    var showSettings by remember { mutableStateOf(false) }
    var showNewGame by remember { mutableStateOf(false) }
    var confirmNew by remember { mutableStateOf(false) }
    LaunchedEffect(state.firstRun) { if (state.firstRun) showNewGame = true }

    val canRoll = state.loaded && !state.busy && !state.game.isOver && !state.phoneToRoll
    val actions = Actions(
        roll = { if (canRoll) viewModel.roll() },
        settings = { feel.tap(); showSettings = true },
        newGame = {
            feel.tap()
            if (state.game.rollCount > 0 && !state.game.isOver) confirmNew = true else showNewGame = true
        },
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(theme.wallEdge)
            .background(Brush.radialGradient(listOf(theme.wallCentre, theme.wallEdge), radius = 2000f)),
    ) {
        AnimatedVisibility(visible = state.loaded, enter = fadeIn(tween(400))) {
            BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                if (maxWidth > maxHeight) {
                    WideLayout(state, motion, canRoll, actions)
                } else {
                    TallLayout(state, motion, canRoll, actions)
                }
            }
        }
        WinCard(state, onPlayAgain = { feel.tap(); viewModel.playAgain() }, onNewGame = { feel.tap(); showNewGame = true })
    }

    if (showSettings) {
        SettingsSheet(
            settings = state.settings,
            players = state.players,
            onSound = viewModel::setSound,
            onHaptics = viewModel::setHaptics,
            onTheme = { feel.tap(); viewModel.setTheme(it) },
            onPiece = { feel.tap(); viewModel.setPiece(it) },
            onColor = { p, c -> feel.tap(); viewModel.setColor(p, c) },
            onDice = { feel.tap(); viewModel.setDice(it) },
            onDismiss = { showSettings = false },
        )
    }
    if (showNewGame) {
        NewGameSheet(
            theme = theme,
            current = state.game,
            players = state.players,
            onStart = { layout, rules, players -> feel.tap(); showNewGame = false; viewModel.newGame(layout, rules, players) },
            onDismiss = { showNewGame = false; viewModel.dismissFirstRun() },
        )
    }
    if (confirmNew) {
        AlertDialog(
            onDismissRequest = { confirmNew = false },
            containerColor = theme.surface,
            titleContentColor = theme.ink,
            textContentColor = theme.dim,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Start a new game?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("This game is still going. Its pieces go back to square 1.", style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { confirmNew = false; showNewGame = true }) { Text("New game", color = theme.accent, style = MaterialTheme.typography.labelLarge) }
            },
            dismissButton = {
                TextButton(onClick = { confirmNew = false }) { Text("Keep playing", color = theme.dim, style = MaterialTheme.typography.labelLarge) }
            },
        )
    }
}

/** Sound and touch together, each obeying its own switch. */
private class Feel(private val sounds: Sounds, private val haptics: Haptics) {
    var soundOn = true
    var hapticsOn = true
    fun play(sound: Sound?, pattern: Haptics.Pattern?, rate: Float = 1f, volume: Float = 1f) {
        if (soundOn && sound != null) sounds.play(sound, rate, volume)
        if (hapticsOn && pattern != null) haptics.play(pattern)
    }
    fun tap() = play(Sound.TAP, Haptics.Pattern.TAP, volume = 0.6f)
}

private suspend fun playTurn(turn: Turn, motion: Motion, feel: Feel, players: Players) {
    val p = turn.player
    val name = players.name(p)
    motion.caption = if (players.isPhone(p)) "Phone is rolling" else "Rolling"

    // The die tumbles, showing random faces, and lands on the roll.
    feel.play(Sound.SHAKE, Haptics.Pattern.SHAKE)
    tumble(motion)
    motion.face = turn.roll
    motion.spin = 0f
    feel.play(Sound.LAND, Haptics.Pattern.LAND)
    motion.caption = "$name rolled ${turn.roll}"
    delay(320)

    if (turn.blocked) {
        motion.caption = "$name needs exactly ${100 - turn.from}"
        feel.play(Sound.BLOCKED, Haptics.Pattern.BLOCKED)
        delay(700)
    }

    // Square by square, each hop a little higher in pitch than the last.
    var at = motion.pos[p]
    turn.steps.forEachIndexed { i, square ->
        val from = at
        val to = squareCentre(square)
        val hop = Animatable(0f)
        hop.animateTo(1f, tween(210, easing = FastOutSlowInEasing)) {
            motion.pos[p] = from + (to - from) * value
            motion.lift[p] = sin(PI.toFloat() * value) * 0.6f
        }
        motion.lift[p] = 0f
        feel.play(Sound.HOP, Haptics.Pattern.HOP, rate = 0.92f + i * 0.035f)
        at = to
        delay(45)
    }

    if (turn.ladder) {
        delay(160)
        motion.caption = "A ladder! Up to ${turn.to}"
        feel.play(Sound.LADDER, Haptics.Pattern.LADDER)
        val from = at
        val to = squareCentre(turn.to)
        val climb = Animatable(0f)
        climb.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) {
            motion.pos[p] = from + (to - from) * value
            motion.lift[p] = 0.35f * sin(PI.toFloat() * value.coerceAtMost(0.5f))
        }
        val settle = Animatable(motion.lift[p])
        settle.animateTo(0f, tween(160)) { motion.lift[p] = value }
        feel.play(Sound.HOP, Haptics.Pattern.HOP, rate = 1.2f)
        delay(250)
    } else if (turn.snake) {
        delay(160)
        motion.caption = "A snake! Down to ${turn.to}"
        feel.play(Sound.SNAKE, Haptics.Pattern.SNAKE)
        val curve = Curve.snake(turn.landed, turn.to)
        val slide = Animatable(0f)
        slide.animateTo(1f, tween(1200, easing = FastOutSlowInEasing)) {
            motion.pos[p] = curve.at(value)
            motion.lift[p] = 0.12f * sin(PI.toFloat() * value)
        }
        motion.lift[p] = 0f
        feel.play(Sound.LAND, Haptics.Pattern.HOP, rate = 0.8f, volume = 0.6f)
        delay(250)
    }

    when {
        turn.won -> { feel.play(Sound.WIN, Haptics.Pattern.WIN); delay(300) }
        turn.again -> { motion.caption = "A six! $name rolls again"; feel.play(Sound.TURN, Haptics.Pattern.TURN, rate = 1.12f); delay(500) }
        else -> feel.play(Sound.TURN, Haptics.Pattern.TURN, volume = 0.7f)
    }
}

/** Spins the die for ~0.7 s, flicking through faces as it goes. */
private suspend fun tumble(motion: Motion) = kotlinx.coroutines.coroutineScope {
    val spin = Animatable(0f)
    val flicker = launch {
        var last = motion.face
        while (true) {
            var next = Random.nextInt(1, 7)
            if (next == last) next = next % 6 + 1
            motion.face = next
            last = next
            delay(75)
        }
    }
    spin.animateTo(1f, tween(720, easing = LinearEasing)) { motion.spin = value }
    flicker.cancel()
}

private class Actions(val roll: () -> Unit, val settings: () -> Unit, val newGame: () -> Unit)

/** Cover screen (or anything taller than wide): title, the two players, the board, the die. */
@Composable
private fun TallLayout(state: UiState, motion: Motion, canRoll: Boolean, actions: Actions) {
    val theme = state.settings.theme
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val side = 16.dp
        val boardSide = minOf(maxWidth - side * 2, maxHeight - 330.dp).coerceAtLeast(160.dp)
        val dieSize = if (maxHeight - boardSide > 420.dp) 92.dp else 76.dp
        Column(Modifier.fillMaxSize().padding(horizontal = side), horizontalAlignment = Alignment.CenterHorizontally) {
            TopBar(theme, actions, centred = true)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().widthIn(max = 560.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PlayerCard(state, 0, Modifier.weight(1f))
                PlayerCard(state, 1, Modifier.weight(1f))
            }
            Spacer(Modifier.weight(1f))
            BoardView(state, motion, Modifier.size(boardSide), onTap = actions.roll)
            Spacer(Modifier.weight(1f))
            Caption(state, motion)
            Spacer(Modifier.height(10.dp))
            Die(state, motion, canRoll, dieSize, actions.roll)
            Spacer(Modifier.height(8.dp))
            RollHint(state, canRoll)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Inner screen: a side panel on the left and the board on the right, sized together and centred. */
@Composable
private fun WideLayout(state: UiState, motion: Motion, canRoll: Boolean, actions: Actions) {
    val theme = state.settings.theme
    BoxWithConstraints(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        val panelWidth = if (maxWidth > 760.dp) 300.dp else 250.dp
        val gap = 36.dp
        val boardSide = minOf(maxHeight, maxWidth - panelWidth - gap)
        Row(Modifier.width(boardSide + gap + panelWidth).height(boardSide), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.width(panelWidth).fillMaxHeight(), horizontalAlignment = Alignment.Start) {
                TopBar(theme, actions, centred = false)
                Spacer(Modifier.height(20.dp))
                PlayerCard(state, 0, Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                PlayerCard(state, 1, Modifier.fillMaxWidth())
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Caption(state, motion)
                    Spacer(Modifier.height(14.dp))
                    Die(state, motion, canRoll, 104.dp, actions.roll)
                    Spacer(Modifier.height(10.dp))
                    RollHint(state, canRoll)
                }
                Spacer(Modifier.weight(0.4f))
            }
            Spacer(Modifier.width(gap))
            BoardView(state, motion, Modifier.size(boardSide), onTap = actions.roll)
        }
    }
}

@Composable
private fun TopBar(theme: Theme, actions: Actions, centred: Boolean) {
    Box(Modifier.fillMaxWidth().height(56.dp)) {
        if (centred) {
            IconButton(onClick = actions.newGame, modifier = Modifier.align(Alignment.CenterStart)) {
                Icon(Icons.Rounded.Replay, contentDescription = "New game", tint = theme.dim)
            }
        }
        Wordmark(theme, Modifier.align(if (centred) Alignment.Center else Alignment.CenterStart))
        Row(Modifier.align(Alignment.CenterEnd)) {
            if (!centred) {
                IconButton(onClick = actions.newGame) { Icon(Icons.Rounded.Replay, contentDescription = "New game", tint = theme.dim) }
            }
            IconButton(onClick = actions.settings) { Icon(Icons.Rounded.Tune, contentDescription = "Settings", tint = theme.dim) }
        }
    }
}

@Composable
private fun Wordmark(theme: Theme, modifier: Modifier = Modifier) {
    Text(
        buildAnnotatedString {
            append("Snakes ")
            withStyle(SpanStyle(color = theme.accent)) { append("&") }
            append(" Ladders")
        },
        style = MaterialTheme.typography.headlineSmall,
        color = theme.ink,
        maxLines = 1,
        modifier = modifier,
    )
}

@Composable
private fun PlayerCard(state: UiState, player: Int, modifier: Modifier) {
    val theme = state.settings.theme
    val game = state.game
    val active = !game.isOver && game.current == player
    val border by animateColorAsState(if (active) theme.accent else theme.ink.copy(alpha = 0.08f), tween(350), label = "border")
    val fill by animateColorAsState(if (active) theme.ink.copy(alpha = 0.09f) else theme.ink.copy(alpha = 0.035f), tween(350), label = "fill")
    val name = state.players.name(player)
    val detail = when {
        game.winner == player -> "Winner"
        active && state.players.isPhone(player) -> "Thinking"
        active -> "Your roll"
        else -> "Square ${game.positions[player]}"
    }
    Row(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .border(1.2.dp, border, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "$name, square ${game.positions[player]}${if (active) ", to roll" else ""}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(34.dp)) {
            drawPiece(center + Offset(0f, size.height * 0.06f), size.minDimension * 0.3f, state.settings.colors[player], state.settings.piece)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = theme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (active) theme.accent else theme.dim, maxLines = 1)
        }
    }
}

/** What just happened, in a few words. */
@Composable
private fun Caption(state: UiState, motion: Motion) {
    val theme = state.settings.theme
    val text = motion.caption ?: idleCaption(state)
    AnimatedContent(text, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) }, label = "caption") { line ->
        Text(line, style = MaterialTheme.typography.titleMedium, color = theme.ink, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun idleCaption(state: UiState): String {
    val game = state.game
    val players = state.players
    game.winner?.let { return "${players.name(it)} wins!" }
    val turn = state.turn
    val name = players.name(game.current)
    return when {
        turn == null -> if (game.rollCount == 0) "${players.name(0)} goes first" else "$name to roll"
        turn.again -> "A six! $name rolls again"
        turn.ladder -> "${players.name(turn.player)} climbed to ${turn.to}"
        turn.snake -> "${players.name(turn.player)} slid to ${turn.to}"
        else -> "$name to roll"
    }
}

@Composable
private fun RollHint(state: UiState, canRoll: Boolean) {
    val theme = state.settings.theme
    val text = when {
        state.game.isOver -> " "
        state.phoneToRoll -> "The phone rolls for itself"
        canRoll -> "Tap the die to roll"
        else -> " "
    }
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = theme.dim)
}

/** The die: tumbles in 3D while rolling, breathes gently when it's waiting for a tap. */
@Composable
private fun Die(state: UiState, motion: Motion, canRoll: Boolean, dieSize: Dp, onRoll: () -> Unit) {
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    val ready by animateFloatAsState(if (canRoll) 1f else 0f, tween(300), label = "ready")
    val spin = motion.spin
    val theme = state.settings.theme
    Box(
        Modifier
            .size(dieSize * 1.35f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = canRoll, onClick = onRoll)
            .testTag("die")
            .semantics { contentDescription = "Die showing ${motion.face}" + if (canRoll) ", tap to roll" else "" },
        contentAlignment = Alignment.Center,
    ) {
        // A soft halo under the die when it's yours to roll.
        Canvas(Modifier.fillMaxSize()) {
            val a = ready * (0.18f + 0.12f * breathe)
            drawCircle(Brush.radialGradient(listOf(theme.accent.copy(alpha = a), Color.Transparent)), radius = dieSize.toPx() * 0.68f)
        }
        Canvas(
            Modifier
                .size(dieSize)
                .graphicsLayer {
                    val s = 1f + sin(PI.toFloat() * spin) * 0.16f + ready * breathe * 0.025f
                    scaleX = s; scaleY = s
                    rotationZ = spin * 540f
                    rotationX = sin(spin * PI.toFloat() * 3) * 28f
                    rotationY = sin(spin * PI.toFloat() * 2) * 22f
                    translationY = -sin(PI.toFloat() * spin) * dieSize.toPx() * 0.25f
                    cameraDistance = 14f * density
                    shadowElevation = (10f + 14f * sin(PI.toFloat() * spin)) * density
                    shape = RoundedCornerShape(dieSize * 0.22f)
                    clip = false
                },
        ) {
            drawDie(motion.face, state.settings.dice)
        }
    }
}

/** The board and the pieces on it. The board is drawn once per look; only the pieces move. */
@Composable
private fun BoardView(state: UiState, motion: Motion, modifier: Modifier, onTap: () -> Unit) {
    val theme = state.settings.theme
    val text = androidx.compose.ui.text.rememberTextMeasurer()
    val game = state.game
    val description = "board, ${state.players.name(0)} on ${game.positions[0]}, ${state.players.name(1)} on ${game.positions[1]}"
    Box(
        modifier
            .padding(6.dp)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
            .semantics { contentDescription = description }
            .testTag("board"),
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer()) { drawBoard(game.layout, theme, text) }
        Canvas(Modifier.fillMaxSize()) {
            val c = size.minDimension / 10f
            val shared = (motion.pos[0] - motion.pos[1]).getDistance() < 0.3f
            // The piece in the air is drawn last, so it passes over the other one.
            val order = if (motion.lift[0] > motion.lift[1]) listOf(1, 0) else listOf(0, 1)
            for (p in order) {
                val nudge = if (shared) Offset(if (p == 0) -0.19f else 0.19f, 0f) else Offset.Zero
                val glow = if (!game.isOver && game.current == p && !state.busy) theme.accent else null
                drawPiece((motion.pos[p] + nudge) * c, c * 0.3f, state.settings.colors[p], state.settings.piece, motion.lift[p], glow)
            }
        }
    }
}

/** Slides in over everything when someone reaches 100, with flecks of gold drifting down. */
@Composable
private fun WinCard(state: UiState, onPlayAgain: () -> Unit, onNewGame: () -> Unit) {
    val game = state.game
    val theme = state.settings.theme
    val winner = game.winner
    AnimatedVisibility(
        visible = state.loaded && winner != null && !state.busy,
        enter = fadeIn(tween(600)),
        exit = fadeOut(tween(200)),
    ) {
        if (winner == null) return@AnimatedVisibility
        Box(Modifier.fillMaxSize().background(theme.wallEdge.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
            Confetti(listOf(theme.accent, state.settings.colors[winner].light, state.settings.colors[winner].base, theme.ink))
            val appear = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(visibleState = appear, enter = scaleIn(tween(500, delayMillis = 150), initialScale = 0.9f) + fadeIn(tween(500, delayMillis = 150))) {
                Column(
                    Modifier
                        .padding(24.dp)
                        .widthIn(max = 380.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(theme.surface)
                        .border(1.dp, theme.frame.copy(alpha = 0.6f), RoundedCornerShape(28.dp))
                        .padding(horizontal = 28.dp, vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Canvas(Modifier.size(64.dp)) {
                        drawPiece(center, size.minDimension * 0.32f, state.settings.colors[winner], state.settings.piece, glow = theme.accent)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("${state.players.name(winner)} wins", style = MaterialTheme.typography.displaySmall, color = theme.accent, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(10.dp))
                    val s = game.stats[winner]
                    Text(
                        "${s.rolls} rolls · ${s.ladders} ${if (s.ladders == 1) "ladder" else "ladders"} · ${s.snakes} ${if (s.snakes == 1) "snake" else "snakes"}",
                        style = MaterialTheme.typography.bodyMedium, color = theme.dim, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(26.dp))
                    Button(
                        onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.onAccent),
                    ) { Text("Play again", style = MaterialTheme.typography.labelLarge) }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
                        Text("New game", style = MaterialTheme.typography.labelLarge, color = theme.dim)
                    }
                }
            }
        }
    }
}

@Composable
private fun Confetti(colors: List<Color>) {
    val flecks = remember {
        val r = Random(5)
        List(70) { floatArrayOf(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f, 3f + r.nextFloat() * 5f, r.nextFloat() * 360f, r.nextInt(colors.size).toFloat()) }
    }
    val t by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "t",
    )
    Canvas(Modifier.fillMaxSize()) {
        for (f in flecks) {
            val y = ((f[1] + t * f[2]) % 1f) * (size.height + 40f) - 20f
            val x = f[0] * size.width + sin((t * f[2] * 6f + f[0]) * PI.toFloat() * 2) * 18f
            val w = f[3] * density
            rotate(f[4] + t * 720f * f[2], pivot = Offset(x, y)) {
                drawRect(colors[f[5].toInt()].copy(alpha = 0.85f), topLeft = Offset(x - w / 2, y - w / 4), size = Size(w, w / 2))
            }
        }
    }
}
