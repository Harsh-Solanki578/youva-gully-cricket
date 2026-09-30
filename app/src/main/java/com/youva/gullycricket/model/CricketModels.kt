package com.youva.gullycricket.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Batter(
    val id: String,
    val name: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val isOut: Boolean = false,
    val dismissalInfo: String = ""
) {
    val strikeRate: Float
        get() = if (balls > 0) (runs.toFloat() / balls) * 100f else 0f
}

@JsonClass(generateAdapter = true)
data class Bowler(
    val id: String,
    val name: String,
    val oversLegalBalls: Int = 0,
    val maidens: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0
) {
    val oversFormatted: String
        get() {
            val ov = oversLegalBalls / 6
            val rem = oversLegalBalls % 6
            return "$ov.$rem"
        }

    val economyRate: Float
        get() {
            val totalOvers = oversLegalBalls / 6f
            return if (totalOvers > 0) runsConceded / totalOvers else 0f
        }
}

@JsonClass(generateAdapter = true)
data class FallOfWicket(
    val wicketNumber: Int,
    val runs: Int,
    val batterName: String,
    val overs: String
)

@JsonClass(generateAdapter = true)
data class TeamScorecard(
    val teamName: String = "Team",
    val batters: List<Batter> = emptyList(),
    val bowlers: List<Bowler> = emptyList(),
    val strikerId: String? = null,
    val nonStrikerId: String? = null,
    val currentBowlerId: String? = null,
    val extrasWides: Int = 0,
    val extrasNoBalls: Int = 0,
    val extrasByes: Int = 0,
    val fallOfWickets: List<FallOfWicket> = emptyList()
)

@JsonClass(generateAdapter = true)
data class RunRatePoint(
    val ball: Int,
    val rate: Float
)

@JsonClass(generateAdapter = true)
data class RunProgressPoint(
    val ball: Int,
    val runs: Int
)

@JsonClass(generateAdapter = true)
data class BallHistoryEntry(
    val over: Int,
    val ball: Int,
    val team: String,
    val event: String
)

@JsonClass(generateAdapter = true)
data class MatchRules(
    val isNoBallEnabled: Boolean = true,     // No ball enabled / disabled
    val noBallRuns: Int = 1,                 // 1 or 2 runs for no ball
    val isFreeHitEnabled: Boolean = true,    // Free hit awarded after no ball
    val isNoBallRebowled: Boolean = true,    // No ball must be re-bowled (doesn't count towards legal balls)
    val isWideBallEnabled: Boolean = true,   // Wide ball enabled / disabled
    val wideBallRuns: Int = 1,               // 1 or 2 runs for wide ball
    val isWideRebowled: Boolean = true,      // Wide ball must be re-bowled (doesn't count towards legal balls)
    val isUnlimitedWickets: Boolean = false, // Unlimited wickets (innings only ends on overs / target)
    val maxWickets: Int = 10,                // Maximum wickets / players before all out (if not unlimited, max 20)
    val isLastManStanding: Boolean = false,  // Last batter standing can continue batting alone
    val isUnusualRunsAllowed: Boolean = true // Unusual runs (5, 7, 8...) allowed or disallowed
)

@JsonClass(generateAdapter = true)
data class MatchStateSnapshot(
    val runs: Int,
    val wickets: Int,
    val balls: Int,
    val overs: Int,
    val currentTeam: String,
    val teamAScore: Int,
    val teamBScore: Int,
    val targetScore: Int,
    val secondInningsStarted: Boolean,
    val gameIsOver: Boolean,
    val isUmpireDecisionPending: Boolean,
    val currentOverBallsEvents: List<String>,
    val runRateData: List<RunRatePoint>,
    val teamARunProgress: List<RunProgressPoint>,
    val teamBRunProgress: List<RunProgressPoint>,
    val teamARunsPerOver: List<Int>,
    val teamBRunsPerOver: List<Int>,
    val ballHistory: List<BallHistoryEntry>,
    val isFreeHitActive: Boolean = false,
    val teamAName: String = "Team A",
    val teamBName: String = "Team B",
    val tossWinner: String = "",
    val tossDecision: String = "",
    val isSetupComplete: Boolean = false,
    val teamAScorecard: TeamScorecard = TeamScorecard("Team A"),
    val teamBScorecard: TeamScorecard = TeamScorecard("Team B"),
    val rules: MatchRules = MatchRules(),
    val matchId: String = "",
    val matchDateFormatted: String = ""
)

@JsonClass(generateAdapter = true)
data class GullyCricketGame(
    val totalOvers: Int,
    val runs: Int,
    val wickets: Int,
    val balls: Int,
    val overs: Int,
    val currentTeam: String,
    val secondInningsStarted: Boolean,
    val gameIsOver: Boolean,
    val isUmpireDecisionPending: Boolean,
    val teamAScore: Int,
    val teamBScore: Int,
    val targetScore: Int,
    val currentOverBallsEvents: List<String>,
    val runRateData: List<RunRatePoint>,
    val teamARunProgress: List<RunProgressPoint>,
    val teamBRunProgress: List<RunProgressPoint>,
    val teamARunsPerOver: List<Int>,
    val teamBRunsPerOver: List<Int>,
    val ballHistory: List<BallHistoryEntry>,
    val actionStack: List<MatchStateSnapshot>,
    val themeMode: String, // "dark" or "light"
    val isFreeHitActive: Boolean = false,
    val teamAName: String = "Team A",
    val teamBName: String = "Team B",
    val tossWinner: String = "",
    val tossDecision: String = "",
    val isSetupComplete: Boolean = false,
    val teamAScorecard: TeamScorecard = TeamScorecard("Team A"),
    val teamBScorecard: TeamScorecard = TeamScorecard("Team B"),
    val rules: MatchRules = MatchRules(),
    val matchId: String = "",
    val matchDateFormatted: String = ""
)

