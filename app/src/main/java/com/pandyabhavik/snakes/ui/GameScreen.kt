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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandyabhavik.snakes.core.Layout
import com.pandyabhavik.snakes.core.Turn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * What the board shows right now, beyond where the pieces stand: the piece in motion (hopping, climbing
 * or sliding), the die's face and spin, a caption, and the pass-the-phone wash.
 */
class Motion {
    var player by mutableIntStateOf(-1)
    var kind by mutableStateOf(Kind.NONE)
    var from by mutableIntStateOf(1)
    var to by mutableIntStateOf(1)
    var t by mutableFloatStateOf(0f)
    var lift by mutableFloatStateOf(0f)
    var wig by mutableFloatStateOf(0f)
    var face by mutableIntStateOf(6)
    var spin by mutableFloatStateOf(0f)
    var caption by mutableStateOf<String?>(null)
    var handoff by mutableStateOf<Int?>(null)
    /** The last roll whose piece has reached its square. Until then the piece is shown where it started. */
    var landed by mutableIntStateOf(0)
    val rolling: Boolean get() = player >= 0

    enum class Kind { NONE, HOP, LADDER, SNAKE }

    /** Which still part of the board to leave out while it's being animated on top. */
    val except: String get() = when (kind) { Kind.LADDER -> "L$from"; Kind.SNAKE -> "S$from"; else -> "" }

    fun reset(face: Int) { player = -1; kind = Kind.NONE; t = 0f; lift = 0f; wig = 0f; spin = 0f; caption = null; this.face = face }
}

/** Sound and touch together, each obeying its own switch. */
class Feel(private val sounds: Sounds, private val haptics: Haptics) {
    var soundOn = true
    var hapticsOn = true
    fun play(sound: Sound?, pattern: Haptics.Pattern?, rate: Float = 1f, volume: Float = 1f) {
        if (soundOn && sound != null) sounds.play(sound, rate, volume)
        if (hapticsOn && pattern != null) haptics.play(pattern)
    }
    fun tap() = play(Sound.TAP, Haptics.Pattern.TAP, volume = 0.6f)
}

@Composable
fun GameScreen(state: UiState, viewModel: GameViewModel, feel: Feel) {
    val motion = remember { Motion() }
    val theme = state.settings.theme
    val twoPlayers = !state.players.vsPhone

    // Every roll (and every new game) runs this once: dice, hops, ladder or snake, then hand back.
    LaunchedEffect(state.turnId) {
        val turn = state.turn
        if (turn == null || !state.busy) {
            motion.reset(state.game.lastRoll ?: 6)
            return@LaunchedEffect
        }
        playTurn(state.turnId, turn, motion, feel, state.players)
        motion.reset(turn.roll)
        viewModel.turnShown(state.turnId)
    }

    val canRoll = !state.busy && !state.game.isOver && !state.phoneToRoll
    val roll = { if (canRoll) viewModel.roll() }
    ShakeToRoll(enabled = state.settings.shake && canRoll, onShake = roll)

    BoxWithConstraints(Modifier.fillMaxSize().background(theme.wallEdge)) {
        val tabletop = twoPlayers && maxWidth >= 560.dp
        // In pass-and-play on the cover screen, a wash in the next player's colour says whose turn it is.
        LaunchedEffect(state.turnId, state.busy) {
            val turn = state.turn ?: return@LaunchedEffect
            if (state.busy || tabletop || !twoPlayers || turn.won || turn.again) return@LaunchedEffect
            motion.handoff = state.game.current
            delay(1400)
            motion.handoff = null
        }
        if (tabletop) Tabletop(state, motion, roll, onStudio = { feel.tap(); viewModel.go(Screen.STUDIO) })
        else Tower(state, motion, canRoll, roll, onStudio = { feel.tap(); viewModel.go(Screen.STUDIO) })
        Handoff(state, motion)
        WinOverlay(state, onRematch = { feel.tap(); viewModel.rematch() }, onHome = { feel.tap(); viewModel.go(Screen.TITLE) })
    }
}

private suspend fun playTurn(id: Int, turn: Turn, motion: Motion, feel: Feel, players: Players) {
    val p = turn.player
    val name = players.name(p)
    motion.player = p
    motion.kind = Motion.Kind.NONE
    motion.caption = if (players.isPhone(p)) "Computer is rolling" else "Rolling"

    feel.play(Sound.SHAKE, Haptics.Pattern.SHAKE)
    tumble(motion)
    motion.face = turn.roll
    feel.play(Sound.LAND, Haptics.Pattern.LAND)
    motion.caption = "$name rolled ${turn.roll}"
    delay(320)

    if (turn.blocked) {
        motion.caption = "$name needs exactly ${100 - turn.from}"
        feel.play(Sound.BLOCKED, Haptics.Pattern.BLOCKED)
        delay(800)
    }

    var at = turn.from
    turn.steps.forEachIndexed { i, square ->
        motion.kind = Motion.Kind.HOP; motion.from = at; motion.to = square
        val hop = Animatable(0f)
        hop.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) { motion.t = value; motion.lift = sin(PI.toFloat() * value) * 0.6f }
        motion.lift = 0f
        feel.play(Sound.HOP, Haptics.Pattern.HOP, rate = 0.92f + i * 0.035f)
        at = square
        delay(45)
    }

    if (turn.ladder) {
        delay(150)
        motion.caption = "A ladder! Up to ${turn.to}"
        motion.kind = Motion.Kind.LADDER; motion.from = turn.landed; motion.to = turn.to; motion.t = 0f
        feel.play(Sound.LADDER, Haptics.Pattern.LADDER)
        val climb = Animatable(0f)
        climb.animateTo(1f, tween(950, easing = FastOutSlowInEasing)) { motion.t = value; motion.lift = 0.35f * sin(PI.toFloat() * value.coerceAtMost(0.5f)) }
        val settle = Animatable(motion.lift)
        settle.animateTo(0f, tween(160)) { motion.lift = value }
        feel.play(Sound.HOP, Haptics.Pattern.HOP, rate = 1.2f)
        delay(250)
    } else if (turn.snake) {
        delay(150)
        motion.caption = "A snake! Down to ${turn.to}"
        motion.kind = Motion.Kind.SNAKE; motion.from = turn.landed; motion.to = turn.to; motion.t = 0f
        feel.play(Sound.SNAKE, Haptics.Pattern.SNAKE)
        val slide = Animatable(0f)
        slide.animateTo(1f, tween(1300, easing = FastOutSlowInEasing)) {
            motion.t = value; motion.wig = sin(PI.toFloat() * value) * 2.2f; motion.lift = 0.15f * sin(PI.toFloat() * value)
        }
        motion.lift = 0f
        feel.play(Sound.LAND, Haptics.Pattern.HOP, rate = 0.8f, volume = 0.6f)
        delay(250)
    }
    motion.kind = Motion.Kind.NONE
    motion.player = -1
    motion.landed = id

    when {
        turn.won -> { motion.caption = "$name wins!"; feel.play(Sound.WIN, Haptics.Pattern.WIN); delay(400) }
        turn.again -> { motion.caption = "A six! $name rolls again"; feel.play(Sound.TURN, Haptics.Pattern.TURN, rate = 1.12f); delay(600) }
        else -> feel.play(Sound.TURN, Haptics.Pattern.TURN, volume = 0.7f)
    }
}

/** Spins the die for ~0.7 s, flicking through faces as it goes. */
private suspend fun tumble(motion: Motion) = coroutineScope {
    val flicker = launch {
        while (true) {
            var next = Random.nextInt(1, 7)
            if (next == motion.face) next = next % 6 + 1
            motion.face = next
            delay(75)
        }
    }
    val spin = Animatable(0f)
    spin.animateTo(1f, tween(720, easing = LinearEasing)) { motion.spin = value }
    flicker.cancel()
    motion.spin = 0f
}

// ---------------------------------------------------------------------------------------------------
// The board

/**
 * The full board, stretched to fill the space it's given. The still parts are drawn once into a bitmap
 * per size and look; the pieces, the reachable-square glow and the animated ladder or snake are drawn
 * over it every frame.
 */
@Composable
fun BoardView(
    layout: Layout,
    theme: Theme,
    modifier: Modifier = Modifier,
    positions: List<Int> = emptyList(),
    colors: List<PieceColor> = emptyList(),
    piece: PieceStyle = PieceStyle.ORB,
    motion: Motion? = null,
    current: Int = -1,
    hints: Boolean = false,
) {
    val text = rememberTextMeasurer()
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "p",
    )
    val except = remember(motion) { derivedStateOf { motion?.except.orEmpty() } }
    val still = remember { StillBoard() }
    Box(
        modifier.drawWithCache {
            val g = BoardGrid(0f, 0f, size.width, size.height)
            onDrawBehind {
                drawImage(still.get(this, g, layout, theme, text, except.value))
                drawMoving(g, layout, theme, positions, colors, piece, motion, current, hints, pulse)
            }
        },
    )
}

/**
 * The still board (tiles, ladders, snakes with their blurred shadows) rendered once into a bitmap and
 * reused until the size, layout, look or the animated part changes, so a frame only draws the pieces.
 */
private class StillBoard {
    private var key: List<Any>? = null
    private var bitmap: ImageBitmap? = null

    fun get(scope: DrawScope, g: BoardGrid, layout: Layout, theme: Theme, text: TextMeasurer, except: String): ImageBitmap {
        val k = listOf(scope.size, layout, theme, except)
        bitmap?.let { if (k == key) return it }
        val b = ImageBitmap(scope.size.width.toInt().coerceAtLeast(1), scope.size.height.toInt().coerceAtLeast(1))
        CanvasDrawScope().draw(scope, scope.layoutDirection, Canvas(b), scope.size) { drawStillBoard(g, layout, theme, text, except) }
        key = k
        bitmap = b
        return b
    }
}

private fun DrawScope.drawMoving(
    g: BoardGrid, layout: Layout, theme: Theme, positions: List<Int>, colors: List<PieceColor>, piece: PieceStyle,
    motion: Motion?, current: Int, hints: Boolean, pulse: State<Float>,
) {
    val kind = motion?.kind ?: Motion.Kind.NONE
    if (kind == Motion.Kind.LADDER) drawLadder(motion!!.from, motion.to, g, theme, glow = motion.t)
    if (kind == Motion.Kind.SNAKE) drawSnake(motion!!.from, motion.to, g, theme, wig = motion.wig)
    if (positions.size < 2) return
    val rolling = motion?.rolling == true

    // Before a roll: the squares within reach glow, a ladder's foot most of all.
    if (hints && !rolling && current in 0..1) {
        val from = positions[current]
        val a = pulse.value
        for (k in 1..6) {
            val sq = from + k
            if (sq > 100) break
            val tl = g.cellTopLeft(sq)
            val ladder = sq in layout.ladders
            val snake = sq in layout.snakes
            drawRect(theme.accent.copy(alpha = (if (ladder) 0.32f else if (snake) 0.12f else 0.16f) * a), topLeft = tl, size = Size(g.cw, g.ch))
            if (ladder) drawRect(
                theme.accent.copy(alpha = 0.9f * a), topLeft = tl + Offset(1.5f * density, 1.5f * density), size = Size(g.cw - 3 * density, g.ch - 3 * density),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f * density),
            )
        }
    }

    fun place(p: Int): Pair<Offset, Float> {
        if (motion != null && motion.player == p && kind != Motion.Kind.NONE) {
            val at = when (kind) {
                Motion.Kind.SNAKE -> g.snakeLine(motion.from, motion.to, motion.wig)(motion.t)
                else -> g.centre(motion.from) + (g.centre(motion.to) - g.centre(motion.from)) * motion.t
            }
            return at to motion.lift
        }
        val shared = positions[0] == positions[1] && !(motion?.player == 1 - p && kind != Motion.Kind.NONE)
        val nudge = if (shared) Offset(if (p == 0) -0.22f * g.m else 0.22f * g.m, 0f) else Offset.Zero
        return g.centre(positions[p]) + nudge to 0f
    }
    val order = if (motion?.player == 0) listOf(1, 0) else listOf(0, 1)
    for (p in order) {
        val (at, lift) = place(p)
        val glow = if (!rolling && current == p) theme.accent else null
        drawPiece(at, g.m * 0.3f, colors[p], piece, lift, glow)
    }
}

/**
 * Where the pieces are shown. The game already knows where a roll ends, but until the piece has walked
 * there it is shown where it started, so the move is a surprise.
 */
private fun shownPositions(state: UiState, motion: Motion): List<Int> {
    val t = state.turn
    return if (state.busy && t != null && motion.landed != state.turnId) state.game.positions.toMutableList().also { it[t.player] = t.from }
    else state.game.positions
}

@Composable
private fun GameBoard(state: UiState, motion: Motion, modifier: Modifier, onTap: () -> Unit) {
    val game = state.game
    val description = "board, ${state.players.name(0)} on ${game.positions[0]}, ${state.players.name(1)} on ${game.positions[1]}"
    BoardView(
        layout = game.layout,
        theme = state.settings.theme,
        positions = shownPositions(state, motion),
        colors = state.settings.colors,
        piece = state.settings.piece,
        motion = motion,
        current = if (game.isOver || state.busy) -1 else game.current,
        hints = state.settings.hints && !state.phoneToRoll,
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTap)
            .semantics { contentDescription = description }
            .testTag("board"),
    )
}

// ---------------------------------------------------------------------------------------------------
// Cover screen (and one player on any screen): the tower

@Composable
private fun Tower(state: UiState, motion: Motion, canRoll: Boolean, roll: () -> Unit, onStudio: () -> Unit) {
    val theme = state.settings.theme
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val top = insets.calculateTopPadding() + 62.dp
    val bottom = insets.calculateBottomPadding() + 92.dp
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(theme.wallCentre, theme.wallEdge)))) {
        GameBoard(state, motion, Modifier.fillMaxSize().padding(top = top, bottom = bottom), roll)
        // Hairlines where the board meets the bars.
        Box(Modifier.fillMaxWidth().padding(top = top).height(1.dp).background(theme.accent.copy(alpha = 0.5f)))
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(bottom = bottom).height(1.dp).background(theme.accent.copy(alpha = 0.5f)))

        Row(
            Modifier.fillMaxWidth().padding(top = insets.calculateTopPadding() + 8.dp, start = 10.dp, end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerPill(state, 0, motion, Modifier.weight(1f))
            PlayerPill(state, 1, motion, Modifier.weight(1f))
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(theme.glass).border(1.dp, theme.line, CircleShape).clickable(onClick = onStudio),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.Tune, contentDescription = "Settings", tint = theme.dim) }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bottom)
                .background(Brush.verticalGradient(0f to Color.Transparent, 0.3f to theme.wallEdge.copy(alpha = 0.9f), 1f to theme.wallEdge))
                .padding(start = 20.dp, end = 14.dp, bottom = insets.calculateBottomPadding()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Caption(state, motion, MaterialTheme.typography.headlineSmall, TextAlign.Start)
                Spacer(Modifier.height(6.dp))
                Text(hint(state, canRoll).uppercase(), style = MaterialTheme.typography.labelSmall, color = theme.dim, maxLines = 1)
            }
            Die(state, motion, ready = canRoll, dieSize = 60.dp, onRoll = roll)
        }
    }
}

private fun hint(state: UiState, canRoll: Boolean) = when {
    state.game.isOver -> ""
    state.phoneToRoll -> "The computer rolls for itself"
    canRoll && state.settings.shake -> "Tap the die or shake"
    canRoll -> "Tap the die or the board"
    else -> ""
}

@Composable
private fun PlayerPill(state: UiState, player: Int, motion: Motion, modifier: Modifier) {
    val theme = state.settings.theme
    val game = state.game
    val active = !game.isOver && game.current == player
    val border by animateColorAsState(if (active) theme.accent else theme.line, tween(350), label = "border")
    val detail = when {
        game.winner == player && !state.busy -> "Winner"
        active && motion.rolling -> "Rolling"
        active && state.players.isPhone(player) -> "Thinking"
        active -> "Your roll"
        else -> "On ${shownPositions(state, motion)[player]}"
    }
    Row(
        modifier.height(48.dp).clip(RoundedCornerShape(50)).background(theme.glass).border(if (active) 1.5.dp else 1.dp, border, RoundedCornerShape(50))
            .padding(start = 6.dp, end = 12.dp)
            .semantics { contentDescription = "${state.players.name(player)}, square ${shownPositions(state, motion)[player]}${if (active) ", to roll" else ""}" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(36.dp)) { drawPiece(center + Offset(0f, size.height * 0.05f), size.minDimension * 0.3f, state.settings.colors[player], state.settings.piece) }
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(state.players.name(player), style = MaterialTheme.typography.titleMedium, color = theme.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = if (active) theme.accent else theme.dim, maxLines = 1)
        }
    }
}

@Composable
private fun Caption(state: UiState, motion: Motion, style: androidx.compose.ui.text.TextStyle, align: TextAlign) {
    val theme = state.settings.theme
    val text = motion.caption ?: idleCaption(state)
    AnimatedContent(text, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) }, label = "caption") { line ->
        Text(line, style = style, color = theme.ink, textAlign = align, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun idleCaption(state: UiState): String {
    val game = state.game
    val players = state.players
    game.winner?.let { return "${players.name(it)} wins!" }
    val turn = state.turn
    val name = players.name(game.current)
    return when {
        turn == null && game.rollCount == 0 -> "$name goes first"
        turn != null && turn.again -> "A six! $name rolls again"
        players.isPhone(game.current) -> "Computer's turn"
        else -> "$name to roll"
    }
}

// ---------------------------------------------------------------------------------------------------
// Inner screen, two players: the tabletop

@Composable
private fun Tabletop(state: UiState, motion: Motion, roll: () -> Unit, onStudio: () -> Unit) {
    val theme = state.settings.theme
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val dock = 68.dp
    Box(Modifier.fillMaxSize().background(theme.wallEdge)) {
        GameBoard(
            state, motion,
            Modifier.fillMaxSize().padding(top = insets.calculateTopPadding() + dock, bottom = insets.calculateBottomPadding() + dock),
            roll,
        )
        // Player 2 sits across the table: their dock is turned to face them.
        Dock(state, 1, motion, roll, Modifier.align(Alignment.TopCenter).padding(top = insets.calculateTopPadding()).height(dock).graphicsLayer { rotationZ = 180f }, onStudio)
        Dock(state, 0, motion, roll, Modifier.align(Alignment.BottomCenter).padding(bottom = insets.calculateBottomPadding()).height(dock), onStudio)
    }
}

@Composable
private fun Dock(state: UiState, player: Int, motion: Motion, roll: () -> Unit, modifier: Modifier, onStudio: () -> Unit) {
    val theme = state.settings.theme
    val game = state.game
    val active = !game.isOver && game.current == player
    val line = when {
        game.winner == player && !state.busy -> "Winner"
        active && motion.caption != null -> motion.caption!!
        active -> "Your roll · tap your die"
        motion.caption != null && motion.player == 1 - player -> motion.caption!!
        else -> "On ${shownPositions(state, motion)[player]}"
    }
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(34.dp)) { drawPiece(center, size.minDimension * 0.3f, state.settings.colors[player], state.settings.piece, glow = if (active) theme.accent else null) }
        Spacer(Modifier.width(10.dp))
        Text(state.players.name(player), style = MaterialTheme.typography.titleMedium, color = theme.ink, maxLines = 1)
        Spacer(Modifier.width(12.dp))
        AnimatedContent(line, transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) }, label = "dock", modifier = Modifier.weight(1f)) {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = if (active) theme.accent else theme.dim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (player == 0) {
            Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onStudio), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Tune, contentDescription = "Settings", tint = theme.dim)
            }
            Spacer(Modifier.width(8.dp))
        }
        val mine = active && !state.busy
        Die(state, motion, ready = mine, dieSize = 52.dp, onRoll = roll, dim = !active, tag = "die${player + 1}")
    }
}

// ---------------------------------------------------------------------------------------------------
// The die, the hand-off wash and the win

/** The die: tumbles in 3D while rolling, breathes with a halo when it's waiting for a tap. */
@Composable
private fun Die(state: UiState, motion: Motion, ready: Boolean, dieSize: Dp, onRoll: () -> Unit, dim: Boolean = false, tag: String = "die") {
    val breathe by rememberInfiniteTransition(label = "breathe").animateFloat(
        initialValue = 0f, targetValue = 1f, animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    val halo by animateFloatAsState(if (ready) 1f else 0f, tween(300), label = "ready")
    val alpha by animateFloatAsState(if (dim) 0.4f else 1f, tween(300), label = "dim")
    val spin = if (dim) 0f else motion.spin
    val face = if (dim) state.game.lastRoll ?: 1 else motion.face
    val theme = state.settings.theme
    Box(
        Modifier
            .size(dieSize * 1.4f)
            .alpha(alpha)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, enabled = ready, onClick = onRoll)
            .testTag(tag)
            .semantics { contentDescription = "Die showing $face" + if (ready) ", tap to roll" else "" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val a = halo * (0.22f + 0.16f * breathe)
            drawCircle(Brush.radialGradient(listOf(theme.accent.copy(alpha = a), Color.Transparent)), radius = size.minDimension / 2)
        }
        Canvas(
            Modifier
                .size(dieSize)
                .graphicsLayer {
                    val s = 1f + sin(PI.toFloat() * spin) * 0.18f + halo * breathe * 0.03f
                    scaleX = s; scaleY = s
                    rotationZ = spin * 540f
                    rotationX = sin(spin * PI.toFloat() * 3) * 28f
                    rotationY = sin(spin * PI.toFloat() * 2) * 22f
                    translationY = -sin(PI.toFloat() * spin) * dieSize.toPx() * 0.3f
                    cameraDistance = 14f * density
                    shadowElevation = (8f + 14f * sin(PI.toFloat() * spin)) * density
                    shape = RoundedCornerShape(dieSize * 0.22f)
                },
        ) { drawDieFace(face, state.settings.dice) }
    }
}

@Composable
private fun Handoff(state: UiState, motion: Motion) {
    val who = motion.handoff
    AnimatedVisibility(visible = who != null, enter = fadeIn(tween(300)), exit = fadeOut(tween(450))) {
        val p = who ?: state.game.current
        val color = state.settings.colors[p]
        Box(
            Modifier.fillMaxSize().background(Brush.radialGradient(listOf(color.base.copy(alpha = 0.88f), color.dark.copy(alpha = 0.85f)))),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PASS THE PHONE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(10.dp))
                Text("${state.players.name(p)}, your turn", style = MaterialTheme.typography.displaySmall, color = Color.White, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun WinOverlay(state: UiState, onRematch: () -> Unit, onHome: () -> Unit) {
    val game = state.game
    val theme = state.settings.theme
    val winner = game.winner
    AnimatedVisibility(visible = winner != null && !state.busy, enter = fadeIn(tween(600)), exit = fadeOut(tween(200))) {
        if (winner == null) return@AnimatedVisibility
        val loser = 1 - winner
        Box(Modifier.fillMaxSize().background(theme.wallEdge.copy(alpha = 0.8f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}, contentAlignment = Alignment.Center) {
            val appear = remember { MutableTransitionState(false).apply { targetState = true } }
            AnimatedVisibility(visibleState = appear, enter = scaleIn(tween(600, delayMillis = 150), initialScale = 0.9f) + fadeIn(tween(600, delayMillis = 150))) {
                Column(Modifier.padding(28.dp).widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("SQUARE 100", style = MaterialTheme.typography.labelSmall, color = theme.accent)
                    Spacer(Modifier.height(14.dp))
                    Canvas(Modifier.size(84.dp)) { drawPiece(center, size.minDimension * 0.3f, state.settings.colors[winner], state.settings.piece, glow = theme.accent) }
                    Spacer(Modifier.height(10.dp))
                    Text("${state.players.name(winner)} wins", style = MaterialTheme.typography.displayMedium, color = theme.ink, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    val s = game.stats[winner]
                    Text(
                        "${s.rolls} rolls · ${s.ladders} ${if (s.ladders == 1) "ladder" else "ladders"} · ${s.snakes} ${if (s.snakes == 1) "snake" else "snakes"}",
                        style = MaterialTheme.typography.bodyMedium, color = theme.dim, textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        ScoreName(state, 0)
                        Text("${state.wins(0)} – ${state.wins(1)}", style = MaterialTheme.typography.displaySmall, color = theme.accent)
                        ScoreName(state, 1)
                    }
                    Spacer(Modifier.height(26.dp))
                    Button(
                        onClick = onRematch, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent, contentColor = theme.onAccent),
                    ) { Text("Rematch · ${state.players.name(loser)} starts", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) { Text("Home", style = MaterialTheme.typography.labelLarge, color = theme.dim) }
                }
            }
            Confetti(listOf(theme.accent, state.settings.colors[winner].light, state.settings.colors[winner].base, theme.ink))
        }
    }
}

@Composable
private fun ScoreName(state: UiState, player: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(22.dp)) { drawPiece(center, size.minDimension * 0.34f, state.settings.colors[player], state.settings.piece) }
        Spacer(Modifier.width(6.dp))
        Text(state.players.name(player), style = MaterialTheme.typography.titleMedium, color = state.settings.theme.ink, maxLines = 1)
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
