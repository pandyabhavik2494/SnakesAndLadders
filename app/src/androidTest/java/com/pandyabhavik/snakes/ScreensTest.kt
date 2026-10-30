package com.pandyabhavik.snakes

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern

/**
 * Plays the game the way a person would on the emulator and saves a screenshot at each step.
 * CI runs it at the Fold's cover-screen and inner-screen sizes; the screenshots are the proof.
 */
@RunWith(AndroidJUnit4::class)
class ScreensTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val device = UiDevice.getInstance(instrumentation)
    private val pkg = "com.pandyabhavik.snakesandladders"
    private val shots: File = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
    private val readyDie: BySelector = By.desc(Pattern.compile("Die showing \\d, tap to roll"))

    private fun shot(name: String) {
        device.waitForIdle()
        Thread.sleep(300)
        assertTrue("screenshot $name", device.takeScreenshot(File(shots, "$name.png")))
    }

    private fun launch() {
        val context = instrumentation.targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertNotNull("app window", device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 10_000))
        Thread.sleep(1500)
    }

    private fun find(selector: BySelector, what: String, timeout: Long = 8_000): UiObject2 {
        val node = device.wait(Until.findObject(selector), timeout)
        assertNotNull(what, node)
        return node!!
    }

    private fun board(): String = find(By.descStartsWith("board"), "the board").contentDescription.orEmpty()

    /** Taps the die once it's ready, then waits for the roll (and any computer roll after it) to play out. */
    private fun roll() {
        find(readyDie, "a die ready to roll", 25_000).click()
        Thread.sleep(600)
        device.wait(Until.findObject(readyDie), 25_000) ?: device.wait(Until.findObject(By.textContains("wins")), 1_000)
    }

    private fun newGame(onePlayer: Boolean, shotName: String) {
        find(By.text("New game"), "New game on the title screen").click()
        find(By.text(if (onePlayer) "1 player" else "2 players"), "player count").click()
        Thread.sleep(400)
        shot(shotName)
        val start = device.wait(Until.findObject(By.textStartsWith("Start")), 2_000)
            ?: run { device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1f); find(By.textStartsWith("Start"), "Start button") }
        start.click()
        Thread.sleep(1200)
    }

    @Test
    fun aTwoPlayerGame() {
        launch()
        shot("01-title")
        newGame(onePlayer = false, shotName = "02-setup-two-players")
        val start = board()
        assertTrue("both pieces start on 1, was: $start", start.contains("Player 1 on 1,") && start.endsWith("Player 2 on 1"))
        shot("03-fresh-board")
        roll()
        shot("04-after-first-roll")
        assertTrue("player 1 should have moved, was: ${board()}", !board().contains("Player 1 on 1,"))
        repeat(5) { roll() }
        shot("05-after-six-rolls")
    }

    @Test
    fun onePlayerAgainstTheComputer() {
        launch()
        newGame(onePlayer = true, shotName = "06-setup-one-player")
        roll()
        // The computer takes its own turn; the die only comes back once it has.
        assertTrue("the computer should be on the board: ${board()}", board().contains("Computer on"))
        repeat(3) { roll() }
        shot("07-versus-computer")
    }

    @Test
    fun studioChangesTheLook() {
        launch()
        find(By.text("Studio"), "Studio on the title screen").click()
        Thread.sleep(800)
        shot("08-studio")
        for ((i, theme) in listOf("Midnight Gold", "Rosewood", "Sakura", "Noir").withIndex()) {
            val swatch = device.wait(Until.findObject(By.desc(theme)), 3_000)
                ?: run { device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 0.6f); find(By.desc(theme), "$theme swatch") }
            swatch.click()
            Thread.sleep(700)
            shot("09-studio-${i + 1}-${theme.lowercase().replace(' ', '-')}")
        }
        find(By.text("PIECES"), "Pieces tab").click(); Thread.sleep(400); find(By.desc("Gem"), "Gem").click(); shot("10-studio-pieces")
        find(By.text("DICE"), "Dice tab").click(); Thread.sleep(400); find(By.desc("Obsidian"), "Obsidian").click(); shot("11-studio-dice")
        find(By.text("TABLE"), "Table tab").click(); Thread.sleep(400); find(By.desc("Jade Palace"), "Jade Palace").click()
        find(By.text("Done"), "Done").click()
        Thread.sleep(600)
    }

    @Test
    fun gameResumesAfterTheAppIsClosed() {
        launch()
        newGame(onePlayer = false, shotName = "12-setup")
        roll()
        val before = board()
        device.pressHome()
        Thread.sleep(600)
        launch()
        shot("13-title-with-continue")
        find(By.text("Continue game"), "Continue card").click()
        Thread.sleep(900)
        assertEquals("the game should resume as it was", before, board())
        shot("14-resumed")
    }
}
