package com.youva.gullycricket

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.youva.gullycricket.db.AppDatabase
import com.youva.gullycricket.db.CricketRepository
import com.youva.gullycricket.db.MatchHistoryEntity
import com.youva.gullycricket.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface DialogState {
    data class SetupOvers(val defaultVal: String = "10") : DialogState
    data class ConfirmReset(val message: String) : DialogState
    data class Alert(
        val title: String, 
        val message: String, 
        val isWinnerDeclaration: Boolean = false, 
        val actionText: String = "OK"
    ) : DialogState
    object UmpireDecision : DialogState
    object WicketDialog : DialogState
    data class RunOutDialog(
        val isNoBall: Boolean = false,
        val isFreeHit: Boolean = false,
        val isWide: Boolean = false
    ) : DialogState
    object ChangeBowlerDialog : DialogState
    object EditPlayersDialog : DialogState
    object FullScorecardDialog : DialogState
    data class RenamePlayerDialog(val playerId: String, val currentName: String, val isBowler: Boolean) : DialogState
    object CustomRulesDialog : DialogState
    data class UnusualRunsDialog(val baseRun: Int = 4) : DialogState
    data class LastManStandingPrompt(val batterId: String, val batterName: String) : DialogState
}

class CricketViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("gully_cricket_app_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(application)
    private val repository = CricketRepository(database.cricketDao(), database.historyDao())

    // --- Match Identification ---
    private val _matchId = MutableStateFlow("")
    val matchId: StateFlow<String> = _matchId.asStateFlow()

    private val _matchDateFormatted = MutableStateFlow("")
    val matchDateFormatted: StateFlow<String> = _matchDateFormatted.asStateFlow()

    // --- Match History Flow ---
    private val _matchHistoryList = MutableStateFlow<List<MatchHistoryEntity>>(emptyList())
    val matchHistoryList: StateFlow<List<MatchHistoryEntity>> = _matchHistoryList.asStateFlow()

    private val _hasSavedActiveMatch = MutableStateFlow(prefs.getBoolean("has_active_match", false))
    val hasSavedActiveMatch: StateFlow<Boolean> = _hasSavedActiveMatch.asStateFlow()

    // --- Custom Match Rules ---
    private val _rules = MutableStateFlow(MatchRules())
    val rules: StateFlow<MatchRules> = _rules.asStateFlow()

    fun updateRules(newRules: MatchRules) {
        val safeRules = if (newRules.isUnlimitedWickets) {
            newRules
        } else {
            newRules.copy(maxWickets = newRules.maxWickets.coerceIn(1, 20))
        }
        _rules.value = safeRules
        checkInningEnd()
        saveGameToDb()
    }

    // --- Time State ---
    private val _clockTime = MutableStateFlow("00:00:00")
    val clockTime: StateFlow<String> = _clockTime.asStateFlow()
    private var clockJob: Job? = null

    // --- Theme State (Instant synchronous access via SharedPreferences) ---
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    // --- Active Dialog State ---
    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> = _dialogState.asStateFlow()

    private val _isBowlerChangePending = MutableStateFlow(false)
    val isBowlerChangePending: StateFlow<Boolean> = _isBowlerChangePending.asStateFlow()

    val isBallInputBlocked: Boolean
        get() = _gameIsOver.value || _isUmpireDecisionPending.value || _isBowlerChangePending.value || _dialogState.value != null

    private fun createDefaultScorecard(teamName: String, prefix: String): TeamScorecard {
        val b1 = Batter(id = "${prefix}_b1", name = "Batter 1")
        val b2 = Batter(id = "${prefix}_b2", name = "Batter 2")
        val bw1 = Bowler(id = "${prefix}_bw1", name = "Bowler 1")
        return TeamScorecard(
            teamName = teamName,
            batters = listOf(b1, b2),
            bowlers = listOf(bw1),
            strikerId = b1.id,
            nonStrikerId = b2.id,
            currentBowlerId = bw1.id
        )
    }

    // --- Scorecards State ---
    private val _teamAScorecard = MutableStateFlow(createDefaultScorecard(prefs.getString("cached_team_a", "Team A") ?: "Team A", "a"))
    val teamAScorecard: StateFlow<TeamScorecard> = _teamAScorecard.asStateFlow()

    private val _teamBScorecard = MutableStateFlow(createDefaultScorecard(prefs.getString("cached_team_b", "Team B") ?: "Team B", "b"))
    val teamBScorecard: StateFlow<TeamScorecard> = _teamBScorecard.asStateFlow()

    // --- Game States ---
    private val _totalOvers = MutableStateFlow(prefs.getInt("cached_total_overs", 0))
    val totalOvers: StateFlow<Int> = _totalOvers.asStateFlow()

    private val _runs = MutableStateFlow(prefs.getInt("cached_runs", 0))
    val runs: StateFlow<Int> = _runs.asStateFlow()

    private val _wickets = MutableStateFlow(prefs.getInt("cached_wickets", 0))
    val wickets: StateFlow<Int> = _wickets.asStateFlow()

    private val _balls = MutableStateFlow(prefs.getInt("cached_balls", 0))
    val balls: StateFlow<Int> = _balls.asStateFlow()

    private val _overs = MutableStateFlow(prefs.getInt("cached_overs", 0))
    val overs: StateFlow<Int> = _overs.asStateFlow()

    private val _currentTeam = MutableStateFlow(prefs.getString("cached_current_team", "A") ?: "A")
    val currentTeam: StateFlow<String> = _currentTeam.asStateFlow()

    private val _secondInningsStarted = MutableStateFlow(prefs.getBoolean("cached_second_innings", false))
    val secondInningsStarted: StateFlow<Boolean> = _secondInningsStarted.asStateFlow()

    private val _gameIsOver = MutableStateFlow(prefs.getBoolean("cached_game_over", false))
    val gameIsOver: StateFlow<Boolean> = _gameIsOver.asStateFlow()

    private val _isUmpireDecisionPending = MutableStateFlow(false)
    val isUmpireDecisionPending: StateFlow<Boolean> = _isUmpireDecisionPending.asStateFlow()

    private val _teamAScore = MutableStateFlow(0)
    val teamAScore: StateFlow<Int> = _teamAScore.asStateFlow()

    private val _teamAOversStr = MutableStateFlow("0.0")

    private val _teamBScore = MutableStateFlow(0)
    val teamBScore: StateFlow<Int> = _teamBScore.asStateFlow()

    private val _targetScore = MutableStateFlow(prefs.getInt("cached_target_score", 0))
    val targetScore: StateFlow<Int> = _targetScore.asStateFlow()

    private val _currentOverBallsEvents = MutableStateFlow<List<String>>(emptyList())
    val currentOverBallsEvents: StateFlow<List<String>> = _currentOverBallsEvents.asStateFlow()

    private val _isFreeHitActive = MutableStateFlow(false)
    val isFreeHitActive: StateFlow<Boolean> = _isFreeHitActive.asStateFlow()

    private val _teamAName = MutableStateFlow(prefs.getString("cached_team_a", "Team A") ?: "Team A")
    val teamAName: StateFlow<String> = _teamAName.asStateFlow()

    private val _teamBName = MutableStateFlow(prefs.getString("cached_team_b", "Team B") ?: "Team B")
    val teamBName: StateFlow<String> = _teamBName.asStateFlow()

    private val _tossWinner = MutableStateFlow("")
    val tossWinner: StateFlow<String> = _tossWinner.asStateFlow()

    private val _tossDecision = MutableStateFlow("")
    val tossDecision: StateFlow<String> = _tossDecision.asStateFlow()

    private val _isSetupComplete = MutableStateFlow(false)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _activeModifier = MutableStateFlow<String?>(null)
    val activeModifier: StateFlow<String?> = _activeModifier.asStateFlow()

    private var modifierCommitJob: Job? = null

    // --- Graphs Data ---
    private val _runRateData = MutableStateFlow<List<RunRatePoint>>(emptyList())
    val runRateData: StateFlow<List<RunRatePoint>> = _runRateData.asStateFlow()

    private val _teamARunProgress = MutableStateFlow<List<RunProgressPoint>>(listOf(RunProgressPoint(0, 0)))
    val teamARunProgress: StateFlow<List<RunProgressPoint>> = _teamARunProgress.asStateFlow()

    private val _teamBRunProgress = MutableStateFlow<List<RunProgressPoint>>(listOf(RunProgressPoint(0, 0)))
    val teamBRunProgress: StateFlow<List<RunProgressPoint>> = _teamBRunProgress.asStateFlow()

    private val _teamARunsPerOver = MutableStateFlow<List<Int>>(emptyList())
    val teamARunsPerOver: StateFlow<List<Int>> = _teamARunsPerOver.asStateFlow()

    private val _teamBRunsPerOver = MutableStateFlow<List<Int>>(emptyList())
    val teamBRunsPerOver: StateFlow<List<Int>> = _teamBRunsPerOver.asStateFlow()

    // --- History & Undo ---
    private val _ballHistory = MutableStateFlow<List<BallHistoryEntry>>(emptyList())
    val ballHistory: StateFlow<List<BallHistoryEntry>> = _ballHistory.asStateFlow()

    private val _actionStack = MutableStateFlow<List<MatchStateSnapshot>>(emptyList())
    val actionStack: StateFlow<List<MatchStateSnapshot>> = _actionStack.asStateFlow()

    private var saveJob: Job? = null
    private val saveMutex = Mutex()

    init {
        startClock()
        viewModelScope.launch {
            try {
                repository.getAllHistoryFlow().catch { e ->
                    e.printStackTrace()
                }.collect { list ->
                    _matchHistoryList.value = list
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        loadSavedGame()
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            while (true) {
                _clockTime.value = sdf.format(Date())
                delay(1000)
            }
        }
    }

    fun toggleTheme() {
        val nextTheme = if (_themeMode.value == "dark") "light" else "dark"
        _themeMode.value = nextTheme
        prefs.edit().putString("theme_mode", nextTheme).apply()
        saveGameToDb()
    }

    fun showDialog(state: DialogState) {
        _dialogState.value = state
    }

    fun dismissDialog() {
        _isBowlerChangePending.value = false
        _dialogState.value = null
    }

    fun setTotalOvers(ovs: Int) {
        val calculatedOvers = ovs.coerceIn(1, 50)
        _totalOvers.value = calculatedOvers
        initializeGraphData()
        dismissDialog()
        saveGameToDb()
    }

    private fun initializeGraphData() {
        _runRateData.value = emptyList()
        _teamARunProgress.value = listOf(RunProgressPoint(0, 0))
        _teamBRunProgress.value = listOf(RunProgressPoint(0, 0))
        _teamARunsPerOver.value = emptyList()
        _teamBRunsPerOver.value = emptyList()
    }

    // --- Scorecard Helper Operations ---

    fun swapStrike() {
        if (_gameIsOver.value || _isUmpireDecisionPending.value) return
        val isTeamA = _currentTeam.value == "A"
        val batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        if (batCard.nonStrikerId == null || batCard.nonStrikerId == batCard.strikerId) return
        val updated = batCard.copy(
            strikerId = batCard.nonStrikerId,
            nonStrikerId = batCard.strikerId
        )
        if (isTeamA) _teamAScorecard.value = updated else _teamBScorecard.value = updated
        saveGameToDb()
    }

    fun updatePlayerNames(strikerName: String, nonStrikerName: String, bowlerName: String) {
        val isTeamA = _currentTeam.value == "A"
        val batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        val bowlCard = if (isTeamA) _teamBScorecard.value else _teamAScorecard.value

        val updatedBatters = batCard.batters.map {
            when (it.id) {
                batCard.strikerId -> if (strikerName.isNotBlank()) it.copy(name = strikerName) else it
                batCard.nonStrikerId -> if (nonStrikerName.isNotBlank()) it.copy(name = nonStrikerName) else it
                else -> it
            }
        }

        val updatedBowlers = bowlCard.bowlers.map {
            if (it.id == bowlCard.currentBowlerId && bowlerName.isNotBlank()) {
                it.copy(name = bowlerName)
            } else it
        }

        val newBatCard = batCard.copy(batters = updatedBatters)
        val newBowlCard = bowlCard.copy(bowlers = updatedBowlers)

        if (isTeamA) {
            _teamAScorecard.value = newBatCard
            _teamBScorecard.value = newBowlCard
        } else {
            _teamBScorecard.value = newBatCard
            _teamAScorecard.value = newBowlCard
        }
        dismissDialog()
        saveGameToDb()
    }

    fun selectOrAddBowler(bowlerName: String) {
        if (bowlerName.isBlank()) return
        val isTeamA = _currentTeam.value == "A"
        val bowlCard = if (isTeamA) _teamBScorecard.value else _teamAScorecard.value
        val bowlersList = bowlCard.bowlers.toMutableList()

        var existing = bowlersList.find { it.name.equals(bowlerName.trim(), ignoreCase = true) }
        if (existing == null) {
            existing = Bowler(id = "bw_${System.currentTimeMillis()}", name = bowlerName.trim())
            // When user adds their own bowler, remove any untouched placeholder bowler (e.g. "Bowler 1") that has not bowled yet
            bowlersList.removeAll { bw ->
                val isPlaceholder = bw.name.matches(Regex("^Bowler\\s*\\d*$", RegexOption.IGNORE_CASE))
                isPlaceholder && bw.oversLegalBalls == 0 && bw.runsConceded == 0 && bw.wickets == 0
            }
            bowlersList.add(existing)
        }

        val updated = bowlCard.copy(bowlers = bowlersList, currentBowlerId = existing.id)
        if (isTeamA) _teamBScorecard.value = updated else _teamAScorecard.value = updated
        _isBowlerChangePending.value = false
        dismissDialog()
        saveGameToDb()
    }

    private fun updatePlayerStatsForBall(
        runsOffBat: Int,
        isExtraWide: Boolean,
        isExtraNoBall: Boolean,
        extraRuns: Int
    ) {
        val isTeamA = _currentTeam.value == "A"
        var batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        var bowlCard = if (isTeamA) _teamBScorecard.value else _teamAScorecard.value

        var battersList = batCard.batters.toMutableList()
        if (battersList.isEmpty()) {
            val s = Batter(id = "b_${System.currentTimeMillis()}_1", name = "Batter 1")
            val ns = Batter(id = "b_${System.currentTimeMillis()}_2", name = "Batter 2")
            battersList.addAll(listOf(s, ns))
            batCard = batCard.copy(strikerId = s.id, nonStrikerId = ns.id)
        }

        var striker = battersList.find { it.id == batCard.strikerId }
        if (striker == null && battersList.isNotEmpty()) {
            striker = battersList.first()
            batCard = batCard.copy(strikerId = striker.id)
        }

        if (!isExtraWide && striker != null) {
            val updatedStriker = striker.copy(
                runs = striker.runs + runsOffBat,
                balls = striker.balls + 1,
                fours = striker.fours + (if (runsOffBat == 4) 1 else 0),
                sixes = striker.sixes + (if (runsOffBat == 6) 1 else 0)
            )
            val idx = battersList.indexOfFirst { it.id == striker.id }
            if (idx >= 0) battersList[idx] = updatedStriker
        }

        var bowlersList = bowlCard.bowlers.toMutableList()
        var bowler = bowlersList.find { it.id == bowlCard.currentBowlerId }
        if (bowler == null) {
            val defaultB = Bowler(id = "bw_${System.currentTimeMillis()}", name = "Bowler 1")
            bowlersList.add(defaultB)
            bowler = defaultB
            bowlCard = bowlCard.copy(currentBowlerId = defaultB.id)
        }

        val isLegalBall = when {
            isExtraNoBall -> !_rules.value.isNoBallRebowled
            isExtraWide -> !_rules.value.isWideRebowled
            else -> true
        }
        val totalRunsConcededThisBall = runsOffBat + extraRuns
        val updatedBowler = bowler.copy(
            runsConceded = bowler.runsConceded + totalRunsConcededThisBall,
            oversLegalBalls = bowler.oversLegalBalls + (if (isLegalBall) 1 else 0)
        )
        val bIdx = bowlersList.indexOfFirst { it.id == bowler.id }
        if (bIdx >= 0) bowlersList[bIdx] = updatedBowler

        var newStrikerId = batCard.strikerId
        var newNonStrikerId = batCard.nonStrikerId

        val hasPartner = newNonStrikerId != null && newNonStrikerId != newStrikerId
        if (hasPartner) {
            if (runsOffBat % 2 != 0) {
                val temp = newStrikerId
                newStrikerId = newNonStrikerId
                newNonStrikerId = temp
            }

            val isEndOfOver = isLegalBall && (_balls.value + 1 >= 6)
            if (isEndOfOver) {
                val temp = newStrikerId
                newStrikerId = newNonStrikerId
                newNonStrikerId = temp
            }
        }

        val newBatCard = batCard.copy(
            batters = battersList,
            strikerId = newStrikerId,
            nonStrikerId = newNonStrikerId,
            extrasWides = batCard.extrasWides + (if (isExtraWide) extraRuns else 0),
            extrasNoBalls = batCard.extrasNoBalls + (if (isExtraNoBall) extraRuns else 0)
        )

        val newBowlCard = bowlCard.copy(bowlers = bowlersList)

        if (isTeamA) {
            _teamAScorecard.value = newBatCard
            _teamBScorecard.value = newBowlCard
        } else {
            _teamBScorecard.value = newBatCard
            _teamAScorecard.value = newBowlCard
        }
    }

    fun processRunOutDismissal(
        outBatterId: String,
        completedRuns: Int,
        newBatterName: String,
        nextStrikerIsNewBatter: Boolean,
        isNoBall: Boolean = false,
        isWide: Boolean = false
    ) {
        if (_gameIsOver.value || _isUmpireDecisionPending.value || _isBowlerChangePending.value) return

        logAction()

        val isTeamA = _currentTeam.value == "A"
        var batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        var bowlCard = if (isTeamA) _teamBScorecard.value else _teamAScorecard.value

        val extraRuns = when {
            isNoBall -> _rules.value.noBallRuns
            isWide -> _rules.value.wideBallRuns
            else -> 0
        }
        val totalRunsThisBall = completedRuns + extraRuns
        _runs.value += totalRunsThisBall
        _wickets.value += 1

        val battersList = batCard.batters.toMutableList()
        val outBatter = battersList.find { it.id == outBatterId }
        val isStrikerOut = outBatterId == batCard.strikerId
        val remainingBatterId = if (isStrikerOut) batCard.nonStrikerId else batCard.strikerId

        val striker = battersList.find { it.id == batCard.strikerId }

        // Update striker runs and balls faced
        if (striker != null) {
            val sRuns = striker.runs + completedRuns
            val sBalls = striker.balls + (if (!isWide) 1 else 0)
            val sFours = striker.fours + (if (completedRuns == 4) 1 else 0)
            val sSixes = striker.sixes + (if (completedRuns == 6) 1 else 0)
            val updatedStriker = striker.copy(
                runs = sRuns,
                balls = sBalls,
                fours = sFours,
                sixes = sSixes,
                isOut = if (isStrikerOut) true else striker.isOut,
                dismissalInfo = if (isStrikerOut) {
                    if (completedRuns > 0) "run out ($completedRuns runs)" else "run out"
                } else striker.dismissalInfo
            )
            val sIdx = battersList.indexOfFirst { it.id == striker.id }
            if (sIdx >= 0) battersList[sIdx] = updatedStriker
        }

        // If non-striker was out
        if (!isStrikerOut && outBatter != null) {
            val updatedNonStriker = outBatter.copy(
                isOut = true,
                dismissalInfo = if (completedRuns > 0) "run out ($completedRuns runs)" else "run out"
            )
            val nsIdx = battersList.indexOfFirst { it.id == outBatter.id }
            if (nsIdx >= 0) battersList[nsIdx] = updatedNonStriker
        }

        val isLegalBall = when {
            isNoBall -> !_rules.value.isNoBallRebowled
            isWide -> !_rules.value.isWideRebowled
            else -> true
        }

        val bowlersList = bowlCard.bowlers.toMutableList()
        val bowler = bowlersList.find { it.id == bowlCard.currentBowlerId }
        if (bowler != null) {
            val updatedBowler = bowler.copy(
                runsConceded = bowler.runsConceded + totalRunsThisBall,
                wickets = bowler.wickets, // Bowler gets NO wicket on run out
                oversLegalBalls = bowler.oversLegalBalls + (if (isLegalBall) 1 else 0)
            )
            val bIdx = bowlersList.indexOfFirst { it.id == bowler.id }
            if (bIdx >= 0) bowlersList[bIdx] = updatedBowler
        }

        val currentOversStr = "${_overs.value}.${_balls.value + (if (isLegalBall) 1 else 0)}"
        val newFow = FallOfWicket(
            wicketNumber = _wickets.value,
            runs = _runs.value,
            batterName = outBatter?.name ?: "Batter",
            overs = currentOversStr
        )
        val fowList = batCard.fallOfWickets + newFow

        val isLoneBatterOut = (batCard.nonStrikerId == null) && (remainingBatterId == null || outBatterId == batCard.strikerId)
        val isAllOut = (!_rules.value.isUnlimitedWickets && (_wickets.value >= _rules.value.maxWickets)) || isLoneBatterOut
        val isLastManRule = _rules.value.isLastManStanding
        val isLastManNow = isLastManRule && !_rules.value.isUnlimitedWickets && (_wickets.value == _rules.value.maxWickets - 1)

        val updatedBatCard = if (isAllOut) {
            // Team is All Out, no new batter enters
            batCard.copy(
                batters = battersList,
                strikerId = null,
                nonStrikerId = null,
                fallOfWickets = fowList,
                extrasWides = batCard.extrasWides + (if (isWide) extraRuns else 0),
                extrasNoBalls = batCard.extrasNoBalls + (if (isNoBall) extraRuns else 0)
            )
        } else if (isLastManNow) {
            // Sole batter standing plays alone
            batCard.copy(
                batters = battersList,
                strikerId = remainingBatterId,
                nonStrikerId = null,
                fallOfWickets = fowList,
                extrasWides = batCard.extrasWides + (if (isWide) extraRuns else 0),
                extrasNoBalls = batCard.extrasNoBalls + (if (isNoBall) extraRuns else 0)
            )
        } else {
            val newBNameClean = if (newBatterName.isBlank()) "Batter ${battersList.size + 1}" else newBatterName.trim()
            val newBatter = Batter(
                id = "b_${System.currentTimeMillis()}",
                name = newBNameClean
            )
            battersList.add(newBatter)

            val (initialStrikerId, initialNonStrikerId) = if (nextStrikerIsNewBatter) {
                Pair(newBatter.id, remainingBatterId)
            } else {
                Pair(remainingBatterId, newBatter.id)
            }

            val (midStrikerId, midNonStrikerId) = if (completedRuns % 2 != 0) {
                Pair(initialNonStrikerId, initialStrikerId)
            } else {
                Pair(initialStrikerId, initialNonStrikerId)
            }

            val isEndOfOver = isLegalBall && (_balls.value + 1 >= 6)
            val (endStrikerId, endNonStrikerId) = if (isEndOfOver) {
                Pair(midNonStrikerId, midStrikerId)
            } else {
                Pair(midStrikerId, midNonStrikerId)
            }

            batCard.copy(
                batters = battersList,
                strikerId = endStrikerId,
                nonStrikerId = endNonStrikerId,
                fallOfWickets = fowList,
                extrasWides = batCard.extrasWides + (if (isWide) extraRuns else 0),
                extrasNoBalls = batCard.extrasNoBalls + (if (isNoBall) extraRuns else 0)
            )
        }

        val updatedBowlCard = bowlCard.copy(bowlers = bowlersList)

        if (isTeamA) {
            _teamAScorecard.value = updatedBatCard
            _teamBScorecard.value = updatedBowlCard
        } else {
            _teamBScorecard.value = updatedBatCard
            _teamAScorecard.value = updatedBowlCard
        }

        val ballEvent = when {
            isNoBall -> if (completedRuns > 0) "NB+${completedRuns}+W (Run Out)" else "NB+W (Run Out)"
            isWide -> if (completedRuns > 0) "WD+${completedRuns}+W (Run Out)" else "WD+W (Run Out)"
            completedRuns > 0 -> "${completedRuns}+W (Run Out)"
            else -> "W (Run Out)"
        }

        if (isNoBall && _rules.value.isFreeHitEnabled) {
            _isFreeHitActive.value = true
        } else if (_isFreeHitActive.value && isLegalBall) {
            _isFreeHitActive.value = false
        }

        _activeModifier.value = null
        modifierCommitJob?.cancel()

        addBall(ballEvent)

        if (_dialogState.value !is DialogState.ChangeBowlerDialog) {
            dismissDialog()
        }
    }

    fun processWicketDismissal(
        dismissalType: String,
        completedRuns: Int,
        outBatterId: String,
        newBatterName: String,
        nextStrikerIsNewBatter: Boolean
    ) {
        if (_gameIsOver.value || _isUmpireDecisionPending.value || _isBowlerChangePending.value) return

        if (dismissalType == "Run Out") {
            processRunOutDismissal(
                outBatterId = outBatterId,
                completedRuns = completedRuns,
                newBatterName = newBatterName,
                nextStrikerIsNewBatter = nextStrikerIsNewBatter,
                isNoBall = false,
                isWide = false
            )
            return
        }

        logAction()

        val isTeamA = _currentTeam.value == "A"
        var batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        var bowlCard = if (isTeamA) _teamBScorecard.value else _teamAScorecard.value

        if (completedRuns > 0) {
            _runs.value += completedRuns
        }
        _wickets.value += 1

        val battersList = batCard.batters.toMutableList()
        val outBatter = battersList.find { it.id == outBatterId }
        val remainingBatterId = if (outBatterId == batCard.strikerId) batCard.nonStrikerId else batCard.strikerId

        val bowler = bowlCard.bowlers.find { it.id == bowlCard.currentBowlerId }
        val bowlerName = bowler?.name ?: "Bowler"

        val dismissalInfoText = when (dismissalType) {
            "Bowled" -> "b $bowlerName"
            "Caught" -> "caught"
            "LBW" -> "lbw b $bowlerName"
            "Stumped" -> "st b $bowlerName"
            "Hit Wicket" -> "hit wicket b $bowlerName"
            else -> dismissalType.lowercase()
        }

        if (outBatter != null) {
            val updatedOutBatter = outBatter.copy(
                runs = outBatter.runs + completedRuns,
                balls = outBatter.balls + 1,
                isOut = true,
                dismissalInfo = dismissalInfoText
            )
            val idx = battersList.indexOfFirst { it.id == outBatterId }
            if (idx >= 0) battersList[idx] = updatedOutBatter
        }

        val isLoneBatterOut = (batCard.nonStrikerId == null) && (remainingBatterId == null || outBatterId == batCard.strikerId)
        val isAllOut = (!_rules.value.isUnlimitedWickets && (_wickets.value >= _rules.value.maxWickets)) || isLoneBatterOut
        val isLastManRule = _rules.value.isLastManStanding
        val isLastManNow = isLastManRule && !_rules.value.isUnlimitedWickets && (_wickets.value == _rules.value.maxWickets - 1)

        val bowlersList = bowlCard.bowlers.toMutableList()
        if (bowler != null) {
            val isBowlerWicket = dismissalType != "Timed Out" && dismissalType != "Obstructing Field"
            val updatedBowler = bowler.copy(
                runsConceded = bowler.runsConceded + completedRuns,
                wickets = bowler.wickets + (if (isBowlerWicket) 1 else 0),
                oversLegalBalls = bowler.oversLegalBalls + 1
            )
            val bIdx = bowlersList.indexOfFirst { it.id == bowler.id }
            if (bIdx >= 0) bowlersList[bIdx] = updatedBowler
        }

        val currentOversStr = "${_overs.value}.${_balls.value + 1}"
        val newFow = FallOfWicket(
            wicketNumber = _wickets.value,
            runs = _runs.value,
            batterName = outBatter?.name ?: "Batter",
            overs = currentOversStr
        )
        val fowList = batCard.fallOfWickets + newFow

        val updatedBatCard = if (isAllOut) {
            // Team is All Out, no new batter enters
            batCard.copy(
                batters = battersList,
                strikerId = null,
                nonStrikerId = null,
                fallOfWickets = fowList
            )
        } else if (isLastManNow) {
            // Sole batter standing plays alone
            batCard.copy(
                batters = battersList,
                strikerId = remainingBatterId,
                nonStrikerId = null,
                fallOfWickets = fowList
            )
        } else {
            val newBNameClean = if (newBatterName.isBlank()) "Batter ${battersList.size + 1}" else newBatterName.trim()
            val newBatter = Batter(
                id = "b_${System.currentTimeMillis()}",
                name = newBNameClean
            )
            battersList.add(newBatter)

            val (initialStrikerId, initialNonStrikerId) = if (nextStrikerIsNewBatter) {
                Pair(newBatter.id, remainingBatterId)
            } else {
                Pair(remainingBatterId, newBatter.id)
            }

            val (midStrikerId, midNonStrikerId) = if (completedRuns % 2 != 0) {
                Pair(initialNonStrikerId, initialStrikerId)
            } else {
                Pair(initialStrikerId, initialNonStrikerId)
            }

            val isEndOfOver = (_balls.value + 1 >= 6)
            val (endStrikerId, endNonStrikerId) = if (isEndOfOver) {
                Pair(midNonStrikerId, midStrikerId)
            } else {
                Pair(midStrikerId, midNonStrikerId)
            }

            batCard.copy(
                batters = battersList,
                strikerId = endStrikerId,
                nonStrikerId = endNonStrikerId,
                fallOfWickets = fowList
            )
        }

        val updatedBowlCard = bowlCard.copy(bowlers = bowlersList)

        if (isTeamA) {
            _teamAScorecard.value = updatedBatCard
            _teamBScorecard.value = updatedBowlCard
        } else {
            _teamBScorecard.value = updatedBatCard
            _teamAScorecard.value = updatedBowlCard
        }

        val ballEvent = "W"

        val wasFreeHit = _isFreeHitActive.value
        if (wasFreeHit) _isFreeHitActive.value = false

        _activeModifier.value = null
        modifierCommitJob?.cancel()

        addBall(ballEvent)

        if (_dialogState.value !is DialogState.ChangeBowlerDialog) {
            dismissDialog()
        }
    }

    // --- Scoring Actions ---

    fun getRunsFromEvent(event: String): Int {
        if (event == "•") return 0
        if (event == "W" || event.endsWith("W")) return 0
        if (event.startsWith("WD")) {
            val base = _rules.value.wideBallRuns
            if (event == "WD") return base
            val extraRuns = event.removePrefix("WD+").split("+").sumOf { it.toIntOrNull() ?: 0 }
            return base + extraRuns
        }
        if (event.startsWith("NB")) {
            val base = _rules.value.noBallRuns
            if (event == "NB") return base
            val extraRuns = event.removePrefix("NB+").split("+").sumOf { it.toIntOrNull() ?: 0 }
            return base + extraRuns
        }
        if (event.contains("+")) {
            return event.split("+").sumOf { it.toIntOrNull() ?: 0 }
        }
        return event.toIntOrNull() ?: 0
    }

    fun selectModifier(mod: String, isLongClick: Boolean = false) {
        if (isBallInputBlocked) return

        if (!isLongClick) {
            // Simple tap commits immediately as single extra delivery (e.g. WD or NB alone)
            commitActiveModifier(mod)
        } else {
            // Long click toggles modifier active for combining with runs (0-6) or Wicket
            _activeModifier.value = if (_activeModifier.value == mod) null else mod
        }
    }

    fun clearActiveModifier() {
        _activeModifier.value = null
    }

    fun commitActiveModifier(mod: String) {
        if (isBallInputBlocked) return
        _activeModifier.value = null
        logAction()
        val isNB = mod == "NB"
        val extraRuns = if (isNB) _rules.value.noBallRuns else _rules.value.wideBallRuns
        _runs.value += extraRuns
        if (isNB && _rules.value.isFreeHitEnabled) {
            _isFreeHitActive.value = true
        }
        updatePlayerStatsForBall(runsOffBat = 0, isExtraWide = (mod == "WD"), isExtraNoBall = isNB, extraRuns = extraRuns)
        addBall(mod)
    }

    fun commitActiveModifierIfNeeded() {
        val active = _activeModifier.value ?: return
        commitActiveModifier(active)
    }

    fun addRun(r: Int) {
        if (isBallInputBlocked) return
        val active = _activeModifier.value
        if (active != null) {
            _activeModifier.value = null
            logAction()
            val isNB = active == "NB"
            val baseExtra = if (isNB) _rules.value.noBallRuns else _rules.value.wideBallRuns
            val totalRuns = baseExtra + r
            _runs.value += totalRuns
            if (isNB && _rules.value.isFreeHitEnabled) {
                _isFreeHitActive.value = true
            }
            updatePlayerStatsForBall(runsOffBat = r, isExtraWide = (active == "WD"), isExtraNoBall = isNB, extraRuns = baseExtra)
            addBall("$active+$r")
            return
        }

        logAction()
        _runs.value += r
        val wasFreeHit = _isFreeHitActive.value
        if (wasFreeHit) {
            _isFreeHitActive.value = false
        }
        updatePlayerStatsForBall(runsOffBat = r, isExtraWide = false, isExtraNoBall = false, extraRuns = 0)
        addBall(r.toString())
    }

    fun addUnusualRuns(runs: Int, eventLabel: String) {
        if (_gameIsOver.value || _isUmpireDecisionPending.value || _isBowlerChangePending.value) return
        if (!_rules.value.isUnusualRunsAllowed) {
            showDialog(DialogState.Alert("Rule Restriction", "Unusual runs are disallowed in custom match rules."))
            return
        }

        val active = _activeModifier.value
        if (active != null) {
            _activeModifier.value = null
            logAction()
            val isNB = active == "NB"
            val baseExtra = if (isNB) _rules.value.noBallRuns else _rules.value.wideBallRuns
            val totalRuns = baseExtra + runs
            _runs.value += totalRuns
            if (isNB && _rules.value.isFreeHitEnabled) {
                _isFreeHitActive.value = true
            }
            updatePlayerStatsForBall(runsOffBat = runs, isExtraWide = (active == "WD"), isExtraNoBall = isNB, extraRuns = baseExtra)
            addBall("$active+$eventLabel")
            dismissDialog()
            return
        }

        logAction()
        _runs.value += runs
        val wasFreeHit = _isFreeHitActive.value
        if (wasFreeHit) {
            _isFreeHitActive.value = false
        }
        updatePlayerStatsForBall(runsOffBat = runs, isExtraWide = false, isExtraNoBall = false, extraRuns = 0)
        addBall(eventLabel)
        dismissDialog()
    }

    fun declareLastManStanding(batterId: String) {
        if (_gameIsOver.value || _isUmpireDecisionPending.value) return
        logAction()

        val isTeamA = _currentTeam.value == "A"
        val batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        val battersList = batCard.batters.toMutableList()

        val selectedBatter = battersList.find { it.id == batterId } ?: return

        val otherBatterId = if (batCard.strikerId == batterId) batCard.nonStrikerId else batCard.strikerId
        val otherBatter = battersList.find { it.id == otherBatterId }
        val fowList = batCard.fallOfWickets.toMutableList()

        if (otherBatter != null && !otherBatter.isOut) {
            _wickets.value += 1
            val updatedOther = otherBatter.copy(
                isOut = true,
                dismissalInfo = "All others out"
            )
            val oIdx = battersList.indexOfFirst { it.id == otherBatter.id }
            if (oIdx >= 0) battersList[oIdx] = updatedOther

            val currentOversStr = "${_overs.value}.${_balls.value}"
            fowList.add(
                FallOfWicket(
                    wicketNumber = _wickets.value,
                    runs = _runs.value,
                    batterName = otherBatter.name,
                    overs = currentOversStr
                )
            )
        }

        val updatedBatCard = batCard.copy(
            batters = battersList,
            strikerId = batterId,
            nonStrikerId = null,
            fallOfWickets = fowList
        )

        if (isTeamA) {
            _teamAScorecard.value = updatedBatCard
        } else {
            _teamBScorecard.value = updatedBatCard
        }

        dismissDialog()
        saveGameToDb()
    }

    fun renameSinglePlayer(playerId: String, newName: String, isBowler: Boolean) {
        if (newName.isBlank()) return
        val isTeamA = _currentTeam.value == "A"
        val batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        val bowlCard = if (isTeamA) _teamBScorecard.value else _teamAScorecard.value

        if (isBowler) {
            val updatedBowlers = bowlCard.bowlers.map {
                if (it.id == playerId) it.copy(name = newName.trim()) else it
            }
            val newBowlCard = bowlCard.copy(bowlers = updatedBowlers)
            if (isTeamA) _teamBScorecard.value = newBowlCard else _teamAScorecard.value = newBowlCard
        } else {
            val updatedBatters = batCard.batters.map {
                if (it.id == playerId) it.copy(name = newName.trim()) else it
            }
            val newBatCard = batCard.copy(batters = updatedBatters)
            if (isTeamA) _teamAScorecard.value = newBatCard else _teamBScorecard.value = newBatCard
        }
        dismissDialog()
        saveGameToDb()
    }

    fun processDirectWicket() {
        if (_gameIsOver.value || _isUmpireDecisionPending.value) return
        val isTeamA = _currentTeam.value == "A"
        val batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        val strikerId = batCard.strikerId ?: batCard.batters.firstOrNull()?.id ?: return

        processWicketDismissal(
            dismissalType = "Bowled",
            completedRuns = 0,
            outBatterId = strikerId,
            newBatterName = "",
            nextStrikerIsNewBatter = true
        )
    }

    fun processDirectRunOut(outBatterId: String, completedRuns: Int) {
        if (_gameIsOver.value || _isUmpireDecisionPending.value) return
        val active = _activeModifier.value
        processRunOutDismissal(
            outBatterId = outBatterId,
            completedRuns = completedRuns,
            newBatterName = "",
            nextStrikerIsNewBatter = true,
            isNoBall = (active == "NB"),
            isWide = (active == "WD")
        )
    }

    fun addWicket() {
        if (isBallInputBlocked) return
        val active = _activeModifier.value
        if (active == "NB" || _isFreeHitActive.value) {
            showDialog(DialogState.RunOutDialog(
                isNoBall = (active == "NB"),
                isFreeHit = _isFreeHitActive.value,
                isWide = (active == "WD")
            ))
            return
        }
        if (active == "WD") {
            showDialog(DialogState.RunOutDialog(
                isNoBall = false,
                isFreeHit = _isFreeHitActive.value,
                isWide = true
            ))
            return
        }

        val isTeamA = _currentTeam.value == "A"
        val batCard = if (isTeamA) _teamAScorecard.value else _teamBScorecard.value
        if (batCard.nonStrikerId == null && batCard.strikerId != null) {
            // Lone batter dismissed: no partner remains and no new batter can enter
            processWicketDismissal(
                dismissalType = "Bowled",
                completedRuns = 0,
                outBatterId = batCard.strikerId,
                newBatterName = "",
                nextStrikerIsNewBatter = false
            )
            return
        }

        showDialog(DialogState.WicketDialog)
    }

    fun addNoRun() {
        if (isBallInputBlocked) return
        val active = _activeModifier.value
        if (active != null) {
            _activeModifier.value = null
            modifierCommitJob?.cancel()
            logAction()
            val isNB = active == "NB"
            val baseExtra = if (isNB) _rules.value.noBallRuns else _rules.value.wideBallRuns
            _runs.value += baseExtra
            if (isNB && _rules.value.isFreeHitEnabled) {
                _isFreeHitActive.value = true
            }
            updatePlayerStatsForBall(runsOffBat = 0, isExtraWide = (active == "WD"), isExtraNoBall = isNB, extraRuns = baseExtra)
            addBall(active)
            return
        }

        logAction()
        val wasFreeHit = _isFreeHitActive.value
        if (wasFreeHit) {
            _isFreeHitActive.value = false
        }
        updatePlayerStatsForBall(runsOffBat = 0, isExtraWide = false, isExtraNoBall = false, extraRuns = 0)
        addBall("•")
    }

    private fun logAction() {
        val snapshot = MatchStateSnapshot(
            runs = _runs.value,
            wickets = _wickets.value,
            balls = _balls.value,
            overs = _overs.value,
            currentTeam = _currentTeam.value,
            teamAScore = _teamAScore.value,
            teamBScore = _teamBScore.value,
            targetScore = _targetScore.value,
            secondInningsStarted = _secondInningsStarted.value,
            gameIsOver = _gameIsOver.value,
            isUmpireDecisionPending = _isUmpireDecisionPending.value,
            currentOverBallsEvents = _currentOverBallsEvents.value,
            runRateData = _runRateData.value,
            teamARunProgress = _teamARunProgress.value,
            teamBRunProgress = _teamBRunProgress.value,
            teamARunsPerOver = _teamARunsPerOver.value,
            teamBRunsPerOver = _teamBRunsPerOver.value,
            ballHistory = _ballHistory.value,
            isFreeHitActive = _isFreeHitActive.value,
            teamAName = _teamAName.value,
            teamBName = _teamBName.value,
            tossWinner = _tossWinner.value,
            tossDecision = _tossDecision.value,
            isSetupComplete = _isSetupComplete.value,
            teamAScorecard = _teamAScorecard.value,
            teamBScorecard = _teamBScorecard.value,
            rules = _rules.value,
            matchId = _matchId.value,
            matchDateFormatted = _matchDateFormatted.value
        )
        // Cap in-memory actionStack to 20 snapshots to prevent unbounded growth in 20-50 over matches
        _actionStack.value = (_actionStack.value + snapshot).takeLast(20)
    }

    private fun addBall(ballEvent: String) {
        val currentEvents = _currentOverBallsEvents.value.toMutableList()
        currentEvents.add(ballEvent)
        _currentOverBallsEvents.value = currentEvents

        val isNoBall = ballEvent.startsWith("NB")
        val isWide = ballEvent.startsWith("WD")
        val isLegal = when {
            isNoBall -> !_rules.value.isNoBallRebowled
            isWide -> !_rules.value.isWideRebowled
            else -> true
        }
        val newBalls = if (isLegal) _balls.value + 1 else _balls.value
        _balls.value = newBalls

        updateGraphDataOnBall()

        val historyEntry = BallHistoryEntry(
            over = _overs.value,
            ball = newBalls,
            team = _currentTeam.value,
            event = ballEvent
        )
        _ballHistory.value = listOf(historyEntry) + _ballHistory.value

        if (isLegal && newBalls >= 6) {
            logCompletedOverForManhattan()
            _balls.value = 0
            _overs.value += 1
            _currentOverBallsEvents.value = emptyList()

            val inningsEnded = (_overs.value >= _totalOvers.value && _totalOvers.value > 0) ||
                (!_rules.value.isUnlimitedWickets && _wickets.value >= _rules.value.maxWickets) ||
                (_currentTeam.value == "B" && _targetScore.value > 0 && _runs.value >= _targetScore.value)

            if (!inningsEnded) {
                _isBowlerChangePending.value = true
                showDialog(DialogState.ChangeBowlerDialog)
            }
        }

        checkInningEnd()
        saveGameToDb()
    }

    private fun updateGraphDataOnBall() {
        val totalBalls = _overs.value * 6 + _balls.value
        val rr = if (totalBalls > 0) (_runs.value.toFloat() / totalBalls * 6f) else 0f
        val formattedRR = Math.round(rr * 100f) / 100f

        val currentRR = _runRateData.value
        _runRateData.value = currentRR.filter { it.ball != totalBalls } + RunRatePoint(totalBalls, formattedRR.toFloat())

        if (_currentTeam.value == "A") {
            val currentProg = _teamARunProgress.value
            _teamARunProgress.value = currentProg.filter { it.ball != totalBalls } + RunProgressPoint(totalBalls, _runs.value)
        } else {
            val currentProg = _teamBRunProgress.value
            _teamBRunProgress.value = currentProg.filter { it.ball != totalBalls } + RunProgressPoint(totalBalls, _runs.value)
        }
    }

    private fun logCompletedOverForManhattan() {
        var runsThisOver = 0
        _currentOverBallsEvents.value.forEach { event ->
            runsThisOver += getRunsFromEvent(event)
        }
        if (_currentTeam.value == "A") {
            _teamARunsPerOver.value = _teamARunsPerOver.value + runsThisOver
        } else if (_currentTeam.value == "B" && _secondInningsStarted.value) {
            _teamBRunsPerOver.value = _teamBRunsPerOver.value + runsThisOver
        }
    }

    private fun checkInningEnd() {
        if (_gameIsOver.value || _isUmpireDecisionPending.value) return
        val allOut = if (_rules.value.isUnlimitedWickets) {
            false
        } else {
            _wickets.value >= _rules.value.maxWickets
        }
        if (_currentTeam.value == "A") {
            if (allOut || (_overs.value >= _totalOvers.value && _totalOvers.value > 0)) {
                switchInning()
            }
        } else {
            val hasReachedTarget = _targetScore.value > 0 && _runs.value >= _targetScore.value
            val isOversCompleted = _overs.value >= _totalOvers.value
            if (_totalOvers.value > 0 && (hasReachedTarget || isOversCompleted || allOut)) {
                finalizeGame(isManualDeclare = false, manualWinner = null)
            }
        }
    }

    fun switchInning() {
        if (_secondInningsStarted.value || _gameIsOver.value || _isUmpireDecisionPending.value) return
        logAction()
        _teamAScore.value = _runs.value
        _teamAOversStr.value = "${_overs.value}.${_balls.value}"
        _targetScore.value = _runs.value + 1

        showDialog(DialogState.Alert(
            title = "Innings Switched",
            message = "${_teamAName.value}: ${_teamAScore.value} runs.\nTarget for ${_teamBName.value}: ${_targetScore.value} runs.",
            actionText = "Start ${_teamBName.value}'s Innings"
        ))

        _runs.value = 0
        _wickets.value = 0
        _balls.value = 0
        _overs.value = 0
        _currentOverBallsEvents.value = emptyList()
        _currentTeam.value = "B"
        _secondInningsStarted.value = true
        _runRateData.value = emptyList()
        _teamBRunProgress.value = listOf(RunProgressPoint(0, 0))
        _teamBRunsPerOver.value = emptyList()
        _isFreeHitActive.value = false
        _activeModifier.value = null
        _isBowlerChangePending.value = false

        saveGameToDb()
    }

    fun finalizeGame(isManualDeclare: Boolean, manualWinner: String? = null) {
        if (_gameIsOver.value && !_isUmpireDecisionPending.value && !isManualDeclare) return
        _gameIsOver.value = true
        _isUmpireDecisionPending.value = false
        _isBowlerChangePending.value = false

        if (_currentTeam.value == "B") {
            _teamBScore.value = _runs.value
        } else {
            _teamAScore.value = _runs.value
        }

        val resultMessage = if (isManualDeclare) {
            when (manualWinner) {
                "A" -> "${_teamAName.value} declared WINNER by Umpire!"
                "B" -> "${_teamBName.value} declared WINNER by Umpire!"
                "TIE" -> "Match TIED by Umpire decision!"
                else -> {
                    _gameIsOver.value = false
                    return
                }
            }
        } else {
            if (_targetScore.value > 0) {
                val bScore = _runs.value
                val target = _targetScore.value
                when {
                    bScore >= target -> "${_teamBName.value} WINS!"
                    bScore == target - 1 -> "Match TIED!"
                    else -> "${_teamAName.value} WINS by ${(target - 1) - bScore} runs!"
                }
            } else if (!_secondInningsStarted.value && _overs.value >= _totalOvers.value && _totalOvers.value > 0) {
                "${_teamAName.value} Scored: ${_teamAScore.value}. Innings Over."
            } else {
                "Match Concluded."
            }
        }

        showDialog(DialogState.Alert(
            title = "Match Over",
            message = resultMessage,
            isWinnerDeclaration = true
        ))

        saveGameToDb()
    }

    fun undoLastBall() {
        if (_actionStack.value.isEmpty()) {
            showDialog(DialogState.Alert("Undo", "Nothing to undo!"))
            return
        }
        if (_isUmpireDecisionPending.value) {
            showDialog(DialogState.Alert("Undo", "Cannot undo during umpire decision."))
            return
        }

        val lastIndex = _actionStack.value.size - 1
        val last = _actionStack.value[lastIndex]
        _actionStack.value = _actionStack.value.subList(0, lastIndex)

        _runs.value = last.runs
        _wickets.value = last.wickets
        _balls.value = last.balls
        _overs.value = last.overs
        _currentTeam.value = last.currentTeam
        _teamAScore.value = last.teamAScore
        _teamBScore.value = last.teamBScore
        _targetScore.value = last.targetScore
        _secondInningsStarted.value = last.secondInningsStarted
        _gameIsOver.value = last.gameIsOver
        _isUmpireDecisionPending.value = last.isUmpireDecisionPending
        _currentOverBallsEvents.value = last.currentOverBallsEvents
        _runRateData.value = last.runRateData
        _teamARunProgress.value = last.teamARunProgress
        _teamBRunProgress.value = last.teamBRunProgress
        _teamARunsPerOver.value = last.teamARunsPerOver
        _teamBRunsPerOver.value = last.teamBRunsPerOver
        _ballHistory.value = last.ballHistory
        _isFreeHitActive.value = last.isFreeHitActive
        _teamAName.value = last.teamAName
        _teamBName.value = last.teamBName
        _tossWinner.value = last.tossWinner
        _tossDecision.value = last.tossDecision
        _isSetupComplete.value = last.isSetupComplete
        _teamAScorecard.value = last.teamAScorecard
        _teamBScorecard.value = last.teamBScorecard
        _rules.value = last.rules
        if (last.matchId.isNotBlank()) _matchId.value = last.matchId
        if (last.matchDateFormatted.isNotBlank()) _matchDateFormatted.value = last.matchDateFormatted
        _activeModifier.value = null

        // Dismiss any auto-opened dialogs (e.g. ChangeBowlerDialog or Match Over alerts)
        _isBowlerChangePending.value = false
        _dialogState.value = null

        saveGameToDb()
    }

    fun startUmpireDecision() {
        if (_gameIsOver.value && !_isUmpireDecisionPending.value) {
            showDialog(DialogState.ConfirmReset("Game already concluded. Override with Umpire Decision?"))
        } else {
            _isUmpireDecisionPending.value = true
            showDialog(DialogState.UmpireDecision)
        }
    }

    fun cancelUmpireDecision() {
        _isUmpireDecisionPending.value = false
        dismissDialog()
        saveGameToDb()
    }

    fun confirmUmpireOverride() {
        dismissDialog()
        _isUmpireDecisionPending.value = true
        showDialog(DialogState.UmpireDecision)
    }

    fun startMatch(
        teamA: String,
        teamB: String,
        overs: Int,
        winner: String,
        decision: String,
        customRules: MatchRules? = null
    ) {
        val newMatchId = "match_${System.currentTimeMillis()}"
        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        _matchId.value = newMatchId
        _matchDateFormatted.value = formattedDate

        if (customRules != null) {
            if (!customRules.isUnlimitedWickets && (customRules.maxWickets < 1 || customRules.maxWickets > 20)) {
                // Reject starting match if wickets are invalid (> 20 or < 1)
                return
            }
            _rules.value = customRules
        }
        if (overs < 1 || overs > 50) {
            // Reject starting match if overs are invalid (< 1 or > 50)
            return
        }
        val actualTeamA = if (teamA.isBlank()) "Team A" else teamA.trim()
        val actualTeamB = if (teamB.isBlank()) "Team B" else teamB.trim()
        _totalOvers.value = overs.coerceIn(1, 50)

        val tossWinnerName = if (winner == "A") actualTeamA else actualTeamB
        val tossDecisionString = if (decision == "bat") "batting" else "bowling"
        _tossWinner.value = tossWinnerName
        _tossDecision.value = tossDecisionString

        val teamABatsFirst = if (winner == "A") {
            decision == "bat"
        } else {
            decision == "bowl"
        }

        if (teamABatsFirst) {
            _teamAName.value = actualTeamA
            _teamBName.value = actualTeamB
        } else {
            _teamAName.value = actualTeamB
            _teamBName.value = actualTeamA
        }

        _currentTeam.value = "A"

        // Setup initial generic Scorecards with clean defaults
        val b1A = Batter(id = "a_b1", name = "Batter 1")
        val b2A = Batter(id = "a_b2", name = "Batter 2")
        val bw1A = Bowler(id = "a_bw1", name = "Bowler 1")

        val b1B = Batter(id = "b_b1", name = "Batter 1")
        val b2B = Batter(id = "b_b2", name = "Batter 2")
        val bw1B = Bowler(id = "b_bw1", name = "Bowler 1")

        _teamAScorecard.value = TeamScorecard(
            teamName = _teamAName.value,
            batters = listOf(b1A, b2A),
            bowlers = listOf(bw1A),
            strikerId = b1A.id,
            nonStrikerId = b2A.id,
            currentBowlerId = bw1A.id
        )

        _teamBScorecard.value = TeamScorecard(
            teamName = _teamBName.value,
            batters = listOf(b1B, b2B),
            bowlers = listOf(bw1B),
            strikerId = b1B.id,
            nonStrikerId = b2B.id,
            currentBowlerId = bw1B.id
        )

        _runs.value = 0
        _wickets.value = 0
        _balls.value = 0
        _overs.value = 0
        _teamAScore.value = 0
        _teamBScore.value = 0
        _targetScore.value = 0
        _secondInningsStarted.value = false
        _gameIsOver.value = false
        _isUmpireDecisionPending.value = false
        _currentOverBallsEvents.value = emptyList()
        _actionStack.value = emptyList()
        _ballHistory.value = emptyList()
        _isFreeHitActive.value = false
        _activeModifier.value = null
        initializeGraphData()

        _isSetupComplete.value = true
        _hasSavedActiveMatch.value = true
        dismissDialog()
        saveGameToDb()
    }

    fun continueActiveMatch() {
        _isSetupComplete.value = true
        dismissDialog()
    }

    fun exitToHomeScreen() {
        _isSetupComplete.value = false
        dismissDialog()
    }

    fun discardSavedActiveMatchAndReset() {
        resetGame()
    }

    fun resumeMatchFromHistory(matchIdToLoad: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = try {
                repository.getMatchById(matchIdToLoad)
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            } ?: return@launch

            val game = repository.deserializeGame(entity.gameJson) ?: return@launch
            withContext(Dispatchers.Main) {
                applyGameState(game)
                _matchId.value = entity.matchId
                _matchDateFormatted.value = entity.formattedDate
                _isSetupComplete.value = true
                _hasSavedActiveMatch.value = true
                dismissDialog()
                saveGameToDb(immediate = true)
            }
        }
    }

    fun deleteMatchFromHistory(matchIdToDelete: String) {
        viewModelScope.launch {
            repository.deleteMatchFromHistory(matchIdToDelete)
            if (_matchId.value == matchIdToDelete) {
                _hasSavedActiveMatch.value = false
                resetGame()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _hasSavedActiveMatch.value = false
            resetGame()
        }
    }

    private fun applyGameState(saved: GullyCricketGame) {
        _totalOvers.value = if (saved.totalOvers > 0) saved.totalOvers.coerceIn(1, 50) else saved.totalOvers
        _runs.value = saved.runs
        _wickets.value = saved.wickets
        _balls.value = saved.balls
        _overs.value = saved.overs
        _currentTeam.value = saved.currentTeam
        _secondInningsStarted.value = saved.secondInningsStarted
        _gameIsOver.value = saved.gameIsOver
        _isUmpireDecisionPending.value = saved.isUmpireDecisionPending
        _teamAScore.value = saved.teamAScore
        _teamBScore.value = saved.teamBScore
        _targetScore.value = saved.targetScore
        _currentOverBallsEvents.value = saved.currentOverBallsEvents
        _runRateData.value = saved.runRateData
        _teamARunProgress.value = saved.teamARunProgress
        _teamBRunProgress.value = saved.teamBRunProgress
        _teamARunsPerOver.value = saved.teamARunsPerOver
        _teamBRunsPerOver.value = saved.teamBRunsPerOver
        _ballHistory.value = saved.ballHistory
        _actionStack.value = saved.actionStack
        val effectiveTheme = prefs.getString("theme_mode", saved.themeMode) ?: saved.themeMode
        _themeMode.value = effectiveTheme
        _isFreeHitActive.value = saved.isFreeHitActive
        _teamAName.value = saved.teamAName
        _teamBName.value = saved.teamBName
        _tossWinner.value = saved.tossWinner
        _tossDecision.value = saved.tossDecision
        _isSetupComplete.value = saved.isSetupComplete
        _teamAScorecard.value = saved.teamAScorecard
        _teamBScorecard.value = saved.teamBScorecard
        _rules.value = saved.rules
        if (saved.matchId.isNotBlank()) _matchId.value = saved.matchId
        if (saved.matchDateFormatted.isNotBlank()) _matchDateFormatted.value = saved.matchDateFormatted
        if (saved.secondInningsStarted) {
            val bowledBalls = saved.teamBScorecard.bowlers.sumOf { it.oversLegalBalls }
            _teamAOversStr.value = if (bowledBalls > 0) "${bowledBalls / 6}.${bowledBalls % 6}" else "${saved.totalOvers}.0"
        }
    }

    fun resetGame() {
        _runs.value = 0
        _wickets.value = 0
        _balls.value = 0
        _overs.value = 0
        _teamAScore.value = 0
        _teamBScore.value = 0
        _teamAOversStr.value = "0.0"
        _targetScore.value = 0
        _currentTeam.value = "A"
        _secondInningsStarted.value = false
        _gameIsOver.value = false
        _isUmpireDecisionPending.value = false
        _currentOverBallsEvents.value = emptyList()
        _actionStack.value = emptyList()
        _totalOvers.value = 0
        _ballHistory.value = emptyList()
        initializeGraphData()
        _isFreeHitActive.value = false
        _activeModifier.value = null
        _isBowlerChangePending.value = false
        _dialogState.value = null

        _isSetupComplete.value = false
        _hasSavedActiveMatch.value = false
        _matchId.value = ""
        _matchDateFormatted.value = ""
        _teamAName.value = "Team A"
        _teamBName.value = "Team B"
        _teamAScorecard.value = createDefaultScorecard("Team A", "a")
        _teamBScorecard.value = createDefaultScorecard("Team B", "b")
        _tossWinner.value = ""
        _tossDecision.value = ""

        prefs.edit()
            .putBoolean("has_active_match", false)
            .putString("cached_team_a", "Team A")
            .putString("cached_team_b", "Team B")
            .putInt("cached_total_overs", 0)
            .putInt("cached_runs", 0)
            .putInt("cached_wickets", 0)
            .putInt("cached_overs", 0)
            .putInt("cached_balls", 0)
            .putInt("cached_target_score", 0)
            .putString("cached_current_team", "A")
            .putBoolean("cached_second_innings", false)
            .putBoolean("cached_game_over", false)
            .apply()

        viewModelScope.launch {
            repository.deleteSavedGame()
        }
    }

    private fun calculateResultSummary(): String {
        if (_gameIsOver.value) {
            if (_targetScore.value > 0) {
                val bScore = if (_secondInningsStarted.value && _currentTeam.value == "B") _runs.value else _teamBScore.value
                val aScore = _teamAScore.value
                val target = _targetScore.value
                val bWickets = if (_secondInningsStarted.value && _currentTeam.value == "B") _wickets.value else _teamBScorecard.value.batters.count { it.isOut }
                val maxW = if (_rules.value.isUnlimitedWickets) 10 else _rules.value.maxWickets
                val wicketsRemaining = (maxW - bWickets).coerceAtLeast(1)
                return when {
                    bScore >= target -> {
                        if (_rules.value.isUnlimitedWickets) "${_teamBName.value} won the game"
                        else "${_teamBName.value} won by $wicketsRemaining wicket${if (wicketsRemaining > 1) "s" else ""}"
                    }
                    bScore == target - 1 -> "Match Tied"
                    else -> "${_teamAName.value} won by ${(target - 1) - bScore} run${if ((target - 1) - bScore > 1) "s" else ""}"
                }
            } else {
                return "Match Concluded"
            }
        } else {
            return if (!_secondInningsStarted.value) {
                "In Progress • 1st Innings: ${_teamAName.value} ${_runs.value}/${_wickets.value} (${_overs.value}.${_balls.value} ov)"
            } else {
                "In Progress • 2nd Innings: ${_teamBName.value} ${_runs.value}/${_wickets.value} (Target: ${_targetScore.value})"
            }
        }
    }

    // --- DB Persistence ---

    fun saveGameToDb(immediate: Boolean = false) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            if (!immediate) {
                delay(80) // 80ms debounce to handle fast scoring button presses cleanly
            }
            saveMutex.withLock {
                try {
                    val currentId = if (_matchId.value.isBlank()) "match_${System.currentTimeMillis()}" else _matchId.value
                    _matchId.value = currentId
                    if (_matchDateFormatted.value.isBlank()) {
                        _matchDateFormatted.value = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                    }

                    // Keep at most 10 undos on disk to ensure row size remains tiny (<100KB) and prevent CursorWindow overflow
                    val game = GullyCricketGame(
                        totalOvers = _totalOvers.value,
                        runs = _runs.value,
                        wickets = _wickets.value,
                        balls = _balls.value,
                        overs = _overs.value,
                        currentTeam = _currentTeam.value,
                        secondInningsStarted = _secondInningsStarted.value,
                        gameIsOver = _gameIsOver.value,
                        isUmpireDecisionPending = _isUmpireDecisionPending.value,
                        teamAScore = _teamAScore.value,
                        teamBScore = _teamBScore.value,
                        targetScore = _targetScore.value,
                        currentOverBallsEvents = _currentOverBallsEvents.value,
                        runRateData = _runRateData.value,
                        teamARunProgress = _teamARunProgress.value,
                        teamBRunProgress = _teamBRunProgress.value,
                        teamARunsPerOver = _teamARunsPerOver.value,
                        teamBRunsPerOver = _teamBRunsPerOver.value,
                        ballHistory = _ballHistory.value,
                        actionStack = _actionStack.value.takeLast(10),
                        themeMode = _themeMode.value,
                        isFreeHitActive = _isFreeHitActive.value,
                        teamAName = _teamAName.value,
                        teamBName = _teamBName.value,
                        tossWinner = _tossWinner.value,
                        tossDecision = _tossDecision.value,
                        isSetupComplete = _isSetupComplete.value,
                        teamAScorecard = _teamAScorecard.value,
                        teamBScorecard = _teamBScorecard.value,
                        rules = _rules.value,
                        matchId = currentId,
                        matchDateFormatted = _matchDateFormatted.value
                    )
                    repository.saveGame(game)

                    prefs.edit()
                        .putString("theme_mode", _themeMode.value)
                        .putBoolean("has_active_match", _isSetupComplete.value)
                        .putString("cached_team_a", _teamAName.value)
                        .putString("cached_team_b", _teamBName.value)
                        .putInt("cached_total_overs", _totalOvers.value)
                        .putInt("cached_runs", _runs.value)
                        .putInt("cached_wickets", _wickets.value)
                        .putInt("cached_overs", _overs.value)
                        .putInt("cached_balls", _balls.value)
                        .putInt("cached_target_score", _targetScore.value)
                        .putString("cached_current_team", _currentTeam.value)
                        .putBoolean("cached_second_innings", _secondInningsStarted.value)
                        .putBoolean("cached_game_over", _gameIsOver.value)
                        .apply()

                    if (_isSetupComplete.value) {
                        _hasSavedActiveMatch.value = true
                        val aScore = if (!_secondInningsStarted.value && _currentTeam.value == "A") _runs.value else _teamAScore.value
                        val aWickets = if (!_secondInningsStarted.value && _currentTeam.value == "A") _wickets.value else _teamAScorecard.value.batters.count { it.isOut }
                        val aOversStr = if (!_secondInningsStarted.value && _currentTeam.value == "A") {
                            "${_overs.value}.${_balls.value}"
                        } else if (_teamAOversStr.value.isNotBlank() && _teamAOversStr.value != "0.0") {
                            _teamAOversStr.value
                        } else {
                            val bowledBalls = _teamBScorecard.value.bowlers.sumOf { it.oversLegalBalls }
                            if (bowledBalls > 0) "${bowledBalls / 6}.${bowledBalls % 6}" else "${_totalOvers.value}.0"
                        }

                        val bScore = if (_secondInningsStarted.value && _currentTeam.value == "B") _runs.value else _teamBScore.value
                        val bWickets = if (_secondInningsStarted.value && _currentTeam.value == "B") _wickets.value else _teamBScorecard.value.batters.count { it.isOut }
                        val bOversStr = if (_secondInningsStarted.value && _currentTeam.value == "B") "${_overs.value}.${_balls.value}" else if (_gameIsOver.value) "${_overs.value}.${_balls.value}" else "0.0"

                        val historyEntity = MatchHistoryEntity(
                            matchId = currentId,
                            timestamp = System.currentTimeMillis(),
                            formattedDate = _matchDateFormatted.value,
                            teamAName = _teamAName.value,
                            teamBName = _teamBName.value,
                            totalOvers = _totalOvers.value,
                            teamAScore = aScore,
                            teamAWickets = aWickets,
                            teamAOversStr = aOversStr,
                            teamBScore = bScore,
                            teamBWickets = bWickets,
                            teamBOversStr = bOversStr,
                            resultSummary = calculateResultSummary(),
                            isCompleted = _gameIsOver.value,
                            // In match history, action stack is not needed and consumes zero snapshot bloat
                            gameJson = repository.serializeGame(game.copy(actionStack = emptyList()))
                        )
                        repository.saveMatchToHistory(historyEntity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun loadSavedGame() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val saved = repository.getSavedGame()
                withContext(Dispatchers.Main) {
                    if (saved != null) {
                        applyGameState(saved)
                        // When app starts, stay on home/history
                        _isSetupComplete.value = false
                        _hasSavedActiveMatch.value = saved.isSetupComplete
                        prefs.edit().putBoolean("has_active_match", saved.isSetupComplete).apply()
                    } else {
                        _isSetupComplete.value = false
                        _hasSavedActiveMatch.value = false
                        prefs.edit().putBoolean("has_active_match", false).apply()
                    }
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isSetupComplete.value = false
                    _hasSavedActiveMatch.value = false
                    prefs.edit().putBoolean("has_active_match", false).apply()
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        saveJob?.cancel()
    }
}
