package com.youva.gullycricket.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youva.gullycricket.CricketViewModel
import com.youva.gullycricket.model.TeamScorecard
import com.youva.gullycricket.ui.theme.CricketColorScheme
import java.util.Locale

data class WinProbResult(
    val teamAPercent: Int,
    val teamBPercent: Int,
    val statusText: String
)

data class PartnershipInfo(
    val currentRuns: Int,
    val currentBalls: Int,
    val strikerName: String,
    val strikerRunsInP: Int,
    val nonStrikerName: String,
    val nonStrikerRunsInP: Int,
    val highestRuns: Int,
    val highestBalls: Int,
    val highestWicketNo: Int
)

fun calculateWinProbability(
    runs: Int,
    wickets: Int,
    overs: Int,
    balls: Int,
    totalOvers: Int,
    currentTeam: String,
    secondInningsStarted: Boolean,
    gameIsOver: Boolean,
    teamAScore: Int,
    targetScore: Int,
    teamAName: String,
    teamBName: String
): WinProbResult {
    if (gameIsOver) {
        return if (secondInningsStarted) {
            val bScore = runs
            if (bScore >= targetScore) {
                WinProbResult(0, 100, "$teamBName Won the Match")
            } else if (bScore == targetScore - 1) {
                WinProbResult(50, 50, "Match Tied")
            } else {
                WinProbResult(100, 0, "$teamAName Won the Match")
            }
        } else {
            WinProbResult(100, 0, "$teamAName Completed Innings ($runs runs)")
        }
    }

    if (!secondInningsStarted) {
        val totalBallsBowled = overs * 6 + balls
        val totalMatchBalls = maxOf(totalOvers * 6, 1)
        val ballsRemaining = maxOf(0, totalMatchBalls - totalBallsBowled)
        val crr = if (totalBallsBowled > 0) (runs.toFloat() / totalBallsBowled) * 6f else 7.5f

        val parRate = 8.0f
        val parScore = totalOvers * parRate
        val projScore = runs + (ballsRemaining / 6f * maxOf(crr, 4.0f))

        var teamAPercent = 50.0f
        teamAPercent += (projScore - parScore) * 0.5f
        teamAPercent -= (wickets * 3.5f)

        val clampedA = teamAPercent.toInt().coerceIn(5, 95)
        val clampedB = 100 - clampedA
        val status = "$teamAName Projected Score: ${projScore.toInt()} runs"
        return WinProbResult(clampedA, clampedB, status)
    } else {
        val totalBallsBowled = overs * 6 + balls
        val totalMatchBalls = maxOf(totalOvers * 6, 1)
        val ballsRemaining = maxOf(0, totalMatchBalls - totalBallsBowled)
        val runsNeeded = targetScore - runs
        val wicketsRemaining = maxOf(0, 10 - wickets)

        if (runsNeeded <= 0) {
            return WinProbResult(0, 100, "$teamBName Reached Target!")
        }
        if (wicketsRemaining <= 0 || (ballsRemaining == 0 && runsNeeded > 1)) {
            return WinProbResult(100, 0, "$teamAName Defended Target!")
        }
        if (ballsRemaining == 0 && runsNeeded == 1) {
            return WinProbResult(50, 50, "Scores Level (Match Tied)")
        }

        val crr = if (totalBallsBowled > 0) (runs.toFloat() / totalBallsBowled) * 6f else 0f
        val rrr = if (ballsRemaining > 0) (runsNeeded.toFloat() / ballsRemaining) * 6f else 99f

        var teamBPercent = 50.0f
        val rateDiff = crr - rrr
        teamBPercent += rateDiff * 6.0f
        teamBPercent += (wicketsRemaining - 5) * 5.0f

        if (rrr > 12f) teamBPercent -= (rrr - 12f) * 4f
        if (rrr <= 6f) teamBPercent += (6f - rrr) * 3f

        val clampedB = teamBPercent.toInt().coerceIn(1, 99)
        val clampedA = 100 - clampedB
        val status = "$teamBName needs $runsNeeded runs off $ballsRemaining balls (RRR: ${String.format(Locale.US, "%.1f", rrr)})"
        return WinProbResult(clampedA, clampedB, status)
    }
}

fun calculatePartnershipInfo(
    batCard: TeamScorecard,
    currentRuns: Int,
    currentOvers: Int,
    currentBalls: Int
): PartnershipInfo {
    val totalCurrentBalls = currentOvers * 6 + currentBalls
    val fallOfWickets = batCard.fallOfWickets

    val lastFow = fallOfWickets.lastOrNull()
    val lastWicketRuns = lastFow?.runs ?: 0
    val lastWicketBalls = lastFow?.let {
        val parts = it.overs.split(".")
        val o = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val b = parts.getOrNull(1)?.toIntOrNull() ?: 0
        o * 6 + b
    } ?: 0

    val currentPRuns = maxOf(0, currentRuns - lastWicketRuns)
    val currentPBalls = maxOf(0, totalCurrentBalls - lastWicketBalls)

    val striker = batCard.batters.find { it.id == batCard.strikerId }
    val nonStriker = batCard.batters.find { it.id == batCard.nonStrikerId }

    val strikerName = striker?.name ?: "Striker"
    val nonStrikerName = nonStriker?.name ?: "Non-Striker"

    var prevRuns = 0
    var prevBalls = 0
    var maxPRuns = currentPRuns
    var maxPBalls = currentPBalls
    var maxPWicketNo = fallOfWickets.size + 1

    fallOfWickets.forEachIndexed { index, fow ->
        val fowBalls = fow.overs.split(".").let { parts ->
            val o = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val b = parts.getOrNull(1)?.toIntOrNull() ?: 0
            o * 6 + b
        }
        val pRuns = fow.runs - prevRuns
        val pBalls = maxOf(0, fowBalls - prevBalls)

        if (pRuns > maxPRuns) {
            maxPRuns = pRuns
            maxPBalls = pBalls
            maxPWicketNo = index + 1
        }
        prevRuns = fow.runs
        prevBalls = fowBalls
    }

    return PartnershipInfo(
        currentRuns = currentPRuns,
        currentBalls = currentPBalls,
        strikerName = strikerName,
        strikerRunsInP = striker?.runs ?: 0,
        nonStrikerName = nonStrikerName,
        nonStrikerRunsInP = nonStriker?.runs ?: 0,
        highestRuns = maxPRuns,
        highestBalls = maxPBalls,
        highestWicketNo = maxPWicketNo
    )
}

@Composable
fun MatchAnalysisView(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    onBack: () -> Unit
) {
    val runs by viewModel.runs.collectAsState()
    val wickets by viewModel.wickets.collectAsState()
    val overs by viewModel.overs.collectAsState()
    val balls by viewModel.balls.collectAsState()
    val totalOvers by viewModel.totalOvers.collectAsState()
    val currentTeam by viewModel.currentTeam.collectAsState()
    val secondInningsStarted by viewModel.secondInningsStarted.collectAsState()
    val gameIsOver by viewModel.gameIsOver.collectAsState()
    val teamAScore by viewModel.teamAScore.collectAsState()
    val teamBScore by viewModel.teamBScore.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val teamAName by viewModel.teamAName.collectAsState()
    val teamBName by viewModel.teamBName.collectAsState()
    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()
    val runRatePoints by viewModel.runRateData.collectAsState()
    val teamAProg by viewModel.teamARunProgress.collectAsState()
    val teamBProg by viewModel.teamBRunProgress.collectAsState()
    val teamARunsPerOver by viewModel.teamARunsPerOver.collectAsState()
    val teamBRunsPerOver by viewModel.teamBRunsPerOver.collectAsState()
    val ballHistory by viewModel.ballHistory.collectAsState()
    val tossWinner by viewModel.tossWinner.collectAsState()
    val tossDecision by viewModel.tossDecision.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val context = LocalContext.current

    var selectedGraphTab by remember { mutableStateOf("worm") } // "worm", "manhattan", "runrate"

    val totalBallsBowled = overs * 6 + balls
    val totalMatchBalls = maxOf(totalOvers * 6, 1)
    val ballsRemaining = maxOf(0, totalMatchBalls - totalBallsBowled)
    val crr = if (totalBallsBowled > 0) (runs.toFloat() / totalBallsBowled) * 6f else 0f
    val rrr = if (secondInningsStarted && ballsRemaining > 0) {
        val runsNeeded = targetScore - runs
        if (runsNeeded > 0) (runsNeeded.toFloat() / ballsRemaining) * 6f else 0f
    } else 0f

    val batCard = if (currentTeam == "A") teamAScorecard else teamBScorecard

    val winProb = remember(
        runs, wickets, overs, balls, totalOvers, currentTeam, secondInningsStarted,
        gameIsOver, teamAScore, targetScore, teamAName, teamBName
    ) {
        calculateWinProbability(
            runs = runs,
            wickets = wickets,
            overs = overs,
            balls = balls,
            totalOvers = totalOvers,
            currentTeam = currentTeam,
            secondInningsStarted = secondInningsStarted,
            gameIsOver = gameIsOver,
            teamAScore = teamAScore,
            targetScore = targetScore,
            teamAName = teamAName,
            teamBName = teamBName
        )
    }

    val partnershipInfo = remember(batCard, runs, overs, balls) {
        calculatePartnershipInfo(
            batCard = batCard,
            currentRuns = runs,
            currentOvers = overs,
            currentBalls = balls
        )
    }

    fun exportPdf() {
        val pdfData = CricketPdfGenerator.MatchPdfData(
            teamAName = teamAName,
            teamBName = teamBName,
            teamAScore = teamAScore,
            teamBScore = teamBScore,
            liveRuns = runs,
            liveWickets = wickets,
            liveOvers = overs,
            liveBalls = balls,
            totalOvers = totalOvers,
            currentTeam = currentTeam,
            secondInningsStarted = secondInningsStarted,
            gameIsOver = gameIsOver,
            targetScore = targetScore,
            tossWinner = tossWinner,
            tossDecision = tossDecision,
            teamAScorecard = teamAScorecard,
            teamBScorecard = teamBScorecard,
            rules = rules,
            teamARunProgress = teamAProg,
            teamBRunProgress = teamBProg,
            teamARunsPerOver = teamARunsPerOver,
            teamBRunsPerOver = teamBRunsPerOver,
            ballHistory = ballHistory
        )
        CricketPdfGenerator.generateAndSharePdf(context, pdfData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("match_analysis_view")
    ) {
        // --- Header Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.buttonBg)
                        .testTag("match_analysis_back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = colors.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Match Analysis",
                    color = colors.textAccent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // PDF Button
                Button(
                    onClick = { exportPdf() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.textAccent,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("analysis_download_pdf_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Download PDF",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "PDF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Live Indicator Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.teamA.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colors.teamA)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE",
                        color = colors.teamA,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // --- Current Match Overview Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (!secondInningsStarted) "$teamAName (Innings 1)" else "$teamBName Chasing $targetScore",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$runs/$wickets",
                            color = colors.textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Overs: $overs.$balls / $totalOvers",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "CRR: ${String.format(Locale.US, "%.2f", crr)}",
                            color = colors.textAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (secondInningsStarted) {
                            Text(
                                text = "RRR: ${String.format(Locale.US, "%.2f", rrr)}",
                                color = colors.teamB,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- 1. Live Win Probability Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(12.dp)
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Live Win Probability",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$teamAName ${winProb.teamAPercent}%",
                            color = colors.teamA,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${winProb.teamBPercent}% $teamBName",
                            color = colors.teamB,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Segmented Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.buttonBg)
                    ) {
                        if (winProb.teamAPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(winProb.teamAPercent.toFloat())
                                    .fillMaxHeight()
                                    .background(colors.teamA)
                            )
                        }
                        if (winProb.teamBPercent > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(winProb.teamBPercent.toFloat())
                                    .fillMaxHeight()
                                    .background(colors.teamB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = winProb.statusText,
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- 2. Projected Score Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (!secondInningsStarted) "Projected Score Analysis" else "Target Chasing Metrics",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (!secondInningsStarted) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val projCRR = runs + (ballsRemaining / 6f * crr).toInt()
                            val proj6 = runs + (ballsRemaining / 6f * 6.0f).toInt()
                            val proj8 = runs + (ballsRemaining / 6f * 8.0f).toInt()
                            val proj10 = runs + (ballsRemaining / 6f * 10.0f).toInt()

                            ProjectedScoreItem(
                                label = "At CRR (${String.format(Locale.US, "%.1f", crr)})",
                                score = "$projCRR",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            ProjectedScoreItem(
                                label = "@ 6 RPO",
                                score = "$proj6",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            ProjectedScoreItem(
                                label = "@ 8 RPO",
                                score = "$proj8",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            ProjectedScoreItem(
                                label = "@ 10 RPO",
                                score = "$proj10",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        val runsNeeded = maxOf(0, targetScore - runs)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ProjectedScoreItem(
                                label = "Target",
                                score = "$targetScore",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            ProjectedScoreItem(
                                label = "Runs Needed",
                                score = "$runsNeeded",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            ProjectedScoreItem(
                                label = "Balls Left",
                                score = "$ballsRemaining",
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                            ProjectedScoreItem(
                                label = "Required RR",
                                score = String.format(Locale.US, "%.2f", rrr),
                                colors = colors,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // --- 3. Partnership Analysis Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Partnership Analysis",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Current Partnership Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.buttonBg)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Current Partnership",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${partnershipInfo.currentRuns} runs",
                                    color = colors.textAccent,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "off ${partnershipInfo.currentBalls} balls",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${partnershipInfo.strikerName} & ${partnershipInfo.nonStrikerName}",
                                    color = colors.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Highest Partnership Box
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.buttonBg)
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Highest Partnership",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${partnershipInfo.highestRuns} runs",
                                    color = colors.teamA,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "off ${partnershipInfo.highestBalls} balls",
                                    color = colors.textSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Wicket #${partnershipInfo.highestWicketNo}",
                                    color = colors.textPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. All Graphs Container Card ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Match Visual Analytics",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Graph Filter Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.buttonBg)
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GraphTabItem(
                            label = "Worm",
                            isSelected = selectedGraphTab == "worm",
                            colors = colors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("graph_tab_worm"),
                            onClick = { selectedGraphTab = "worm" }
                        )
                        GraphTabItem(
                            label = "Manhattan",
                            isSelected = selectedGraphTab == "manhattan",
                            colors = colors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("graph_tab_manhattan"),
                            onClick = { selectedGraphTab = "manhattan" }
                        )
                        GraphTabItem(
                            label = "Run Rate",
                            isSelected = selectedGraphTab == "runrate",
                            colors = colors,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("graph_tab_runrate"),
                            onClick = { selectedGraphTab = "runrate" }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Selected Graph Container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                    ) {
                        when (selectedGraphTab) {
                            "worm" -> WormLineChart(
                                teamAProgress = teamAProg,
                                teamBProgress = teamBProg,
                                totalOvers = totalOvers,
                                colors = colors,
                                modifier = Modifier.fillMaxSize(),
                                ballHistory = ballHistory,
                                teamAName = teamAName,
                                teamBName = teamBName
                            )
                            "manhattan" -> ManhattanChart(
                                teamARuns = teamARunsPerOver,
                                teamBRuns = teamBRunsPerOver,
                                totalOvers = totalOvers,
                                colors = colors,
                                modifier = Modifier.fillMaxSize(),
                                ballHistory = ballHistory,
                                teamAName = teamAName,
                                teamBName = teamBName
                            )
                            "runrate" -> RunRateLineChart(
                                runRatePoints = runRatePoints,
                                colors = colors,
                                modifier = Modifier.fillMaxSize(),
                                ballHistory = ballHistory
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ProjectedScoreItem(
    label: String,
    score: String,
    colors: CricketColorScheme,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = colors.textSecondary,
            fontSize = 9.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = score,
            color = colors.textAccent,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GraphTabItem(
    label: String,
    isSelected: Boolean,
    colors: CricketColorScheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) colors.surface else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) colors.textAccent else colors.textSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
