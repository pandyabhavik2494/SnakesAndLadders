package com.pandyabhavik.snakes.ui

import androidx.compose.ui.graphics.Color

/**
 * A complete look for the table: the wall behind the board, the tiles, the snakes and ladders, and the
 * text and accent colours of everything around it. [light] themes get dark text.
 */
enum class Theme(
    val id: String,
    val label: String,
    val wallCentre: Color,
    val wallEdge: Color,
    val frame: Color,
    val tileA: Color,
    val tileB: Color,
    val number: Color,
    val ink: Color,
    val dim: Color,
    val accent: Color,
    val snakeLight: Color,
    val snakeDark: Color,
    val band: Color,
    val eye: Color,
    val ladderLight: Color,
    val ladderDark: Color,
    val rung: Color,
    val panel: Color,
    val light: Boolean = false,
) {
    JADE(
        "jade", "Jade Palace",
        wallCentre = Color(0xFF16312B), wallEdge = Color(0xFF081311), frame = Color(0xFFD9B26A),
        tileA = Color(0xFFF3ECDF), tileB = Color(0xFFE5D9C3), number = Color(0xFF8C7A5A),
        ink = Color(0xFFF3ECDF), dim = Color(0xFFA8B5AC), accent = Color(0xFFD9B26A),
        snakeLight = Color(0xFF3FA37C), snakeDark = Color(0xFF1D5A45), band = Color(0xFFD9B26A), eye = Color(0xFFF4E7B8),
        ladderLight = Color(0xFF8A5D38), ladderDark = Color(0xFF5B3A20), rung = Color(0xFFA8794D), panel = Color(0xFF0F211D),
    ),
    MIDNIGHT(
        "midnight", "Midnight Gold",
        wallCentre = Color(0xFF1B2240), wallEdge = Color(0xFF070912), frame = Color(0xFFD4AF37),
        tileA = Color(0xFF161C36), tileB = Color(0xFF1D2445), number = Color(0xFF7D7A62),
        ink = Color(0xFFF1EAD2), dim = Color(0xFF8F93AD), accent = Color(0xFFE2C26B),
        snakeLight = Color(0xFFC8384F), snakeDark = Color(0xFF6E1426), band = Color(0xFFE2C26B), eye = Color(0xFFF2C7A5),
        ladderLight = Color(0xFFF1D68A), ladderDark = Color(0xFFA57F2B), rung = Color(0xFFE8C766), panel = Color(0xFF0D1124),
    ),
    ROSEWOOD(
        "rosewood", "Rosewood",
        wallCentre = Color(0xFF2E1D14), wallEdge = Color(0xFF0F0906), frame = Color(0xFFE0B977),
        tileA = Color(0xFF7A4E2F), tileB = Color(0xFF6A4127), number = Color(0xFFD9B98F),
        ink = Color(0xFFF4EAD8), dim = Color(0xFFBBA58C), accent = Color(0xFFE0B977),
        snakeLight = Color(0xFF3D9A6E), snakeDark = Color(0xFF17503A), band = Color(0xFFF0D9A0), eye = Color(0xFFF0E6B0),
        ladderLight = Color(0xFFEFD391), ladderDark = Color(0xFFA47B35), rung = Color(0xFFE6C579), panel = Color(0xFF1E130D),
    ),
    SAKURA(
        "sakura", "Sakura",
        wallCentre = Color(0xFFF8EEEC), wallEdge = Color(0xFFE6CFCF), frame = Color(0xFFB98A7C),
        tileA = Color(0xFFFFFAF6), tileB = Color(0xFFF6E6E3), number = Color(0xFFB39A93),
        ink = Color(0xFF4A2F35), dim = Color(0xFF8D6F73), accent = Color(0xFFC4657F),
        snakeLight = Color(0xFFD97A98), snakeDark = Color(0xFF8F3B5A), band = Color(0xFFFFF1F4), eye = Color(0xFFFFE1E8),
        ladderLight = Color(0xFFC9B27A), ladderDark = Color(0xFF8F7643), rung = Color(0xFFD8C38C), panel = Color(0xFFF1DFDC),
        light = true,
    ),
    NOIR(
        "noir", "Noir",
        wallCentre = Color(0xFF1C1C1C), wallEdge = Color(0xFF050505), frame = Color(0xFFBDBDBD),
        tileA = Color(0xFF171717), tileB = Color(0xFF1F1F1F), number = Color(0xFF5D5D5D),
        ink = Color(0xFFF2F2F2), dim = Color(0xFF8A8A8A), accent = Color(0xFFF2F2F2),
        snakeLight = Color(0xFFE9E9E9), snakeDark = Color(0xFF8D8D8D), band = Color(0xFF2A2A2A), eye = Color(0xFFFFFFFF),
        ladderLight = Color(0xFFCFCFCF), ladderDark = Color(0xFF7A7A7A), rung = Color(0xFFBFBFBF), panel = Color(0xFF0E0E0E),
    );

    /** A raised surface (sheets, cards) that sits on this wall. */
    val surface: Color get() = if (light) Color(0xFFFBF3F1) else panel
    /** Frosted glass for the bars and pills that float over the board. */
    val glass: Color get() = if (light) Color(0xCCFFF8F6) else wallEdge.copy(alpha = 0.78f)
    /** Hairline borders. */
    val line: Color get() = if (light) Color(0x244A2F35) else Color(0x1AFFFFFF)
    /** Text on an accent-filled button. */
    val onAccent: Color get() = if (light) Color.White else Color(0xFF1B1408)

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: JADE
    }
}

/** The shape of the playing pieces. */
enum class PieceStyle(val id: String, val label: String) {
    ORB("orb", "Orb"), PAWN("pawn", "Pawn"), GEM("gem", "Gem"), COIN("coin", "Coin");

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: ORB
    }
}

/** A piece's colour: highlight, body and shadow tones of one material. */
enum class PieceColor(val id: String, val label: String, val light: Color, val base: Color, val dark: Color) {
    RUBY("ruby", "Ruby", Color(0xFFFF6B7D), Color(0xFFB3172F), Color(0xFF5C0A17)),
    SAPPHIRE("sapphire", "Sapphire", Color(0xFF7FB2FF), Color(0xFF1F56C7), Color(0xFF0B2460)),
    EMERALD("emerald", "Emerald", Color(0xFF8BF0BF), Color(0xFF128A55), Color(0xFF053D25)),
    AMBER("amber", "Amber", Color(0xFFFFE08A), Color(0xFFD68A12), Color(0xFF6B3E04)),
    AMETHYST("amethyst", "Amethyst", Color(0xFFD9A8FF), Color(0xFF7B35C2), Color(0xFF341060)),
    PEARL("pearl", "Pearl", Color(0xFFFFFFFF), Color(0xFFE2DDD3), Color(0xFF8E877A)),
    ONYX("onyx", "Onyx", Color(0xFF8A8A92), Color(0xFF2A2A30), Color(0xFF060608));

    companion object {
        fun byId(id: String?, fallback: PieceColor) = entries.firstOrNull { it.id == id } ?: fallback
    }
}

/** The die's material. */
enum class DiceStyle(val id: String, val label: String, val light: Color, val dark: Color, val pip: Color, val edge: Color) {
    IVORY("ivory", "Ivory", Color(0xFFFFFDF6), Color(0xFFE4DAC4), Color(0xFF2A2018), Color(0xFFC9BB9C)),
    OBSIDIAN("obsidian", "Obsidian", Color(0xFF3A3A40), Color(0xFF0C0C0F), Color(0xFFE7C873), Color(0xFF000000)),
    CRYSTAL("crystal", "Crystal", Color(0xDDBEE1FF), Color(0xC04678BE), Color(0xFFFFFFFF), Color(0x99FFFFFF)),
    GOLD("gold", "Gold", Color(0xFFFFE9A8), Color(0xFFB8862C), Color(0xFF3A2508), Color(0xFF8A6420)),
    JADE("jade", "Jade", Color(0xFF7FD1AD), Color(0xFF1E6B4F), Color(0xFFF6F0E1), Color(0xFF124331));

    companion object {
        fun byId(id: String?) = entries.firstOrNull { it.id == id } ?: IVORY
    }
}
