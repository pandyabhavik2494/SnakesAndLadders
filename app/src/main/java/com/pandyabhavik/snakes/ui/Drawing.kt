package com.pandyabhavik.snakes.ui

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pandyabhavik.snakes.core.Layout
import com.pandyabhavik.snakes.ui.theme.Display
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/** Centre of a square in board units: the board is 10 units wide, x to the right, y down. */
fun squareCentre(square: Int): Offset {
    val (col, row) = Layout.columnRow(square)
    return Offset(col + 0.5f, 9 - row + 0.5f)
}

/** A cubic curve in board units: the line a snake's body follows from head to tail. */
class Curve(private val p0: Offset, private val p1: Offset, private val p2: Offset, private val p3: Offset) {
    fun at(t: Float): Offset {
        val u = 1 - t
        return p0 * (u * u * u) + p1 * (3 * u * u * t) + p2 * (3 * u * t * t) + p3 * (t * t * t)
    }

    companion object {
        /** An S-shaped curve from the snake's head to its tail; odd heads sway one way, even the other. */
        fun snake(head: Int, tail: Int): Curve {
            val a = squareCentre(head)
            val b = squareCentre(tail)
            val d = b - a
            val len = hypot(d.x, d.y)
            val n = Offset(-d.y / len, d.x / len)
            val sway = (len * 0.32f).coerceIn(0.55f, 1.3f) * if (head % 2 == 1) 1f else -1f
            return Curve(a, a + d / 3f + n * sway, a + d * (2f / 3f) - n * sway, b)
        }
    }
}

/** The board itself, drawn once per theme or layout change: frame, tiles, numbers, ladders, then snakes. */
fun DrawScope.drawBoard(layout: Layout, theme: Theme, text: TextMeasurer) {
    val side = min(size.width, size.height)
    val c = side / 10f

    // A soft shadow and a framed tray under the tiles.
    drawRoundRect(Color.Black.copy(alpha = 0.28f), topLeft = Offset(-c * 0.12f, c * 0.02f), size = Size(side + c * 0.24f, side + c * 0.3f), cornerRadius = CornerRadius(c * 0.4f))
    drawRoundRect(theme.panel, topLeft = Offset(-c * 0.16f, -c * 0.16f), size = Size(side + c * 0.32f, side + c * 0.32f), cornerRadius = CornerRadius(c * 0.36f))
    drawRoundRect(theme.frame, topLeft = Offset(-c * 0.09f, -c * 0.09f), size = Size(side + c * 0.18f, side + c * 0.18f), cornerRadius = CornerRadius(c * 0.28f), style = Stroke(c * 0.025f))

    val numberStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = (c * 0.2f / density / fontScale).sp, color = theme.number)
    val topStyle = numberStyle.copy(fontWeight = FontWeight.Bold, fontSize = (c * 0.26f / density / fontScale).sp, color = theme.accent)
    val tray = Path().apply { addRoundRect(RoundRect(0f, 0f, side, side, CornerRadius(c * 0.2f))) }
    clipPath(tray) {
        for (square in 1..100) {
            val (col, row) = Layout.columnRow(square)
            val x = col * c
            val y = (9 - row) * c
            drawRect(if ((col + row) % 2 == 0) theme.tileA else theme.tileB, topLeft = Offset(x, y), size = Size(c + 0.6f, c + 0.6f))
            drawText(text, square.toString(), topLeft = Offset(x + c * 0.07f, y + c * 0.04f), style = if (square == 100) topStyle else numberStyle)
        }
    }
    // The finish square gets a small laurel of dots; light pools in the middle of the board.
    val top = squareCentre(100) * c
    for (i in 0 until 9) {
        val a = PI.toFloat() * (0.15f + 0.7f * i / 8f)
        drawCircle(theme.accent.copy(alpha = 0.75f), c * 0.035f, top + Offset(cos(a) * c * 0.26f, sin(a) * c * 0.26f) + Offset(c * 0.08f, -c * 0.02f))
    }
    drawRect(
        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Black.copy(alpha = 0.16f)), center = Offset(side * 0.45f, side * 0.4f), radius = side * 0.78f),
        size = Size(side, side),
    )

    layout.ladders.forEach { (from, to) -> drawLadder(from, to, c, theme) }
    layout.snakes.forEach { (head, tail) -> drawSnake(head, tail, c, theme) }
}

private fun DrawScope.drawLadder(from: Int, to: Int, c: Float, theme: Theme) {
    val a = squareCentre(from) * c
    val b = squareCentre(to) * c
    val d = b - a
    val len = d.getDistance()
    val u = d / len
    val n = Offset(-u.y, u.x)
    val w = c * 0.2f
    val start = a - u * (c * 0.12f)
    val end = b + u * (c * 0.12f)
    val shadow = Offset(c * 0.06f, c * 0.08f)
    for (s in listOf(-1f, 1f)) drawLine(Color.Black.copy(alpha = 0.22f), start + n * (s * w) + shadow, end + n * (s * w) + shadow, c * 0.1f, StrokeCap.Round)
    val rungs = max(2, (len / (c * 0.38f)).toInt())
    for (i in 1 until rungs) {
        val p = a + d * (i / rungs.toFloat())
        drawLine(Color.Black.copy(alpha = 0.18f), p - n * w + shadow, p + n * w + shadow, c * 0.07f)
        drawLine(theme.ladderDark, p - n * w, p + n * w, c * 0.075f)
        drawLine(theme.rung, p - n * w - Offset(0f, c * 0.012f), p + n * w - Offset(0f, c * 0.012f), c * 0.035f)
    }
    for (s in listOf(-1f, 1f)) {
        drawLine(theme.ladderDark, start + n * (s * w), end + n * (s * w), c * 0.095f, StrokeCap.Round)
        drawLine(theme.ladderLight, start + n * (s * w) - Offset(c * 0.012f, c * 0.012f), end + n * (s * w) - Offset(c * 0.012f, c * 0.012f), c * 0.05f, StrokeCap.Round)
    }
}

/** Body thickness along the snake, in board units: full just behind the head, tapering to a thin tail. */
private fun snakeWidth(t: Float): Float = 0.2f * (1 - t).pow(0.75f) * min(1f, 0.65f + t * 5f) + 0.025f

private fun DrawScope.drawSnake(head: Int, tail: Int, c: Float, theme: Theme) {
    val curve = Curve.snake(head, tail)
    val steps = 84
    val pts = List(steps + 1) { curve.at(it / steps.toFloat()) * c }
    val shadow = Offset(c * 0.07f, c * 0.09f)
    for (i in 0..steps) drawCircle(Color.Black.copy(alpha = 0.035f), snakeWidth(i / steps.toFloat()) * c, pts[i] + shadow)
    // Tail first, so each segment nearer the head overlaps the one behind it like scales.
    for (i in steps downTo 0) {
        val t = i / steps.toFloat()
        val p = pts[i]
        val r = snakeWidth(t) * c
        drawCircle(Brush.radialGradient(listOf(theme.snakeLight, theme.snakeDark), center = p + Offset(-r * 0.35f, -r * 0.4f), radius = r * 1.25f), r, p)
        if (i % 8 == 4 && t > 0.06f && t < 0.9f) {
            val q = pts[max(0, i - 1)]
            val angle = atan2(p.y - q.y, p.x - q.x) * 180f / PI.toFloat()
            rotate(angle, pivot = p) {
                val diamond = Path().apply {
                    moveTo(p.x - r * 0.55f, p.y); lineTo(p.x, p.y - r * 0.62f); lineTo(p.x + r * 0.55f, p.y); lineTo(p.x, p.y + r * 0.62f); close()
                }
                drawPath(diamond, theme.band.copy(alpha = 0.85f))
            }
        }
    }
    // A glint running along the back.
    val glint = Path()
    for (i in 3 until steps - 4) {
        val r = snakeWidth(i / steps.toFloat()) * c
        val p = pts[i] + Offset(-r * 0.3f, -r * 0.45f)
        if (i == 3) glint.moveTo(p.x, p.y) else glint.lineTo(p.x, p.y)
    }
    drawPath(glint, Color.White.copy(alpha = 0.26f), style = Stroke(c * 0.025f, cap = StrokeCap.Round))

    // The head, turned to face away from the body, with a flicker of tongue.
    val h = pts[0]
    val behind = pts[4]
    val angle = atan2(h.y - behind.y, h.x - behind.x) * 180f / PI.toFloat()
    val hr = c * 0.2f
    rotate(angle, pivot = h) {
        drawLine(Color(0xFFC2304A), h + Offset(hr * 1.05f, 0f), h + Offset(hr * 1.45f, -hr * 0.12f), c * 0.022f, StrokeCap.Round)
        drawLine(Color(0xFFC2304A), h + Offset(hr * 1.05f, 0f), h + Offset(hr * 1.45f, hr * 0.12f), c * 0.022f, StrokeCap.Round)
        drawOval(
            Brush.radialGradient(listOf(theme.snakeLight, theme.snakeDark), center = h + Offset(-hr * 0.2f, -hr * 0.3f), radius = hr * 1.3f),
            topLeft = h - Offset(hr * 1.1f, hr * 0.85f), size = Size(hr * 2.2f, hr * 1.7f),
        )
        for (s in listOf(-1f, 1f)) {
            drawCircle(theme.eye, hr * 0.2f, h + Offset(hr * 0.35f, s * hr * 0.42f))
            drawOval(Color(0xFF111111), topLeft = h + Offset(hr * 0.34f, s * hr * 0.42f - hr * 0.14f), size = Size(hr * 0.12f, hr * 0.28f))
        }
    }
}

/**
 * One playing piece standing at [centre] with its shadow on the board. [lift] (0..1) raises it off the
 * board during a hop: the piece rises and its shadow shrinks and fades.
 */
fun DrawScope.drawPiece(centre: Offset, r: Float, color: PieceColor, style: PieceStyle, lift: Float = 0f, glow: Color? = null) {
    drawOval(
        Color.Black.copy(alpha = 0.36f - lift * 0.18f),
        topLeft = centre + Offset(r * 0.1f - r * (0.85f - lift * 0.2f), r * 0.55f - r * 0.32f),
        size = Size(r * 2 * (0.85f - lift * 0.2f), r * 0.64f),
    )
    val p = centre - Offset(0f, lift * r * 1.4f)
    if (glow != null) drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), Color.Transparent), center = p, radius = r * 2f), r * 2f, p)
    val body = Brush.radialGradient(0f to color.light, 0.55f to color.base, 1f to color.dark, center = p + Offset(-r * 0.35f, -r * 0.45f), radius = r * 1.15f)
    when (style) {
        PieceStyle.ORB -> drawCircle(body, r, p)
        PieceStyle.COIN -> {
            drawOval(color.dark, topLeft = p + Offset(-r, -r * 0.65f), size = Size(r * 2, r * 1.6f))
            drawOval(body, topLeft = p + Offset(-r, -r * 0.85f), size = Size(r * 2, r * 1.6f))
            drawOval(Color.White.copy(alpha = 0.35f), topLeft = p + Offset(-r * 0.7f, -r * 0.6f), size = Size(r * 1.4f, r * 1.1f), style = Stroke(r * 0.08f))
        }
        PieceStyle.GEM -> {
            drawPath(polygon(p, r, 6), body)
            drawPath(polygon(p, r * 0.5f, 6), Color.White.copy(alpha = 0.4f), style = Stroke(r * 0.07f))
            for (i in 0 until 6) {
                val a = PI.toFloat() / 6 + i * PI.toFloat() / 3
                drawLine(Color.White.copy(alpha = 0.18f), p + Offset(cos(a), sin(a)) * (r * 0.5f), p + Offset(cos(a), sin(a)) * r, r * 0.04f)
            }
        }
        PieceStyle.PAWN -> {
            drawOval(body, topLeft = p + Offset(-r * 0.85f, r * 0.2f), size = Size(r * 1.7f, r * 0.7f))
            val neck = Path().apply {
                moveTo(p.x - r * 0.55f, p.y + r * 0.5f)
                quadraticTo(p.x - r * 0.2f, p.y - r * 0.1f, p.x - r * 0.22f, p.y - r * 0.35f)
                lineTo(p.x + r * 0.22f, p.y - r * 0.35f)
                quadraticTo(p.x + r * 0.2f, p.y - r * 0.1f, p.x + r * 0.55f, p.y + r * 0.5f)
                close()
            }
            drawPath(neck, body)
            drawCircle(body, r * 0.42f, p + Offset(0f, -r * 0.62f))
        }
    }
    val glintAt = if (style == PieceStyle.PAWN) p + Offset(-r * 0.14f, -r * 0.78f) else p + Offset(-r * 0.32f, -r * 0.42f)
    val gr = if (style == PieceStyle.PAWN) r * 0.5f else r
    rotate(-35f, pivot = glintAt) {
        drawOval(Color.White.copy(alpha = 0.55f), topLeft = glintAt - Offset(gr * 0.28f, gr * 0.16f), size = Size(gr * 0.56f, gr * 0.32f))
    }
}

private fun polygon(centre: Offset, r: Float, sides: Int) = Path().apply {
    for (i in 0 until sides) {
        val a = PI.toFloat() / 6 + i * 2 * PI.toFloat() / sides
        val p = centre + Offset(cos(a), sin(a)) * r
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

private val pipLayout = mapOf(
    1 to listOf(0 to 0),
    2 to listOf(-1 to -1, 1 to 1),
    3 to listOf(-1 to -1, 0 to 0, 1 to 1),
    4 to listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1),
    5 to listOf(-1 to -1, 1 to -1, 0 to 0, -1 to 1, 1 to 1),
    6 to listOf(-1 to -1, 1 to -1, -1 to 0, 1 to 0, -1 to 1, 1 to 1),
)

/** A die face filling the draw area, with pips sunk into it and a sheen across the top. */
fun DrawScope.drawDie(face: Int, style: DiceStyle) {
    val s = min(size.width, size.height)
    val origin = Offset((size.width - s) / 2, (size.height - s) / 2)
    translate(origin.x, origin.y) {
        drawRoundRect(Brush.linearGradient(listOf(style.light, style.dark), start = Offset.Zero, end = Offset(s, s)), size = Size(s, s), cornerRadius = CornerRadius(s * 0.22f))
        drawRoundRect(style.edge, size = Size(s, s), cornerRadius = CornerRadius(s * 0.22f), style = Stroke(s * 0.012f))
        drawRoundRect(Color.White.copy(alpha = 0.14f), topLeft = Offset(s * 0.06f, s * 0.05f), size = Size(s * 0.88f, s * 0.4f), cornerRadius = CornerRadius(s * 0.18f))
        val mid = Offset(s / 2, s / 2)
        for ((i, j) in pipLayout.getValue(face.coerceIn(1, 6))) {
            val p = mid + Offset(i * s * 0.26f, j * s * 0.26f)
            drawCircle(style.pip, s * 0.085f, p)
            drawCircle(Color.Black.copy(alpha = 0.18f), s * 0.085f, p + Offset(s * 0.008f, s * 0.012f), style = Stroke(s * 0.012f))
            drawCircle(Color.White.copy(alpha = 0.25f), s * 0.03f, p - Offset(s * 0.015f, s * 0.02f))
        }
    }
}
