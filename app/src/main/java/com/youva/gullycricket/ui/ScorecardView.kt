package com.youva.gullycricket.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youva.gullycricket.CricketViewModel
import com.youva.gullycricket.model.Batter
import com.youva.gullycricket.model.Bowler
import com.youva.gullycricket.model.MatchRules
import com.youva.gullycricket.ui.theme.CricketColorScheme
import java.util.Locale

@Composable
fun ScorecardView(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val teamAName by viewModel.teamAName.collectAsState()
    val teamBName by viewModel.teamBName.collectAsState()
    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()
    val teamAScore by viewModel.teamAScore.collectAsState()
    val teamBScore by viewModel.teamBScore.collectAsState()
    val runs by viewModel.runs.collectAsState()
    val wickets by viewModel.wickets.collectAsState()
    val overs by viewModel.overs.collectAsState()
    val balls by viewModel.balls.collectAsState()
    val totalOvers by viewModel.totalOvers.collectAsState()
    val currentTeam by viewModel.currentTeam.collectAsState()
    val secondInningsStarted by viewModel.secondInningsStarted.collectAsState()
    val gameIsOver by viewModel.gameIsOver.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val tossWinner by viewModel.tossWinner.collectAsState()
    val tossDecision by viewModel.tossDecision.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val teamARunProgress by viewModel.teamARunProgress.collectAsState()
    val teamBRunProgress by viewModel.teamBRunProgress.collectAsState()
    val teamARunsPerOver by viewModel.teamARunsPerOver.collectAsState()
    val teamBRunsPerOver by viewModel.teamBRunsPerOver.collectAsState()
    val ballHistory by viewModel.ballHistory.collectAsState()

    var selectedTeamTab by remember { mutableStateOf(currentTeam) } // "A" or "B"

    val isSelectedCurrent = selectedTeamTab == currentTeam
    val card = if (selectedTeamTab == "A") teamAScorecard else teamBScorecard
    val bowlCard = if (selectedTeamTab == "A") teamBScorecard else teamAScorecard
    val displayTeamName = if (selectedTeamTab == "A") teamAName else teamBName

    val teamTotalRuns = if (isSelectedCurrent) runs else (if (selectedTeamTab == "A") teamAScore else teamBScore)
    val teamTotalWickets = if (isSelectedCurrent) wickets else card.fallOfWickets.size
    val teamOversStr = if (isSelectedCurrent) "$overs.$balls" else if (gameIsOver || secondInningsStarted) "$totalOvers" else "Yet to bat"

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
            teamARunProgress = teamARunProgress,
            teamBRunProgress = teamBRunProgress,
            teamARunsPerOver = teamARunsPerOver,
            teamBRunsPerOver = teamBRunsPerOver,
            ballHistory = ballHistory
        )
        CricketPdfGenerator.generateAndSharePdf(context, pdfData)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("scorecard_view")
    ) {
        // --- Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.buttonBg)
                        .testTag("scorecard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Match",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "SCORECARD",
                        color = colors.textAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "$teamAName vs $teamBName",
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // PDF Download Button
            Button(
                onClick = { exportPdf() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textAccent,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("download_pdf_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Download PDF",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "PDF",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // --- Team Innings Switch Tabs ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surface)
                .padding(3.dp)
        ) {
            listOf("A" to teamAName, "B" to teamBName).forEach { (tKey, tName) ->
                val isSelected = selectedTeamTab == tKey
                val tabColor = if (tKey == "A") colors.teamA else colors.teamB
                val tScore = if (tKey == currentTeam) "$runs/$wickets" else if (tKey == "A") "$teamAScore" else "$teamBScore"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) tabColor.copy(alpha = 0.25f) else Color.Transparent)
                        .border(
                            width = if (isSelected) 1.5.dp else 0.dp,
                            color = if (isSelected) tabColor else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedTeamTab = tKey }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = tName.uppercase(),
                            color = if (isSelected) tabColor else colors.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tScore,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // --- Main Scorecard Body (LazyColumn) ---
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Team Score Summary Banner
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = displayTeamName,
                                color = if (selectedTeamTab == "A") colors.teamA else colors.teamB,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val crr = if (isSelectedCurrent && (overs * 6 + balls) > 0) {
                                String.format(Locale.US, "%.2f", runs.toFloat() / (overs + balls / 6f))
                            } else if (totalOvers > 0) {
                                String.format(Locale.US, "%.2f", teamTotalRuns.toFloat() / totalOvers)
                            } else "0.0"
                            Text(
                                text = "CRR: $crr RPO",
                                color = colors.textSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$teamTotalRuns/$teamTotalWickets",
                                color = colors.textAccent,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "($teamOversStr / $totalOvers Ovs)",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. Batting Table Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "BATTING",
                            color = colors.textAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Batting Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.buttonBg)
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "BATTER", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text(text = "R", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "B", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "4s", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "6s", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "SR", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                        }

                        if (card.batters.isEmpty()) {
                            Text(
                                text = "No batting stats available yet.",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                            )
                        } else {
                            card.batters.forEachIndexed { idx, batter ->
                                val sr = if (batter.balls > 0) String.format(Locale.US, "%.1f", batter.runs * 100f / batter.balls) else "0.0"
                                val isCurrentStriker = batter.id == card.strikerId
                                val isNonStriker = batter.id == card.nonStrikerId

                                val statusStr = when {
                                    batter.isOut -> batter.dismissalInfo
                                    isCurrentStriker || isNonStriker -> "batting"
                                    else -> "not out"
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${batter.name}${if (isCurrentStriker) " *" else ""}",
                                            color = if (isCurrentStriker) colors.textAccent else colors.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isCurrentStriker || isNonStriker) FontWeight.Bold else FontWeight.Medium,
                                            modifier = Modifier.weight(2f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(text = "${batter.runs}", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(text = "${batter.balls}", color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(text = "${batter.fours}", color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(text = "${batter.sixes}", color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                        Text(text = sr, color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                                    }
                                    Text(
                                        text = statusStr,
                                        color = if (batter.isOut) colors.wicket.copy(alpha = 0.9f) else colors.textSecondary,
                                        fontSize = 11.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }

                                if (idx < card.batters.lastIndex) {
                                    HorizontalDivider(
                                        color = colors.buttonBg.copy(alpha = 0.4f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }

                        // Extras Row
                        val totalExtras = card.extrasWides + card.extrasNoBalls + card.extrasByes
                        Surface(
                            color = colors.buttonBg.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Extras (wd ${card.extrasWides}, nb ${card.extrasNoBalls}, b/lb ${card.extrasByes})",
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "$totalExtras",
                                    color = colors.textPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Bowling Table Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "BOWLING",
                            color = colors.textAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        // Bowling Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(colors.buttonBg)
                                .padding(vertical = 6.dp, horizontal = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "BOWLER", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                            Text(text = "O", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "R", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "W", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                            Text(text = "ECON", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                        }

                        if (bowlCard.bowlers.isEmpty()) {
                            Text(
                                text = "No bowling figures recorded yet.",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp)
                            )
                        } else {
                            bowlCard.bowlers.forEachIndexed { idx, bw ->
                                val ovStr = "${bw.oversLegalBalls / 6}.${bw.oversLegalBalls % 6}"
                                val econ = if (bw.oversLegalBalls > 0) String.format(Locale.US, "%.1f", bw.runsConceded * 6f / bw.oversLegalBalls) else "0.0"
                                val isCurrentBW = bw.id == bowlCard.currentBowlerId

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${bw.name}${if (isCurrentBW) " *" else ""}",
                                        color = if (isCurrentBW) (if (selectedTeamTab == "A") colors.teamB else colors.teamA) else colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrentBW) FontWeight.Bold else FontWeight.Medium,
                                        modifier = Modifier.weight(2f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(text = ovStr, color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(text = "${bw.runsConceded}", color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(text = "${bw.wickets}", color = colors.wicket, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                                    Text(text = econ, color = colors.textSecondary, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                                }

                                if (idx < bowlCard.bowlers.lastIndex) {
                                    HorizontalDivider(
                                        color = colors.buttonBg.copy(alpha = 0.4f),
                                        thickness = 0.5.dp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Fall of Wickets Card
            if (card.fallOfWickets.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "FALL OF WICKETS",
                                color = colors.textAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            card.fallOfWickets.forEach { fow ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${fow.runs}-${fow.wicketNumber} (${fow.batterName})",
                                        color = colors.textPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${fow.overs} ov",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Offline PDF Download & Share Banner at bottom
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = colors.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = "PDF",
                                tint = colors.textAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Offline Match Report & Analysis",
                                color = colors.textAccent,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Download a complete PDF report with match scorecard, bowling metrics, run rates, and key analytical highlights to save offline or share with friends.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { exportPdf() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.textAccent,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("download_pdf_banner_button")
                        ) {
                            Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = "Download PDF")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DOWNLOAD MATCH REPORT (PDF)",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
