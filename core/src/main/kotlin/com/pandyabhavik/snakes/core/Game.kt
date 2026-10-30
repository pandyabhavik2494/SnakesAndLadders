package com.pandyabhavik.snakes.core

/** What happens when a roll would carry a piece past 100. */
enum class FinishRule(val id: String, val label: String) {
    /** The piece stays put unless the roll lands exactly on 100. */
    EXACT("exact", "Exact roll"),
    /** The piece walks to 100 and back for the rest of the roll. */
    BOUNCE("bounce", "Bounce back"),
    /** Any roll that reaches 100 wins. */
    ANY("any", "Any roll");

    companion object {
        fun byId(id: String?): FinishRule = entries.firstOrNull { it.id == id } ?: BOUNCE
    }
}

data class Rules(val finish: FinishRule = FinishRule.BOUNCE, val sixRollsAgain: Boolean = true)

/** Per-player tallies, shown when the game ends. */
data class Stats(val rolls: Int = 0, val ladders: Int = 0, val snakes: Int = 0, val sixes: Int = 0)

/**
 * One roll, everything the screen needs to animate it: the squares the piece steps through one by one
 * ([steps], ending where it stopped, possibly after bouncing off 100), then the ladder or snake if any.
 */
data class Turn(
    val player: Int,
    val roll: Int,
    val from: Int,
    val steps: List<Int>,
    /** Where the piece stopped before any ladder or snake. */
    val landed: Int,
    /** Where it ends up. Higher than [landed] for a ladder, lower for a snake. */
    val to: Int,
    val won: Boolean,
    /** The same player rolls again (a six, with the rule on). */
    val again: Boolean,
) {
    val ladder: Boolean get() = to > landed
    val snake: Boolean get() = to < landed
    /** The roll overshot 100 under the exact-roll rule, so the piece didn't move. */
    val blocked: Boolean get() = steps.isEmpty()
}

/**
 * The whole state of a game between two players. Immutable: [play] returns the next game and the turn
 * that led to it. Pieces start on square 1.
 */
data class Game(
    val layout: Layout = Layout.CLASSIC,
    val rules: Rules = Rules(),
    val positions: List<Int> = listOf(1, 1),
    val current: Int = 0,
    val winner: Int? = null,
    val stats: List<Stats> = listOf(Stats(), Stats()),
    val lastRoll: Int? = null,
) {
    val isOver: Boolean get() = winner != null
    val rollCount: Int get() = stats.sumOf { it.rolls }

    fun play(roll: Int): Pair<Game, Turn> {
        require(roll in 1..6) { "A die shows 1 to 6" }
        check(!isOver) { "The game is over" }
        val from = positions[current]
        val steps = buildList {
            var at = from
            var dir = 1
            val overshoot = from + roll > 100
            if (overshoot && rules.finish == FinishRule.EXACT) return@buildList
            repeat(roll) {
                if (at == 100) {
                    if (rules.finish == FinishRule.ANY) return@buildList
                    dir = -1
                }
                at += dir
                add(at)
            }
        }
        val landed = steps.lastOrNull() ?: from
        val to = layout.jumpFrom(landed)
        val won = to == 100
        val again = !won && roll == 6 && rules.sixRollsAgain
        val s = stats[current]
        val newStats = s.copy(
            rolls = s.rolls + 1,
            ladders = s.ladders + if (to > landed) 1 else 0,
            snakes = s.snakes + if (to < landed) 1 else 0,
            sixes = s.sixes + if (roll == 6) 1 else 0,
        )
        val next = copy(
            positions = positions.toMutableList().also { it[current] = to },
            current = if (won || again) current else 1 - current,
            winner = if (won) current else null,
            stats = stats.toMutableList().also { it[current] = newStats },
            lastRoll = roll,
        )
        return next to Turn(current, roll, from, steps, landed, to, won, again)
    }

    /** A fresh game on the same board with the same rules. */
    fun restart(): Game = Game(layout = layout, rules = rules)

    /** One line of text: survives app restarts. */
    fun serialize(): String = listOf(
        "v1",
        layout.id,
        rules.finish.id,
        if (rules.sixRollsAgain) "1" else "0",
        positions.joinToString(","),
        current.toString(),
        winner?.toString() ?: "-",
        lastRoll?.toString() ?: "-",
        stats.joinToString(";") { "${it.rolls},${it.ladders},${it.snakes},${it.sixes}" },
    ).joinToString("|")

    companion object {
        fun deserialize(text: String): Game? = runCatching {
            val p = text.split("|")
            if (p[0] != "v1") return null
            Game(
                layout = Layout.byId(p[1]),
                rules = Rules(FinishRule.byId(p[2]), p[3] == "1"),
                positions = p[4].split(",").map { it.toInt().coerceIn(1, 100) }.also { require(it.size == 2) },
                current = p[5].toInt().coerceIn(0, 1),
                winner = p[6].toIntOrNull(),
                lastRoll = p[7].toIntOrNull(),
                stats = p[8].split(";").map { s -> s.split(",").map(String::toInt).let { Stats(it[0], it[1], it[2], it[3]) } }.also { require(it.size == 2) },
            )
        }.getOrNull()
    }
}
