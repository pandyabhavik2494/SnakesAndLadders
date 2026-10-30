package com.pandyabhavik.snakes.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    private fun at(vararg positions: Int, rules: Rules = Rules(), current: Int = 0) =
        Game(rules = rules, positions = positions.toList(), current = current)

    @Test fun layoutsAreSound() {
        for (layout in Layout.entries) {
            val ends = layout.ladders.flatMap { listOf(it.key, it.value) } + layout.snakes.flatMap { listOf(it.key, it.value) }
            assertEquals("$layout: every square holds at most one ladder or snake end", ends.size, ends.toSet().size)
            layout.ladders.forEach { (a, b) -> assertTrue("$layout ladder $a goes up", b > a && a > 1 && b <= 100) }
            layout.snakes.forEach { (a, b) -> assertTrue("$layout snake $a goes down", b < a && b >= 1 && a < 100) }
            layout.ladders.forEach { (a, b) -> assertTrue("$layout ladder $a spans rows", Layout.columnRow(a).second != Layout.columnRow(b).second) }
            layout.snakes.forEach { (a, b) -> assertTrue("$layout snake $a spans rows", Layout.columnRow(a).second != Layout.columnRow(b).second) }
        }
    }

    @Test fun boardSnakesLeftAndRight() {
        assertEquals(0 to 0, Layout.columnRow(1))
        assertEquals(9 to 0, Layout.columnRow(10))
        assertEquals(9 to 1, Layout.columnRow(11))
        assertEquals(0 to 1, Layout.columnRow(20))
        assertEquals(0 to 9, Layout.columnRow(100))
    }

    @Test fun plainMoveStepsSquareBySquareAndPassesTheTurn() {
        val (next, turn) = at(1, 1).play(3)
        assertEquals(listOf(2, 3, 4), turn.steps)
        assertEquals(4, next.positions[0])
        assertEquals(1, next.current)
        assertFalse(turn.ladder || turn.snake)
    }

    @Test fun ladderClimbsAndSnakeSlides() {
        val (up, climb) = at(25, 1).play(3)   // 28 → 47
        assertTrue(climb.ladder)
        assertEquals(28, climb.landed)
        assertEquals(47, up.positions[0])
        assertEquals(1, up.stats[0].ladders)
        val (down, bite) = at(1, 60, current = 1).play(2)  // 62 → 19
        assertTrue(bite.snake)
        assertEquals(19, down.positions[1])
        assertEquals(1, down.stats[1].snakes)
    }

    @Test fun sixRollsAgainWhenTheRuleIsOn() {
        assertEquals(0, at(1, 1).play(6).first.current)
        assertTrue(at(1, 1).play(6).second.again)
        assertEquals(1, at(1, 1, rules = Rules(sixRollsAgain = false)).play(6).first.current)
    }

    @Test fun exactFinishStaysPutOnOvershoot() {
        val (next, turn) = at(97, 1, rules = Rules(FinishRule.EXACT)).play(5)
        assertTrue(turn.blocked)
        assertEquals(97, next.positions[0])
        assertNull(next.winner)
        assertEquals(1, next.current)
        assertEquals(0, at(97, 1, rules = Rules(FinishRule.EXACT)).play(3).first.winner)
    }

    @Test fun bounceWalksBackFromTheTop() {
        val (next, turn) = at(97, 1, rules = Rules(FinishRule.BOUNCE)).play(5)
        assertEquals(listOf(98, 99, 100, 99, 98), turn.steps)
        assertEquals(98, next.positions[0])
        // Bouncing back onto a snake's head still slides.
        val (bit, t) = at(97, 1, rules = Rules(FinishRule.BOUNCE)).play(4)
        assertEquals(99, t.landed)
        assertEquals(80, bit.positions[0])
    }

    @Test fun anyRollReachingTheTopWins() {
        val (next, turn) = at(97, 1, rules = Rules(FinishRule.ANY)).play(6)
        assertEquals(listOf(98, 99, 100), turn.steps)
        assertTrue(turn.won)
        assertEquals(0, next.winner)
        assertTrue(next.isOver)
    }

    @Test fun ladderToTheTopWins() {
        val game = Game(layout = Layout.GENTLE, positions = listOf(78, 1))
        val (next, turn) = game.play(2)  // 80 → 99, not a win
        assertFalse(turn.won)
        assertEquals(99, next.positions[0])
    }

    @Test fun savedGameRoundTrips() {
        var g = Game(layout = Layout.WILD, rules = Rules(FinishRule.EXACT, false))
        for (r in listOf(3, 6, 2, 5, 4, 1, 6, 6)) g = g.play(r).first
        assertEquals(g, Game.deserialize(g.serialize()))
        assertNull(Game.deserialize("nonsense"))
    }

    @Test fun aLongGameAlwaysEnds() {
        val random = kotlin.random.Random(7)
        for (layout in Layout.entries) for (finish in FinishRule.entries) {
            var g = Game(layout = layout, rules = Rules(finish))
            var n = 0
            while (!g.isOver && n < 10_000) { g = g.play(random.nextInt(1, 7)).first; n++ }
            assertTrue("$layout $finish finishes", g.isOver)
        }
    }
}
