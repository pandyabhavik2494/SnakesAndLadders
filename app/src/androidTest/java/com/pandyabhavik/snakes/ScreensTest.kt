package com.pandyabhavik.snakes

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
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

    private fun shot(name: String) {
        device.waitForIdle()
        assertTrue("screenshot $name", device.takeScreenshot(File(shots, "$name.png")))
    }

    private fun launch() {
        val context = instrumentation.targetContext
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        assertNotNull("app window", device.wait(Until.hasObject(By.pkg(pkg).depth(0)), 10_000))
        Thread.sleep(1200)
    }

    private fun find(selector: androidx.test.uiautomator.BySelector, what: String, timeout: Long = 8_000): UiObject2 {
        val node = device.wait(Until.findObject(selector), timeout)
        assertNotNull(what, node)
        return node!!
    }

    private fun board(): String = find(By.descStartsWith("board"), "the board").contentDescription.orEmpty()

    /** Taps the die once it's ready, then waits for the roll (and any phone roll after it) to play out. */
    private fun roll() {
        find(By.desc(Pattern.compile("Die showing \\d, tap to roll")), "a die ready to roll", 20_000).click()
        Thread.sleep(500)
        assertNotNull("the die should be ready again", device.wait(Until.findObject(By.desc(Pattern.compile("Die showing \\d, tap to roll"))), 25_000)
            ?: device.wait(Until.findObject(By.textContains("wins")), 1_000))
    }

    private fun startGame(vsPhone: Boolean) {
        device.wait(Until.findObject(By.text("Start game")), 3_000) ?: run {
            find(By.desc("New game"), "New game button").click()
            // A game in progress asks first.
            device.wait(Until.findObjects(By.text("New game")), 2_000)?.lastOrNull()?.click()
            find(By.text("Start game"), "the new game sheet")
        }
        find(By.text(if (vsPhone) "Versus phone" else "Two players"), "player choice").click()
        Thread.sleep(300)
        shot(if (vsPhone) "5-new-game-vs-phone" else "1-new-game")
        find(By.text("Start game"), "Start game").click()
        Thread.sleep(900)
    }

    @Test
    fun twoPlayersTakeTurns() {
        launch()
        startGame(vsPhone = false)
        val start = board()
        assertTrue("both pieces start on 1, was: $start", start.contains("Player 1 on 1") && start.contains("Player 2 on 1"))
        shot("2-fresh-board")
        roll()
        shot("3-after-first-roll")
        assertTrue("player 1 should have moved, was: ${board()}", !board().contains("Player 1 on 1,"))
        repeat(5) { roll() }
        shot("4-after-six-rolls")
    }

    @Test
    fun phoneRollsForItself() {
        launch()
        startGame(vsPhone = true)
        roll()
        // The phone takes its turn without a tap; the die only comes back to us once it has.
        Thread.sleep(500)
        val after = board()
        assertTrue("the phone should have moved too, was: $after", !after.contains("Phone on 1") || after.contains("Player 1 on"))
        shot("6-after-phone-turn")
    }

    @Test
    fun themesAndSettings() {
        launch()
        device.wait(Until.findObject(By.text("Start game")), 2_000)?.click()
        Thread.sleep(600)
        find(By.desc("Settings"), "Settings button").click()
        Thread.sleep(700)
        shot("7-settings")
        for ((i, theme) in listOf("Midnight Gold", "Rosewood", "Sakura", "Noir", "Jade Palace").withIndex()) {
            find(By.desc(theme), "$theme swatch").click()
            Thread.sleep(400)
            if (i < 4) {
                device.pressBack()
                Thread.sleep(700)
                shot("8-theme-${theme.lowercase().replace(' ', '-')}")
                find(By.desc("Settings"), "Settings button").click()
                Thread.sleep(700)
            }
        }
        device.pressBack()
    }

    @Test
    fun gameResumesAfterTheAppIsClosed() {
        launch()
        startGame(vsPhone = false)
        roll()
        val before = board()
        device.pressHome()
        Thread.sleep(600)
        launch()
        val after = board()
        assertTrue("the game should resume as it was: $before / $after", before == after)
        shot("9-resumed")
    }
}
