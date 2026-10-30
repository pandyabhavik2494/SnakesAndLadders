package com.pandyabhavik.snakes.ui

import android.graphics.BlurMaskFilter
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
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
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The board stretched over a rectangle: 10 columns by 10 rows filling whatever space the screen gives
 * it. Cells are tall on the cover screen (a tower to climb) and close to square on the inner screen.
 * [m] is the size things are drawn at: between the cell's short and long side, so snakes, ladders and
 * pieces stay bold in tall cells without spilling out of them.
 */
class BoardGrid(val left: Float, val top: Float, val width: Float, val height: Float) {
    val cw = width / 10f
    val ch = height / 10f
    val m = min(min(cw, ch) * 0.62f + max(cw, ch) * 0.38f, min(cw, ch) * 1.3f)

    fun centre(square: Int): Offset {
        val (col, row) = Layout.columnRow(square)
        return Offset(left + (col + 0.5f) * cw, top + (9 - row + 0.5f) * ch)
    }

    fun cellTopLeft(square: Int): Offset {
        val (col, row) = Layout.columnRow(square)
        return Offset(left + col * cw, top + (9 - row) * ch)
    }

    /**
     * The centre line of a snake, head (s = 0) to tail (s = 1): a straight run with a wave across it that
     * dies away at both ends. [wig] shifts the wave, so the snake wriggles while it carries a piece.
     */
    fun snakeLine(head: Int, tail: Int, wig: Float = 0f): (Float) -> Offset {
        val h = centre(head)
        val d = centre(tail) - h
        val len = hypot(d.x, d.y)
        val n = Offset(-d.y / len, d.x / len)
        val waves = max(1, (len / (m * 2.4f)).roundToInt())
        val amp = m * min(0.55f, 0.22f + len / m * 0.04f)
        val phase = if (head % 2 == 1) 0f else PI.toFloat()
        // sin(π) is a hair below zero in Float, and a negative to a fractional power is NaN, which would
        // make Skia drop the whole body path: hence the coerceAtLeast.
        return { s ->
            val w = amp * sin(PI.toFloat() * s).coerceAtLeast(0f).pow(0.8f) * sin(2 * PI.toFloat() * waves * s + phase + wig)
            h + d * s + n * w
        }
    }
}

private fun Offset.normal(): Offset { val l = getDistance().takeIf { it > 0f } ?: 1f; return Offset(-y / l, x / l) }

/** A soft shadow: [path] filled through a blur, offset down and to the right. */
private fun DrawScope.blurredShadow(path: Path, blur: Float, offset: Offset, alpha: Float) {
    drawIntoCanvas { canvas ->
        val paint = Paint().asFrameworkPaint().apply {
            isAntiAlias = true
            color = Color.Black.copy(alpha = alpha).toArgb()
            maskFilter = BlurMaskFilter(max(1f, blur), BlurMaskFilter.Blur.NORMAL)
        }
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.translate(offset.x, offset.y)
        canvas.nativeCanvas.drawPath(path.asAndroidPath(), paint)
        canvas.nativeCanvas.restore()
    }
}

/** Tiles and numbers, edge to edge, with a little bevel on each tile and a soft vignette over the lot. */
fun DrawScope.drawTiles(g: BoardGrid, theme: Theme, text: TextMeasurer) {
    val numberStyle = TextStyle(fontFamily = Display, fontWeight = FontWeight.Medium, fontSize = (g.m * 0.24f / density / fontScale).sp, color = theme.number)
    val topStyle = numberStyle.copy(fontWeight = FontWeight.Bold, fontSize = (g.m * 0.34f / density / fontScale).sp, color = theme.accent)
    for (square in 1..100) {
        val (col, row) = Layout.columnRow(square)
        val p = g.cellTopLeft(square)
        drawRect(if ((col + row) % 2 == 0) theme.tileB else theme.tileA, topLeft = p, size = Size(g.cw + 1f, g.ch + 1f))
        drawRect(Color.White.copy(alpha = 0.05f), topLeft = p, size = Size(g.cw, density))
        drawRect(Color.Black.copy(alpha = 0.06f), topLeft = Offset(p.x, p.y + g.ch - density), size = Size(g.cw, density))
        drawText(text, square.toString(), topLeft = p + Offset(g.m * 0.1f, g.m * 0.05f), style = if (square == 100) topStyle else numberStyle)
    }
    drawRect(
        Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.05f), Color.Black.copy(alpha = 0.22f)),
            center = Offset(g.left + g.width / 2, g.top + g.height * 0.4f), radius = max(g.width, g.height) * 0.75f,
        ),
        topLeft = Offset(g.left, g.top), size = Size(g.width, g.height),
    )
}

/** A wooden ladder with a cast shadow. Rungs up to [glow] (0..1) light up, for the climb. */
fun DrawScope.drawLadder(from: Int, to: Int, g: BoardGrid, theme: Theme, glow: Float = -1f) {
    val a = g.centre(from)
    val b = g.centre(to)
    val d = b - a
    val len = d.getDistance()
    val u = d / len
    val n = Offset(-u.y, u.x)
    val m = g.m
    val w = m * 0.2f
    val s0 = a - u * (m * 0.14f)
    val s1 = b + u * (m * 0.14f)
    val shadow = Path().apply {
        for (s in listOf(-1f, 1f)) {
            val p0 = s0 + n * (w * s); val p1 = s1 + n * (w * s)
            val e = n * (m * 0.05f)
            moveTo(p0.x - e.x, p0.y - e.y); lineTo(p1.x - e.x, p1.y - e.y); lineTo(p1.x + e.x, p1.y + e.y); lineTo(p0.x + e.x, p0.y + e.y); close()
        }
    }
    blurredShadow(shadow, m * 0.06f, Offset(m * 0.07f, m * 0.09f), 0.35f)

    val rungs = max(2, (len / (m * 0.42f)).roundToInt())
    for (i in 1 until rungs) {
        val p = a + d * (i / rungs.toFloat())
        drawLine(theme.ladderDark, p - n * w, p + n * w, m * 0.085f, StrokeCap.Round)
        drawLine(theme.rung, p - n * w - Offset(0f, m * 0.014f), p + n * w - Offset(0f, m * 0.014f), m * 0.04f, StrokeCap.Round)
        if (glow >= i / rungs.toFloat()) {
            drawLine(theme.accent.copy(alpha = 0.28f), p - n * w, p + n * w, m * 0.22f, StrokeCap.Round)
            drawLine(theme.accent, p - n * w, p + n * w, m * 0.05f, StrokeCap.Round)
        }
    }
    for (s in listOf(-1f, 1f)) {
        val p0 = s0 + n * (w * s)
        val p1 = s1 + n * (w * s)
        drawLine(
            Brush.linearGradient(listOf(theme.ladderLight, theme.ladderDark), start = p0 - n * (m * 0.06f), end = p0 + n * (m * 0.06f)),
            p0, p1, m * 0.11f, StrokeCap.Round,
        )
        val sheen = Offset(m * 0.015f, m * 0.015f)
        drawLine(Color.White.copy(alpha = 0.28f), p0 - sheen, p1 - sheen, m * 0.025f, StrokeCap.Round)
    }
}

/**
 * A snake as one smooth tapered body: cast shadow, base colour, banded saddles, a fine scale texture,
 * form shading lit from the top left, an outline, then the head. [wig] makes it wriggle.
 */
fun DrawScope.drawSnake(head: Int, tail: Int, g: BoardGrid, theme: Theme, wig: Float = 0f) {
    val line = g.snakeLine(head, tail, wig)
    val count = 140
    val m = g.m
    val pts = List(count + 1) { line(it / count.toFloat()) }
    val normals = List(count + 1) { (pts[min(count, it + 1)] - pts[max(0, it - 1)]).normal() }
    val widths = List(count + 1) {
        val s = it / count.toFloat()
        val neck = 0.72f + 0.28f * min(1f, s / 0.12f)
        m * 0.17f * neck * (1 - s).pow(0.9f) + m * 0.018f
    }
    val body = Path().apply {
        for (i in 0..count) { val p = pts[i] + normals[i] * widths[i]; if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        for (i in count downTo 0) { val p = pts[i] - normals[i] * widths[i]; lineTo(p.x, p.y) }
        close()
    }
    blurredShadow(body, m * 0.06f, Offset(m * 0.07f, m * 0.1f), 0.38f)

    clipPath(body) {
        drawPath(body, theme.snakeLight)
        // Saddles across the back, each edged in the dark tone.
        var i = 10
        while (i < count - 8) {
            val j = min(count, i + 4)
            val q = Path().apply {
                val a = pts[i] + normals[i] * (widths[i] * 1.2f); val b = pts[j] + normals[j] * (widths[j] * 1.2f)
                val c = pts[j] - normals[j] * (widths[j] * 1.2f); val e = pts[i] - normals[i] * (widths[i] * 1.2f)
                moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(c.x, c.y); lineTo(e.x, e.y); close()
            }
            drawPath(q, theme.band.copy(alpha = 0.9f))
            drawPath(q, theme.snakeDark.copy(alpha = 0.5f), style = Stroke(m * 0.02f))
            i += 9
        }
        // Scales: rows of tiny overlapping rings.
        val scale = Stroke(max(0.6f * density, m * 0.008f))
        var k = 6
        while (k < count - 4) {
            for (f in floatArrayOf(-0.55f, 0f, 0.55f)) drawCircle(theme.snakeDark.copy(alpha = 0.18f), widths[k] * 0.28f, pts[k] + normals[k] * (widths[k] * f), style = scale)
            k += 2
        }
        // Form shading: dark on the side away from the light, a sheen on the near side.
        fun shade(f: Float, color: Color, width: Float) {
            val path = Path()
            for (n in 0..count) {
                val nm = normals[n]
                val side = if (nm.x * -0.6f + nm.y * -0.8f >= 0f) 1f else -1f
                val p = pts[n] + nm * (widths[n] * f * side)
                if (n == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        shade(-0.95f, theme.snakeDark.copy(alpha = 0.75f), m * 0.13f)
        shade(0.45f, Color.White.copy(alpha = 0.28f), m * 0.045f)
        shade(0.55f, Color.White.copy(alpha = 0.5f), m * 0.015f)
    }
    drawPath(body, theme.snakeDark.copy(alpha = 0.7f), style = Stroke(max(density, m * 0.012f)))
    val angle = atan2(pts[0].y - pts[3].y, pts[0].x - pts[3].x)
    drawHead(pts[0], angle, widths[0] * 1.55f, theme)
}

/** A spade-shaped head: wider at the jaw, a rounded snout, slit-pupil eyes and a forked tongue. */
private fun DrawScope.drawHead(at: Offset, angle: Float, r: Float, theme: Theme) {
    rotate(angle * 180f / PI.toFloat(), pivot = at) {
        translate(at.x, at.y) {
            val tongue = Path().apply {
                moveTo(r * 1.0f, 0f); lineTo(r * 1.45f, 0f); lineTo(r * 1.7f, -r * 0.16f)
                moveTo(r * 1.45f, 0f); lineTo(r * 1.7f, r * 0.16f)
            }
            drawPath(tongue, Color(0xFFC2304A), style = Stroke(r * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            val head = Path().apply {
                moveTo(-r * 0.55f, -r * 0.5f)
                cubicTo(-r * 0.1f, -r * 0.95f, r * 0.55f, -r * 0.85f, r * 0.95f, -r * 0.35f)
                cubicTo(r * 1.12f, -r * 0.12f, r * 1.12f, r * 0.12f, r * 0.95f, r * 0.35f)
                cubicTo(r * 0.55f, r * 0.85f, -r * 0.1f, r * 0.95f, -r * 0.55f, r * 0.5f)
                close()
            }
            blurredShadow(head, r * 0.25f, Offset(r * 0.25f, r * 0.35f), 0.35f)
            drawPath(head, Brush.radialGradient(listOf(theme.snakeLight, theme.snakeDark), center = Offset(r * 0.1f, -r * 0.35f), radius = r * 1.2f))
            drawPath(head, theme.snakeDark, style = Stroke(max(density, r * 0.06f)))
            drawOval(theme.band.copy(alpha = 0.55f), topLeft = Offset(r * 0.1f - r * 0.42f, -r * 0.22f), size = Size(r * 0.84f, r * 0.44f))
            for (s in floatArrayOf(-1f, 1f)) {
                val eye = Offset(r * 0.5f, s * r * 0.48f)
                drawOval(theme.eye, topLeft = eye - Offset(r * 0.2f, r * 0.17f), size = Size(r * 0.4f, r * 0.34f))
                drawOval(theme.snakeDark, topLeft = eye - Offset(r * 0.2f, r * 0.17f), size = Size(r * 0.4f, r * 0.34f), style = Stroke(r * 0.05f))
                drawOval(Color(0xFF0B0B0B), topLeft = eye + Offset(r * 0.02f - r * 0.045f, -r * 0.14f), size = Size(r * 0.09f, r * 0.28f))
                drawCircle(Color.White.copy(alpha = 0.85f), r * 0.04f, eye + Offset(-r * 0.04f, -r * 0.06f))
                drawCircle(Color.Black.copy(alpha = 0.5f), r * 0.035f, Offset(r * 0.92f, s * r * 0.13f))
            }
            drawOval(Color.White.copy(alpha = 0.22f), topLeft = Offset(r * 0.2f - r * 0.35f, -r * 0.3f - r * 0.1f), size = Size(r * 0.7f, r * 0.2f))
        }
    }
}

/** Everything that doesn't move: tiles, ladders and snakes, leaving out the one being animated. */
fun DrawScope.drawStillBoard(g: BoardGrid, layout: Layout, theme: Theme, text: TextMeasurer, except: String = "") {
    drawTiles(g, theme, text)
    layout.ladders.forEach { (a, b) -> if (except != "L$a") drawLadder(a, b, g, theme) }
    layout.snakes.forEach { (a, b) -> if (except != "S$a") drawSnake(a, b, g, theme) }
}

/**
 * One playing piece standing at [centre] with its shadow. [lift] (0..1) raises it off the board during
 * a hop: the piece rises, its shadow shrinks and fades. [glow] haloes the piece whose turn it is.
 */
fun DrawScope.drawPiece(centre: Offset, r: Float, color: PieceColor, style: PieceStyle, lift: Float = 0f, glow: Color? = null) {
    drawOval(
        Color.Black.copy(alpha = 0.36f - lift * 0.18f),
        topLeft = centre + Offset(r * 0.1f - r * (0.85f - lift * 0.2f), r * 0.55f - r * 0.32f),
        size = Size(r * 2 * (0.85f - lift * 0.2f), r * 0.64f),
    )
    val p = centre - Offset(0f, lift * r * 1.4f)
    if (glow != null) drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.55f), Color.Transparent), center = p, radius = r * 2.1f), r * 2.1f, p)
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

/** A die face of side [s] at the origin of the current transform, with sunk pips and a sheen. */
fun DrawScope.drawDieFace(face: Int, style: DiceStyle, s: Float = min(size.width, size.height), origin: Offset = Offset((size.width - s) / 2, (size.height - s) / 2)) {
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
