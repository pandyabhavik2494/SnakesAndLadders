package com.pandyabhavik.snakes.core

/**
 * Where the ladders and snakes sit on the 10×10 board. Squares run 1..100 from the bottom-left, left to
 * right on the bottom row, then right to left on the next, and so on (the way the board is always drawn).
 */
enum class Layout(val id: String, val label: String, val ladders: Map<Int, Int>, val snakes: Map<Int, Int>) {
    CLASSIC(
        "classic", "Classic",
        ladders = mapOf(2 to 38, 8 to 31, 21 to 42, 28 to 47, 36 to 57, 51 to 72, 71 to 91, 78 to 98),
        snakes = mapOf(16 to 6, 46 to 25, 49 to 11, 62 to 19, 74 to 53, 89 to 68, 95 to 75, 99 to 80),
    ),
    // More and longer ladders, fewer snakes: a quicker, kinder game.
    GENTLE(
        "gentle", "Gentle",
        ladders = mapOf(3 to 24, 9 to 33, 15 to 44, 27 to 58, 40 to 61, 54 to 75, 63 to 86, 72 to 93, 80 to 99),
        snakes = mapOf(31 to 12, 47 to 35, 66 to 52, 85 to 64, 97 to 83),
    ),
    // Few, long ladders and long snakes: big swings either way.
    WILD(
        "wild", "Wild",
        ladders = mapOf(4 to 56, 12 to 50, 22 to 58, 41 to 79, 63 to 95, 70 to 89),
        snakes = mapOf(98 to 8, 87 to 36, 93 to 54, 73 to 27, 61 to 18, 45 to 17, 34 to 10),
    );

    /** Where a piece that stops on [square] ends up: up a ladder, down a snake, or where it is. */
    fun jumpFrom(square: Int): Int = ladders[square] ?: snakes[square] ?: square

    companion object {
        fun byId(id: String?): Layout = entries.firstOrNull { it.id == id } ?: CLASSIC

        /** Board column (0..9, left to right) and row (0..9, bottom to top) of a square. */
        fun columnRow(square: Int): Pair<Int, Int> {
            val i = square.coerceIn(1, 100) - 1
            val row = i / 10
            val k = i % 10
            return (if (row % 2 == 0) k else 9 - k) to row
        }
    }
}
