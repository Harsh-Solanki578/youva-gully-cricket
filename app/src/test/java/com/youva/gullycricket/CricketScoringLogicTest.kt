package com.youva.gullycricket

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CricketScoringLogicTest {

    private lateinit var app: Application
    private lateinit var viewModel: CricketViewModel

    @Before
    fun setup() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = CricketViewModel(app)
        viewModel.resetGame()
        // Initialize with 2 overs match
        viewModel.setTotalOvers(2)
    }

    @Test
    fun testInitialState() {
        assertEquals(0, viewModel.runs.value)
        assertEquals(0, viewModel.wickets.value)
        assertEquals(0, viewModel.balls.value)
        assertEquals(0, viewModel.overs.value)
        assertFalse(viewModel.isFreeHitActive.value)
        assertNull(viewModel.activeModifier.value)
    }

    @Test
    fun testWideAlone() {
        // Press Wide button (simple tap) -> immediately scores Wide extra (1 run)
        viewModel.selectModifier("WD", isLongClick = false)

        assertEquals(1, viewModel.runs.value)
        assertEquals(0, viewModel.balls.value) // Over / ball count remains unchanged
        assertNull(viewModel.activeModifier.value)
        assertFalse(viewModel.isFreeHitActive.value)

        // Ball History records Wide
        val history = viewModel.ballHistory.value
        assertEquals(1, history.size)
        assertEquals("WD", history[0].event)
        assertEquals(0, history[0].ball) // Ball count not incremented
    }

    @Test
    fun testWidePlusRuns() {
        // Press Wide (long click to combine)
        viewModel.selectModifier("WD", isLongClick = true)
        assertEquals("WD", viewModel.activeModifier.value)
        
        // Press 3 runs button
        viewModel.addRun(3)

        // Total runs should be 1 (Wide extra) + 3 = 4
        assertEquals(4, viewModel.runs.value)
        assertEquals(0, viewModel.balls.value) // Ball count remains unchanged

        val history = viewModel.ballHistory.value
        assertEquals(1, history.size)
        assertEquals("WD+3", history[0].event)
    }

    @Test
    fun testNoBallAloneTriggersFreeHit() {
        // Press No Ball (simple tap) -> immediately scores No Ball extra (1 run) and triggers Free Hit
        viewModel.selectModifier("NB", isLongClick = false)

        assertEquals(1, viewModel.runs.value)
        assertEquals(0, viewModel.balls.value) // Ball count remains unchanged
        assertTrue(viewModel.isFreeHitActive.value) // Triggers Free Hit

        val history = viewModel.ballHistory.value
        assertEquals("NB", history[0].event)

        // Next legal delivery (dot) consumes Free Hit
        viewModel.addNoRun()
        assertEquals(1, viewModel.runs.value)
        assertEquals(1, viewModel.balls.value) // Legally incremented
        assertFalse(viewModel.isFreeHitActive.value) // Free hit consumed
    }

    @Test
    fun testNoBallPlusRunsTriggersFreeHit() {
        // Press No Ball (long click to combine)
        viewModel.selectModifier("NB", isLongClick = true)
        // Press 4 runs
        viewModel.addRun(4)

        // 1 (NB extra) + 4 runs = 5 runs total
        assertEquals(5, viewModel.runs.value)
        assertEquals(0, viewModel.balls.value)
        assertTrue(viewModel.isFreeHitActive.value)

        val history = viewModel.ballHistory.value
        assertEquals("NB+4", history[0].event)

        // Next legal ball is scored as 6 runs
        viewModel.addRun(6)
        assertEquals(11, viewModel.runs.value)
        assertEquals(1, viewModel.balls.value)
        assertFalse(viewModel.isFreeHitActive.value) // Free Hit consumed
    }

    @Test
    fun testWicketOnWideAndNoBall() {
        // Wide + Wicket (long click to combine with run out dismissal)
        viewModel.selectModifier("WD", isLongClick = true)
        val strikerId = viewModel.teamAScorecard.value.strikerId ?: ""
        viewModel.processDirectRunOut(strikerId, 0)

        assertEquals(1, viewModel.runs.value) // Still adds the extra run
        assertEquals(1, viewModel.wickets.value)
        assertEquals(0, viewModel.balls.value) // Doesn't count as legal delivery

        val history = viewModel.ballHistory.value
        assertEquals(1, history.size)
        assertTrue(history[0].event.startsWith("WD+W"))
    }

    @Test
    fun testUndoLastBallRestoresFreeHit() {
        // Score a simple No Ball (immediately active and triggers Free Hit)
        viewModel.selectModifier("NB", isLongClick = false)
        assertTrue(viewModel.isFreeHitActive.value)

        // Consume free hit
        viewModel.addNoRun()
        assertFalse(viewModel.isFreeHitActive.value)

        // Undo last ball (the dot ball that consumed the Free Hit)
        viewModel.undoLastBall()
        assertTrue(viewModel.isFreeHitActive.value) // Free hit should be restored!
    }

    @Test
    fun testUnusualRunsAndStrikeRotation() {
        val originalStrikerId = viewModel.teamAScorecard.value.strikerId
        val originalNonStrikerId = viewModel.teamAScorecard.value.nonStrikerId

        // Score unusual 5 runs ("4+1")
        viewModel.addUnusualRuns(5, "4+1")

        assertEquals(5, viewModel.runs.value)
        assertEquals(1, viewModel.balls.value)
        val history = viewModel.ballHistory.value
        assertEquals("4+1", history[0].event)

        // 5 is odd: strike should have rotated
        val updatedCard = viewModel.teamAScorecard.value
        assertEquals(originalNonStrikerId, updatedCard.strikerId)
        assertEquals(originalStrikerId, updatedCard.nonStrikerId)

        // The batter who faced gets 5 runs
        val batter = updatedCard.batters.find { it.id == originalStrikerId }
        assertNotNull(batter)
        assertEquals(5, batter!!.runs)
        assertEquals(1, batter.balls)
    }

    @Test
    fun testLastManStandingDeclarationAndDismissal() {
        val strikerId = viewModel.teamAScorecard.value.strikerId ?: ""
        // Declare striker as last man standing
        viewModel.declareLastManStanding(strikerId)

        val card = viewModel.teamAScorecard.value
        assertNotNull(card.strikerId)
        assertNull(card.nonStrikerId)

        // Now dismiss the solo striker
        viewModel.addWicket()

        // Since it's Last Man Standing with lone batter dismissed, innings must conclude or show dialog
        // Batter should be marked out
        val batter = viewModel.teamAScorecard.value.batters.find { it.id == strikerId }
        assertTrue(batter?.isOut == true)
    }

    @Test
    fun testLongMatchActionStackCappingAndStability() {
        // Set 50 overs match
        viewModel.setTotalOvers(50)

        // Simulate scoring 150 consecutive balls (25 overs)
        for (i in 1..150) {
            if (viewModel.isBowlerChangePending.value) {
                viewModel.selectOrAddBowler("Bowler ${(i / 6) % 3 + 1}")
            }
            viewModel.addRun(1)
        }

        assertEquals(150, viewModel.runs.value)
        assertEquals(25, viewModel.overs.value)
        assertEquals(0, viewModel.balls.value)

        // actionStack MUST NOT exceed 20 to prevent CursorWindow overflow
        assertTrue("actionStack size must be <= 20", viewModel.actionStack.value.size <= 20)

        // Undo should still work smoothly
        viewModel.undoLastBall()
        assertEquals(149, viewModel.runs.value)
        assertEquals(24, viewModel.overs.value)
        assertEquals(5, viewModel.balls.value)
    }
}
