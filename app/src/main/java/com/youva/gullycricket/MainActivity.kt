package com.youva.gullycricket

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.youva.gullycricket.model.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import java.util.Locale
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.youva.gullycricket.ui.MatchAnalysisView
import com.youva.gullycricket.ui.MatchHistoryView
import com.youva.gullycricket.ui.RunRateLineChart
import com.youva.gullycricket.ui.ScorecardView
import com.youva.gullycricket.ui.theme.CricketColorScheme
import com.youva.gullycricket.ui.theme.CricketDarkColors
import com.youva.gullycricket.ui.theme.CricketLightColors
import com.youva.gullycricket.ui.theme.MyApplicationTheme
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CricketViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()

            MyApplicationTheme(darkTheme = themeMode == "dark") {
                val cricketColors = if (themeMode == "dark") CricketDarkColors else CricketLightColors

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold"),
                    containerColor = cricketColors.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0) // Draw completely full-screen edge-to-edge
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        MainLayout(viewModel = viewModel, colors = cricketColors)
                    }
                }
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: CricketViewModel, colors: CricketColorScheme) {
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()

    var activeView by remember { mutableStateOf("score") } // "score", "match_history", "history", "analysis", "scorecard"
    var menuOpen by remember { mutableStateOf(false) }

    if (!isSetupComplete) {
        if (activeView == "match_history") {
            MatchHistoryView(
                viewModel = viewModel,
                colors = colors,
                onBack = { activeView = "score" },
                onContinueMatch = { activeView = "score" }
            )
        } else {
            HomeSetupScreen(
                viewModel = viewModel,
                colors = colors,
                onOpenHistory = { activeView = "match_history" }
            )
        }
        return
    }

    val clockTime by viewModel.clockTime.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val statusPadding = WindowInsets.statusBars.asPaddingValues()
    val navPadding = WindowInsets.navigationBars.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // --- Custom Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(top = statusPadding.calculateTopPadding() + 8.dp)
                .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = clockTime,
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium
            )

            // Dynamic theme button
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.buttonBg)
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (themeMode == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = "Theme Toggle",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // --- Main Visual Screen Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (activeView) {
                "score" -> ScoreView(
                    viewModel = viewModel,
                    colors = colors,
                    onOpenMenu = { menuOpen = true }
                )
                "history" -> HistoryView(
                    viewModel = viewModel,
                    colors = colors,
                    onBack = { activeView = "score" }
                )
                "analysis" -> MatchAnalysisView(
                    viewModel = viewModel,
                    colors = colors,
                    onBack = { activeView = "score" }
                )
                "scorecard" -> ScorecardView(
                    viewModel = viewModel,
                    colors = colors,
                    onBack = { activeView = "score" }
                )
            }
        }

        Spacer(modifier = Modifier.height(navPadding.calculateBottomPadding()))
    }

    // --- Slide-Up Action Menu Overlay ---
    AnimatedVisibility(
        visible = menuOpen,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { menuOpen = false }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // prevent click through
            ) {
                MenuContent(
                    viewModel = viewModel,
                    colors = colors,
                    onSelectScorecard = {
                        activeView = "scorecard"
                        menuOpen = false
                    },
                    onSelectHistory = {
                        activeView = "history"
                        menuOpen = false
                    },
                    onSelectMatchAnalysis = {
                        activeView = "analysis"
                        menuOpen = false
                    },
                    onClose = { menuOpen = false }
                )
            }
        }
    }

    // --- Custom Alert/Confirm Dialog Router ---
    DialogRouter(viewModel = viewModel, colors = colors)
}

fun formatBallEventForDisplay(event: String): String {
    return when {
        event == "NB" -> "N"
        event.startsWith("NB+") -> {
            val suffix = event.substringAfter("+")
            "N$suffix"
        }
        event == "WD" -> "WD"
        event.startsWith("WD+") -> {
            val suffix = event.substringAfter("+")
            "WD$suffix"
        }
        else -> event
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScoreView(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    onOpenMenu: () -> Unit
) {
    val currentTeam by viewModel.currentTeam.collectAsState()
    val totalOvers by viewModel.totalOvers.collectAsState()
    val runs by viewModel.runs.collectAsState()
    val wickets by viewModel.wickets.collectAsState()
    val overs by viewModel.overs.collectAsState()
    val balls by viewModel.balls.collectAsState()
    val secondInningsStarted by viewModel.secondInningsStarted.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()
    val currentOverBallsEvents by viewModel.currentOverBallsEvents.collectAsState()

    val teamAName by viewModel.teamAName.collectAsState()
    val teamBName by viewModel.teamBName.collectAsState()
    val currentTeamName = if (currentTeam == "A") teamAName else teamBName

    val tossWinner by viewModel.tossWinner.collectAsState()
    val tossDecision by viewModel.tossDecision.collectAsState()

    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()

    val runRatePoints by viewModel.runRateData.collectAsState()
    val ballHistory by viewModel.ballHistory.collectAsState()

    val gameIsOver by viewModel.gameIsOver.collectAsState()
    val isUmpireDecisionPending by viewModel.isUmpireDecisionPending.collectAsState()
    val isFreeHitActive by viewModel.isFreeHitActive.collectAsState()
    val activeModifier by viewModel.activeModifier.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val actionStack by viewModel.actionStack.collectAsState()
    val isBowlerChangePending by viewModel.isBowlerChangePending.collectAsState()
    val dialogState by viewModel.dialogState.collectAsState()

    val interactive = !gameIsOver && !isUmpireDecisionPending && !isBowlerChangePending && dialogState == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Match Status ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val teamColor = if (currentTeam == "A") colors.teamA else colors.teamB
            Text(
                text = currentTeamName.uppercase(),
                color = teamColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif
            )
            Text(
                text = "/ $totalOvers OVS",
                color = colors.textSecondary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }

        if (tossWinner.isNotEmpty() && tossDecision.isNotEmpty()) {
            Text(
                text = "Toss: $tossWinner won & chose to $tossDecision first",
                color = colors.textSecondary,
                fontSize = 11.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                textAlign = TextAlign.Start
            )
        }

        // --- Main Runs / Wickets Card ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Runs
                Text(
                    text = "$runs",
                    color = colors.textPrimary,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "/",
                    color = colors.textSecondary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                // Wickets
                Text(
                    text = "$wickets",
                    color = colors.wicket,
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // --- Overs & Balls display ---
        Text(
            text = "$overs.$balls",
            color = colors.textSecondary,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // --- Active Batting & Bowling Scorecard Card ---
        var selectedRunForRunOut by remember { mutableStateOf<Int?>(null) }

        val batCard = if (currentTeam == "A") teamAScorecard else teamBScorecard
        val bowlCard = if (currentTeam == "A") teamBScorecard else teamAScorecard

        val isSoloBatting = batCard.nonStrikerId == null || (rules.isLastManStanding && !rules.isUnlimitedWickets && wickets >= rules.maxWickets - 1)

        val striker = batCard.batters.find { it.id == batCard.strikerId }
            ?: batCard.batters.getOrNull(0) ?: Batter(id = "b1", name = "Batter 1")
        val nonStriker = batCard.batters.find { it.id == batCard.nonStrikerId }
            ?: batCard.batters.getOrNull(1) ?: Batter(id = "b2", name = "Batter 2")
        val bowler = bowlCard.bowlers.find { it.id == bowlCard.currentBowlerId }
            ?: bowlCard.bowlers.getOrNull(0) ?: Bowler(id = "bw1", name = "Bowler 1")

        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // LEFT COLUMN: BATSMEN
                    Column(
                        modifier = Modifier
                            .weight(1.3f)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isSoloBatting) {
                            // Lone Batsman Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "LONE BATTER",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textAccent
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Surface(
                                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(3.dp)
                                    ) {
                                        Text(
                                            text = "Last Man",
                                            color = Color(0xFFFFD700),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            // Single Lone Striker Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedRunForRunOut != null) colors.wicket.copy(alpha = 0.2f) else Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedRunForRunOut != null) {
                                                val r = selectedRunForRunOut!!
                                                selectedRunForRunOut = null
                                                viewModel.processDirectRunOut(striker.id, r)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.showDialog(
                                                DialogState.Alert(
                                                    title = "Last Man Standing",
                                                    message = "${striker.name} is already batting as the Last Man Standing."
                                                )
                                            )
                                        }
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "* ${striker.name} ",
                                        color = colors.textAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "(${striker.runs}) ${striker.balls}b",
                                        color = colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.showDialog(DialogState.RenamePlayerDialog(striker.id, striker.name, false))
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Striker",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        } else {
                            // Standard 2-batter header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BATSMEN",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = "Swap",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textAccent,
                                    modifier = Modifier.clickable { viewModel.swapStrike() }
                                )
                            }

                            // Striker Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedRunForRunOut != null) colors.wicket.copy(alpha = 0.2f) else Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedRunForRunOut != null) {
                                                val r = selectedRunForRunOut!!
                                                selectedRunForRunOut = null
                                                viewModel.processDirectRunOut(striker.id, r)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.showDialog(
                                                DialogState.LastManStandingPrompt(striker.id, striker.name)
                                            )
                                        }
                                    )
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "* ${striker.name} ",
                                        color = colors.textAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "(${striker.runs})${striker.balls}",
                                        color = colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.showDialog(DialogState.RenamePlayerDialog(striker.id, striker.name, false))
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Striker",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }

                            // Non-Striker Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (selectedRunForRunOut != null) colors.wicket.copy(alpha = 0.2f) else Color.Transparent)
                                    .combinedClickable(
                                        onClick = {
                                            if (selectedRunForRunOut != null) {
                                                val r = selectedRunForRunOut!!
                                                selectedRunForRunOut = null
                                                viewModel.processDirectRunOut(nonStriker.id, r)
                                            }
                                        },
                                        onLongClick = {
                                            viewModel.showDialog(
                                                DialogState.LastManStandingPrompt(nonStriker.id, nonStriker.name)
                                            )
                                        }
                                    )
                                    .padding(vertical = 1.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "  ${nonStriker.name} ",
                                        color = colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "(${nonStriker.runs})${nonStriker.balls}",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        viewModel.showDialog(DialogState.RenamePlayerDialog(nonStriker.id, nonStriker.name, false))
                                    },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Non-Striker",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                }
                            }
                        }
                    }

                    // VERTICAL SEPARATOR
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(75.dp)
                            .background(colors.buttonBg)
                    )

                    // RIGHT COLUMN: BOWLER
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "BOWLER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.showDialog(DialogState.RenamePlayerDialog(bowler.id, bowler.name, true))
                                }
                        ) {
                            Text(
                                text = bowler.name,
                                color = colors.teamB,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            IconButton(
                                onClick = {
                                    viewModel.showDialog(DialogState.RenamePlayerDialog(bowler.id, bowler.name, true))
                                },
                                modifier = Modifier.size(16.dp).padding(start = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Bowler",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        val bowlerOversStr = "${bowler.oversLegalBalls / 6}.${bowler.oversLegalBalls % 6}"
                        Text(
                            text = "${bowler.wickets}/${bowler.runsConceded} ($bowlerOversStr ov)",
                            color = colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = { viewModel.showDialog(DialogState.ChangeBowlerDialog) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.buttonBg,
                                contentColor = colors.textPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .height(24.dp)
                        ) {
                            Text(text = "Change Bowler", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (selectedRunForRunOut != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.wicket)
                            .padding(vertical = 2.dp, horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tap batsman above to RUN OUT ($selectedRunForRunOut runs)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cancel",
                            color = Color.Yellow,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { selectedRunForRunOut = null }
                        )
                    }
                }
            }
        }

        if (isFreeHitActive) {
            Box(
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.wicket)
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FREE HIT",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }

        // --- Target & Req Run Rate Banner ---
        if (secondInningsStarted && targetScore > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row {
                    Text(text = "TGT: ", color = colors.textSecondary, fontSize = 12.sp)
                    Text(text = "$targetScore", color = colors.textAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Row {
                    Text(text = "REQ: ", color = colors.textSecondary, fontSize = 12.sp)
                    val rrrText = remember(runs, overs, balls, targetScore, totalOvers, gameIsOver) {
                        val runsNeeded = targetScore - runs
                        val totalBalls = totalOvers * 6
                        val ballsBowled = overs * 6 + balls
                        val ballsRemaining = totalBalls - ballsBowled
                        when {
                            runsNeeded <= 0 -> "WON"
                            ballsRemaining <= 0 -> "LOST"
                            else -> String.format(Locale.US, "%.1f", (runsNeeded.toFloat() / ballsRemaining) * 6f)
                        }
                    }
                    Text(text = rrrText, color = colors.textAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Current Over Balls events ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            currentOverBallsEvents.forEach { event ->
                val ballColor = when {
                    event == "W" || event.endsWith("W") -> colors.wicket
                    event == "6" || event == "4" || event.endsWith("6") || event.endsWith("4") -> colors.boundary
                    event == "•" -> colors.textSecondary
                    event.startsWith("WD") || event.startsWith("NB") -> colors.umpire
                    else -> colors.buttonBg
                }
                val textColor = if (event == "•") colors.surface else if (ballColor == colors.buttonBg) colors.textPrimary else Color.White
                val displayedText = formatBallEventForDisplay(event)

                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                        .clip(CircleShape)
                        .background(ballColor)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayedText,
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        style = TextStyle(lineHeight = 10.sp)
                    )
                }
            }
        }

        // --- Run Rate Graph ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .weight(1.5f)
                .padding(vertical = 4.dp)
        ) {
            RunRateLineChart(
                runRatePoints = runRatePoints,
                colors = colors,
                modifier = Modifier.fillMaxSize(),
                ballHistory = ballHistory
            )
        }

        // --- Action Buttons ---

        if (activeModifier != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.umpire.copy(alpha = 0.2f))
                    .padding(vertical = 4.dp, horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Press 0-6 / W for ${if (activeModifier == "WD") "Wide" else "No Ball"}",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = "Cancel",
                    color = colors.wicket,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { viewModel.clearActiveModifier() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (rules.isWideBallEnabled || rules.isNoBallEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (rules.isWideBallEnabled) {
                    ScoringButton(
                        text = "WD",
                        colors = colors,
                        enabled = interactive,
                        modifier = Modifier.weight(1f),
                        isHighlighted = activeModifier == "WD",
                        onLongClick = { viewModel.selectModifier("WD", isLongClick = true) },
                        onClick = { viewModel.selectModifier("WD", isLongClick = false) }
                    )
                }
                if (rules.isNoBallEnabled) {
                    ScoringButton(
                        text = "NB",
                        colors = colors,
                        enabled = interactive,
                        modifier = Modifier.weight(1f),
                        isHighlighted = activeModifier == "NB",
                        onLongClick = { viewModel.selectModifier("NB", isLongClick = true) },
                        onClick = { viewModel.selectModifier("NB", isLongClick = false) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1, 2, 3, 4).forEach { score ->
                ScoringButton(
                    text = score.toString(),
                    colors = colors,
                    enabled = interactive,
                    modifier = Modifier.weight(1f),
                    isRun = true,
                    onLongClick = {
                        if (rules.isUnusualRunsAllowed) {
                            viewModel.showDialog(DialogState.UnusualRunsDialog(baseRun = score))
                        } else {
                            viewModel.showDialog(
                                DialogState.Alert(
                                    title = "Rule Restriction",
                                    message = "Unusual runs (5, 7, 8...) are disallowed in custom match rules."
                                )
                            )
                        }
                    },
                    onClick = { viewModel.addRun(score) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ScoringButton(
                text = "6",
                colors = colors,
                enabled = interactive,
                modifier = Modifier.weight(1f),
                isRun = true,
                onLongClick = {
                    if (rules.isUnusualRunsAllowed) {
                        viewModel.showDialog(DialogState.UnusualRunsDialog(baseRun = 6))
                    } else {
                        viewModel.showDialog(
                            DialogState.Alert(
                                title = "Rule Restriction",
                                message = "Unusual runs (5, 7, 8...) are disallowed in custom match rules."
                            )
                        )
                    }
                },
                onClick = { viewModel.addRun(6) }
            )
            ScoringButton(
                text = "W",
                colors = colors,
                enabled = interactive,
                modifier = Modifier.weight(1f),
                isWicket = true,
                onLongClick = {
                    viewModel.showDialog(
                        DialogState.RunOutDialog(
                            isNoBall = (activeModifier == "NB"),
                            isFreeHit = isFreeHitActive,
                            isWide = (activeModifier == "WD")
                        )
                    )
                },
                onClick = { viewModel.addWicket() }
            )
            ScoringButton(
                text = "•",
                colors = colors,
                enabled = interactive,
                modifier = Modifier.weight(1f),
                onLongClick = { selectedRunForRunOut = 0 },
                onClick = { viewModel.addNoRun() }
            )
            ScoringButton(
                text = "≡",
                colors = colors,
                enabled = true,
                modifier = Modifier.weight(1f),
                onClick = onOpenMenu
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScoringButton(
    text: String,
    colors: CricketColorScheme,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isRun: Boolean = false,
    isWicket: Boolean = false,
    isHighlighted: Boolean = false,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val bg = when {
        !enabled -> colors.buttonBg.copy(alpha = 0.5f)
        isHighlighted -> colors.textAccent
        isRun -> colors.textAccent
        isWicket -> colors.wicket
        else -> colors.buttonBg
    }
    val contentColor = when {
        !enabled -> colors.textSecondary.copy(alpha = 0.5f)
        isHighlighted && colors == CricketLightColors -> Color.Black
        isHighlighted -> Color.Black
        isRun && colors == CricketLightColors -> Color.Black
        isRun -> Color.Black
        isWicket -> colors.textPrimary
        else -> colors.textPrimary
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .combinedClickable(
                enabled = enabled,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun HistoryView(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    onBack: () -> Unit
) {
    val ballHistory by viewModel.ballHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.buttonBg)
            ) {
                Text(text = "◀", color = colors.textPrimary, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Ball History",
                color = colors.textAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ballHistory.isEmpty()) {
                item {
                    Text(
                        text = "No history yet.",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp)
                    )
                }
            } else {
                items(ballHistory) { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Over ${entry.over}.${entry.ball}",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row {
                            Text(
                                text = "Team ${entry.team}: ",
                                color = colors.textSecondary,
                                fontSize = 13.sp
                            )
                            Text(
                                text = formatBallEventForDisplay(entry.event),
                                color = colors.textAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MenuContent(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    onSelectScorecard: () -> Unit,
    onSelectHistory: () -> Unit,
    onSelectMatchAnalysis: () -> Unit,
    onClose: () -> Unit
) {
    val secondInningsStarted by viewModel.secondInningsStarted.collectAsState()
    val gameIsOver by viewModel.gameIsOver.collectAsState()
    val isUmpireDecisionPending by viewModel.isUmpireDecisionPending.collectAsState()
    val totalOvers by viewModel.totalOvers.collectAsState()
    val overs by viewModel.overs.collectAsState()
    val runs by viewModel.runs.collectAsState()
    val teamAScore by viewModel.teamAScore.collectAsState()
    val targetScore by viewModel.targetScore.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(colors.surface)
            .padding(16.dp)
            .testTag("menu_sheet"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "More Actions",
            color = colors.textAccent,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Match Scorecard
            MenuButton(
                icon = Icons.Default.BarChart,
                text = "Match Scorecard",
                colors = colors,
                onClick = onSelectScorecard
            )

            // Undo Last Ball
            MenuButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                text = "Undo Last Ball",
                colors = colors,
                enabled = !isUmpireDecisionPending,
                onClick = {
                    viewModel.undoLastBall()
                    onClose()
                }
            )

            MenuButton(
                icon = Icons.Default.BarChart,
                text = "Match Analysis",
                colors = colors,
                onClick = onSelectMatchAnalysis
            )

            MenuButton(
                icon = Icons.AutoMirrored.Filled.List,
                text = "Ball History",
                colors = colors,
                onClick = onSelectHistory
            )

            MenuButton(
                icon = Icons.Default.Tune,
                text = "Match Rules (Gully Settings)",
                colors = colors,
                onClick = {
                    viewModel.showDialog(DialogState.CustomRulesDialog)
                    onClose()
                }
            )

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.buttonBg)
                    .padding(vertical = 4.dp)
            )

            // Switch Inning
            MenuButton(
                icon = Icons.Default.SwapHoriz,
                text = "Switch Inning",
                colors = colors,
                enabled = !secondInningsStarted && !gameIsOver && !isUmpireDecisionPending,
                onClick = {
                    viewModel.switchInning()
                    onClose()
                }
            )

            // Declare Winner (Umpire)
            MenuButton(
                icon = Icons.Default.Gavel,
                text = "Declare Winner",
                colors = colors,
                enabled = !isUmpireDecisionPending,
                onClick = {
                    viewModel.startUmpireDecision()
                    onClose()
                }
            )

            // Exit to Home
            MenuButton(
                icon = Icons.Default.Home,
                text = "Home & Match Setup",
                colors = colors,
                enabled = !isUmpireDecisionPending,
                onClick = {
                    viewModel.exitToHomeScreen()
                    onClose()
                }
            )

            // Reset Game
            MenuButton(
                icon = Icons.Default.RestartAlt,
                text = "Reset Game",
                colors = colors,
                enabled = !isUmpireDecisionPending,
                onClick = {
                    viewModel.showDialog(DialogState.ConfirmReset("Are you sure you want to reset the game and clear all scores?"))
                    onClose()
                }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Close Menu Button
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.textAccent,
                contentColor = if (colors == CricketLightColors) Color.Black else Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(text = "Close Menu", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun MenuButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    colors: CricketColorScheme,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) colors.buttonBg else colors.buttonBg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun DialogRouter(viewModel: CricketViewModel, colors: CricketColorScheme) {
    val dialogState by viewModel.dialogState.collectAsState()
    val state = dialogState ?: return

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .testTag("dialog_overlay"),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surface)
                    .padding(20.dp)
            ) {
                when (state) {
                    is DialogState.SetupOvers -> {
                        var oversText by remember { mutableStateOf(state.defaultVal) }
                        val parsed = oversText.toIntOrNull()
                        val isValid = parsed != null && parsed in 1..50
                        val errorMsg = when {
                            oversText.isBlank() -> "Please enter total overs"
                            parsed == null || parsed < 1 -> "Total overs must be at least 1"
                            parsed > 50 -> "Max 50 overs allowed. More than 50 is not allowed."
                            else -> null
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Setup Innings",
                                color = colors.textAccent,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Enter total overs per innings (1 - 50):",
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            OutlinedTextField(
                                value = oversText,
                                onValueChange = { if (it.all { c -> c.isDigit() }) oversText = it.take(3) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = errorMsg != null,
                                supportingText = {
                                    if (errorMsg != null) {
                                        Text(
                                            text = errorMsg,
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (errorMsg != null) MaterialTheme.colorScheme.error else colors.textAccent,
                                    unfocusedBorderColor = if (errorMsg != null) MaterialTheme.colorScheme.error else colors.buttonBg,
                                    errorBorderColor = MaterialTheme.colorScheme.error,
                                    errorLabelColor = MaterialTheme.colorScheme.error,
                                    focusedTextColor = colors.textPrimary,
                                    unfocusedTextColor = colors.textPrimary
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .testTag("overs_input_field")
                            )

                            if (parsed != null && parsed > 50) {
                                Button(
                                    onClick = { oversText = "50" },
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.wicket, contentColor = Color.White),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text("Set to Max 50 Overs", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    if (parsed != null && parsed in 1..50) {
                                        viewModel.setTotalOvers(parsed)
                                    }
                                },
                                enabled = isValid,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.textAccent,
                                    contentColor = Color.Black,
                                    disabledContainerColor = colors.buttonBg,
                                    disabledContentColor = colors.textSecondary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = "Confirm Overs", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is DialogState.ConfirmReset -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Confirm Operation",
                                color = colors.wicket,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = state.message,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.dismissDialog() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.buttonBg,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Cancel")
                                }
                                Button(
                                    onClick = {
                                        if (state.message.contains("reset", ignoreCase = true)) {
                                            viewModel.resetGame()
                                        } else {
                                            viewModel.confirmUmpireOverride()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.wicket,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = "Confirm", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    is DialogState.Alert -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.title,
                                color = colors.textAccent,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Text(
                                text = state.message,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 20.dp)
                            )

                            Button(
                                onClick = { viewModel.dismissDialog() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.textAccent,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = state.actionText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    is DialogState.UmpireDecision -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Umpire Decision",
                                color = colors.umpire,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.finalizeGame(true, "A") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.teamA,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Team A Wins", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.finalizeGame(true, "B") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.teamB,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Team B Wins", fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.finalizeGame(true, "TIE") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.textAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Match Tied", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { viewModel.cancelUmpireDecision() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.buttonBg,
                                        contentColor = colors.textPrimary
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = "Cancel")
                                }
                            }
                        }
                    }
                    is DialogState.WicketDialog -> {
                        WicketDialogContent(viewModel = viewModel, colors = colors)
                    }
                    is DialogState.RunOutDialog -> {
                        RunOutDialogContent(state = state, viewModel = viewModel, colors = colors)
                    }
                    is DialogState.EditPlayersDialog -> {
                        EditPlayersDialogContent(viewModel = viewModel, colors = colors)
                    }
                    is DialogState.ChangeBowlerDialog -> {
                        ChangeBowlerDialogContent(viewModel = viewModel, colors = colors)
                    }
                    is DialogState.FullScorecardDialog -> {
                        FullScorecardDialogContent(viewModel = viewModel, colors = colors)
                    }
                    is DialogState.RenamePlayerDialog -> {
                        RenamePlayerDialogContent(
                            viewModel = viewModel,
                            colors = colors,
                            playerId = state.playerId,
                            currentName = state.currentName,
                            isBowler = state.isBowler
                        )
                    }
                    is DialogState.CustomRulesDialog -> {
                        CustomRulesDialogContent(viewModel = viewModel, colors = colors)
                    }
                    is DialogState.UnusualRunsDialog -> {
                        UnusualRunsDialogContent(
                            initialBase = state.baseRun,
                            viewModel = viewModel,
                            colors = colors
                        )
                    }
                    is DialogState.LastManStandingPrompt -> {
                        LastManStandingPromptContent(
                            state = state,
                            viewModel = viewModel,
                            colors = colors
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RunOutDialogContent(
    state: DialogState.RunOutDialog,
    viewModel: CricketViewModel,
    colors: CricketColorScheme
) {
    val currentTeam by viewModel.currentTeam.collectAsState()
    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()
    val batCard = if (currentTeam == "A") teamAScorecard else teamBScorecard

    val striker = batCard.batters.find { it.id == batCard.strikerId } ?: Batter(id = "s", name = "Striker")
    val nonStriker = batCard.batters.find { it.id == batCard.nonStrikerId } ?: Batter(id = "ns", name = "Non-Striker")

    val wickets by viewModel.wickets.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val isWillBeAllOut = !rules.isUnlimitedWickets && (wickets + 1 >= rules.maxWickets)

    var completedRuns by remember { mutableStateOf(0) }
    var selectedOutBatterId by remember { mutableStateOf(striker.id) }
    var newBatterName by remember { mutableStateOf("") }
    var nextStrikerIsNewBatter by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = when {
                state.isNoBall -> "NO BALL - RUN OUT"
                state.isFreeHit -> "FREE HIT - RUN OUT"
                else -> "RUN OUT"
            },
            color = colors.wicket,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )

        if (state.isNoBall || state.isFreeHit) {
            Surface(
                color = colors.wicket.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (state.isNoBall) 
                        "No Ball (+1 extra). Batsman cannot be bowled/caught/LBW; only Run Out is valid." 
                    else 
                        "Free Hit ball. Batsman cannot be bowled/caught/LBW; only Run Out is valid.",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        Text(
            text = "Select Player Who Got Run Out:",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                striker.id to "${striker.name} (Striker)",
                nonStriker.id to "${nonStriker.name} (Non-Striker)"
            ).forEach { (bId, bLabel) ->
                val isSelected = selectedOutBatterId == bId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.wicket else colors.buttonBg)
                        .clickable { selectedOutBatterId = bId }
                        .padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bLabel,
                        color = if (isSelected) Color.White else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Text(
            text = "Runs Completed Before Run Out:",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(0, 1, 2, 3, 4).forEach { r ->
                val isSelected = completedRuns == r
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) colors.textAccent else colors.buttonBg)
                        .clickable { completedRuns = r }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$r",
                        color = if (isSelected) Color.Black else colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        if (isWillBeAllOut) {
            Surface(
                color = colors.wicket.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "This wicket will conclude the innings (All Out).",
                        color = colors.wicket,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Text(
                text = "New Batter Name:",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            OutlinedTextField(
                value = newBatterName,
                onValueChange = { newBatterName = it },
                placeholder = { Text("e.g. Batter ${batCard.batters.size + 1}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.textAccent,
                    unfocusedBorderColor = colors.buttonBg,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Text(
                text = "Next Striker (Facing Next Ball):",
                color = colors.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                val remainingName = if (selectedOutBatterId == striker.id) nonStriker.name else striker.name
                listOf(true to "New Batter", false to remainingName).forEach { (isNew, label) ->
                    val isSelected = nextStrikerIsNewBatter == isNew
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.textAccent else colors.buttonBg)
                            .clickable { nextStrikerIsNewBatter = isNew }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.processRunOutDismissal(
                        outBatterId = selectedOutBatterId,
                        completedRuns = completedRuns,
                        newBatterName = if (isWillBeAllOut) "" else newBatterName,
                        nextStrikerIsNewBatter = nextStrikerIsNewBatter,
                        isNoBall = state.isNoBall,
                        isWide = state.isWide
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.wicket, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isWillBeAllOut) "Confirm Run Out (All Out)" else "Confirm Run Out", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WicketDialogContent(viewModel: CricketViewModel, colors: CricketColorScheme) {
    val currentTeam by viewModel.currentTeam.collectAsState()
    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()
    val batCard = if (currentTeam == "A") teamAScorecard else teamBScorecard

    val striker = batCard.batters.find { it.id == batCard.strikerId } ?: Batter(id = "s", name = "Striker")
    val nonStriker = batCard.batters.find { it.id == batCard.nonStrikerId } ?: Batter(id = "ns", name = "Non-Striker")

    val wickets by viewModel.wickets.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val isWillBeAllOut = !rules.isUnlimitedWickets && (wickets + 1 >= rules.maxWickets)

    var selectedDismissal by remember { mutableStateOf("Bowled") }
    var completedRuns by remember { mutableStateOf(0) }
    var selectedOutBatterId by remember { mutableStateOf(striker.id) }
    var newBatterName by remember { mutableStateOf("") }
    var nextStrikerIsNewBatter by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Wicket / Dismissal",
            color = colors.wicket,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Dismissal Type selection
        Text(text = "Dismissal Type:", color = colors.textPrimary, fontSize = 13.sp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("Bowled", "Caught", "Run Out", "LBW", "Stumped", "Hit Wicket").chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { type ->
                        val isSelected = selectedDismissal == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.wicket else colors.buttonBg)
                                .clickable { selectedDismissal = type }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type,
                                color = if (isSelected) Color.White else colors.textPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (selectedDismissal == "Run Out") {
            Text(text = "Runs completed before Run Out:", color = colors.textPrimary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(0, 1, 2, 3, 4).forEach { r ->
                    val isSelected = completedRuns == r
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.textAccent else colors.buttonBg)
                            .clickable { completedRuns = r }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$r",
                            color = if (isSelected) Color.Black else colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Text(text = "Batter Out:", color = colors.textPrimary, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(striker.id to "${striker.name} (Striker)", nonStriker.id to "${nonStriker.name} (Non-Striker)").forEach { (bId, bName) ->
                val isSelected = selectedOutBatterId == bId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) colors.wicket else colors.buttonBg)
                        .clickable { selectedOutBatterId = bId }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = bName,
                        color = if (isSelected) Color.White else colors.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }

        if (isWillBeAllOut) {
            Surface(
                color = colors.wicket.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "This wicket will conclude the innings (All Out).",
                        color = colors.wicket,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            Text(text = "New Batter Name:", color = colors.textPrimary, fontSize = 13.sp)
            OutlinedTextField(
                value = newBatterName,
                onValueChange = { newBatterName = it },
                placeholder = { Text("e.g. Batter ${batCard.batters.size + 1}") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.textAccent,
                    unfocusedBorderColor = colors.buttonBg,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                )
            )

            Text(text = "Next Striker:", color = colors.textPrimary, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val remainingName = if (selectedOutBatterId == striker.id) nonStriker.name else striker.name
                listOf(true to "New Batter", false to remainingName).forEach { (isNew, label) ->
                    val isSelected = nextStrikerIsNewBatter == isNew
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.textAccent else colors.buttonBg)
                            .clickable { nextStrikerIsNewBatter = isNew }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (selectedDismissal == "Run Out") {
                        viewModel.processRunOutDismissal(
                            outBatterId = selectedOutBatterId,
                            completedRuns = completedRuns,
                            newBatterName = if (isWillBeAllOut) "" else newBatterName,
                            nextStrikerIsNewBatter = nextStrikerIsNewBatter,
                            isNoBall = false,
                            isWide = false
                        )
                    } else {
                        viewModel.processWicketDismissal(
                            dismissalType = selectedDismissal,
                            completedRuns = 0,
                            outBatterId = selectedOutBatterId,
                            newBatterName = if (isWillBeAllOut) "" else newBatterName,
                            nextStrikerIsNewBatter = nextStrikerIsNewBatter
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.wicket, contentColor = Color.White),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isWillBeAllOut) "Confirm Wicket (All Out)" else "Confirm Wicket", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EditPlayersDialogContent(viewModel: CricketViewModel, colors: CricketColorScheme) {
    val currentTeam by viewModel.currentTeam.collectAsState()
    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()
    val batCard = if (currentTeam == "A") teamAScorecard else teamBScorecard
    val bowlCard = if (currentTeam == "A") teamBScorecard else teamAScorecard

    val striker = batCard.batters.find { it.id == batCard.strikerId }
    val nonStriker = batCard.batters.find { it.id == batCard.nonStrikerId }
    val bowler = bowlCard.bowlers.find { it.id == bowlCard.currentBowlerId }

    var strikerName by remember { mutableStateOf(striker?.name ?: "") }
    var nonStrikerName by remember { mutableStateOf(nonStriker?.name ?: "") }
    var bowlerName by remember { mutableStateOf(bowler?.name ?: "") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Edit Active Players",
            color = colors.textAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = strikerName,
            onValueChange = { strikerName = it },
            label = { Text("Striker Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.textAccent,
                unfocusedBorderColor = colors.buttonBg,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        OutlinedTextField(
            value = nonStrikerName,
            onValueChange = { nonStrikerName = it },
            label = { Text("Non-Striker Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.textAccent,
                unfocusedBorderColor = colors.buttonBg,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        OutlinedTextField(
            value = bowlerName,
            onValueChange = { bowlerName = it },
            label = { Text("Bowler Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.textAccent,
                unfocusedBorderColor = colors.buttonBg,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.updatePlayerNames(strikerName, nonStrikerName, bowlerName)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.textAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun RenamePlayerDialogContent(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    playerId: String,
    currentName: String,
    isBowler: Boolean
) {
    var nameText by remember { mutableStateOf(currentName) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = if (isBowler) "Rename Bowler" else "Rename Batter",
            color = colors.textAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = nameText,
            onValueChange = { nameText = it },
            label = { Text("Player Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.textAccent,
                unfocusedBorderColor = colors.buttonBg,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.renameSinglePlayer(playerId, nameText, isBowler)
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.textAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Save Name", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ChangeBowlerDialogContent(viewModel: CricketViewModel, colors: CricketColorScheme) {
    val currentTeam by viewModel.currentTeam.collectAsState()
    val teamAScorecard by viewModel.teamAScorecard.collectAsState()
    val teamBScorecard by viewModel.teamBScorecard.collectAsState()
    val bowlCard = if (currentTeam == "A") teamBScorecard else teamAScorecard

    var newBowlerName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Over Complete - Select Bowler",
            color = colors.textAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Quick Select for user-defined bowlers
        if (bowlCard.bowlers.isNotEmpty()) {
            Text(
                text = "Quick Select Bowler:",
                color = colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                bowlCard.bowlers.forEach { bw ->
                    val isCurrent = bw.id == bowlCard.currentBowlerId
                    val ovs = "${bw.oversLegalBalls / 6}.${bw.oversLegalBalls % 6}"
                    Surface(
                        color = if (isCurrent) colors.teamB else colors.buttonBg,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable {
                            viewModel.selectOrAddBowler(bw.name)
                        }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = bw.name,
                                color = if (isCurrent) Color.White else colors.textAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isCurrent) "Last over (${bw.wickets}/${bw.runsConceded})" else "${bw.wickets}/${bw.runsConceded} ($ovs ov)",
                                color = if (isCurrent) Color.White.copy(alpha = 0.85f) else colors.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Add New Bowler:",
            color = colors.textSecondary,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.Start)
        )
        OutlinedTextField(
            value = newBowlerName,
            onValueChange = { newBowlerName = it },
            placeholder = { Text("Enter new bowler name...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.textPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.textAccent,
                unfocusedBorderColor = colors.buttonBg,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (newBowlerName.isNotBlank()) {
                        viewModel.selectOrAddBowler(newBowlerName)
                    }
                },
                enabled = newBowlerName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.textAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Add & Select", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FullScorecardDialogContent(viewModel: CricketViewModel, colors: CricketColorScheme) {
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
    val currentTeam by viewModel.currentTeam.collectAsState()

    var selectedTeamTab by remember { mutableStateOf(currentTeam) } // "A" or "B"
    val card = if (selectedTeamTab == "A") teamAScorecard else teamBScorecard
    val bowlCard = if (selectedTeamTab == "A") teamBScorecard else teamAScorecard
    val displayTeamName = if (selectedTeamTab == "A") teamAName else teamBName

    val teamTotalRuns = if (selectedTeamTab == currentTeam) runs else (if (selectedTeamTab == "A") teamAScore else teamBScore)
    val teamTotalWickets = if (selectedTeamTab == currentTeam) wickets else card.fallOfWickets.size
    val teamOversStr = if (selectedTeamTab == currentTeam) "$overs.$balls" else "Ended"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MATCH SCORECARD",
                color = colors.textAccent,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = { viewModel.dismissDialog() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Team Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.buttonBg)
                .padding(2.dp)
        ) {
            listOf("A" to teamAName, "B" to teamBName).forEach { (tKey, tName) ->
                val isSelected = selectedTeamTab == tKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) colors.surface else Color.Transparent)
                        .clickable { selectedTeamTab = tKey }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tName,
                        color = if (isSelected) colors.textAccent else colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Score Summary
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.surface).padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = displayTeamName, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "$teamTotalRuns/$teamTotalWickets ($teamOversStr Ovs)", color = colors.textAccent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                }
            }

            // Batting Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.buttonBg).padding(vertical = 4.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "BATTER", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text(text = "R", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "B", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "4s", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "6s", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "SR", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                }
            }

            // Batting Rows
            items(card.batters) { batter ->
                val sr = if (batter.balls > 0) String.format("%.1f", batter.runs * 100f / batter.balls) else "0.0"
                val statusStr = if (batter.isOut) batter.dismissalInfo else if (batter.id == card.strikerId || batter.id == card.nonStrikerId) "batting" else "not out"
                val isCurrentStriker = batter.id == card.strikerId

                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${batter.name}${if (isCurrentStriker) " *" else ""}",
                            color = if (isCurrentStriker) colors.textAccent else colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f)
                        )
                        Text(text = "${batter.runs}", color = colors.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text(text = "${batter.balls}", color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text(text = "${batter.fours}", color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text(text = "${batter.sixes}", color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                        Text(text = sr, color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.9f))
                    }
                    Text(
                        text = statusStr,
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Bowling Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().background(colors.buttonBg).padding(vertical = 4.dp, horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "BOWLER", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2f))
                    Text(text = "O", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "R", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "W", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "ECON", color = colors.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }

            // Bowling Rows
            items(bowlCard.bowlers) { bw ->
                val ovStr = "${bw.oversLegalBalls / 6}.${bw.oversLegalBalls % 6}"
                val econ = if (bw.oversLegalBalls > 0) String.format(Locale.US, "%.1f", bw.runsConceded * 6f / bw.oversLegalBalls) else "0.0"
                val isCurrentBW = bw.id == bowlCard.currentBowlerId

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${bw.name}${if (isCurrentBW) " *" else ""}",
                        color = if (isCurrentBW) colors.teamB else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(2f)
                    )
                    Text(text = ovStr, color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "${bw.runsConceded}", color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = "${bw.wickets}", color = colors.wicket, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(0.6f))
                    Text(text = econ, color = colors.textSecondary, fontSize = 12.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }

            // Fall of Wickets Section
            if (card.fallOfWickets.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Fall of Wickets", color = colors.textAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = card.fallOfWickets.joinToString(", ") { "${it.runs}-${it.wicketNumber} (${it.batterName}, ${it.overs} ov)" },
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CustomRulesDialogContent(viewModel: CricketViewModel, colors: CricketColorScheme) {
    val currentRules by viewModel.rules.collectAsState()
    var localRules by remember { mutableStateOf(currentRules) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MATCH RULES",
                color = colors.textAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(onClick = { viewModel.dismissDialog() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        var isRulesValid by remember { mutableStateOf(true) }

        MatchRulesConfigSection(
            rules = localRules,
            onRulesChange = { localRules = it },
            onValidationChange = { isValid, _ -> isRulesValid = isValid },
            colors = colors
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    if (isRulesValid) {
                        val safeRules = localRules.copy(
                            maxWickets = if (localRules.isUnlimitedWickets) localRules.maxWickets else localRules.maxWickets.coerceIn(1, 20)
                        )
                        viewModel.updateRules(safeRules)
                        viewModel.dismissDialog()
                    }
                },
                enabled = isRulesValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textAccent,
                    contentColor = Color.Black,
                    disabledContainerColor = colors.buttonBg,
                    disabledContentColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Apply Rules", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LastManStandingPromptContent(
    state: DialogState.LastManStandingPrompt,
    viewModel: CricketViewModel,
    colors: CricketColorScheme
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Last Man Standing",
            color = colors.textAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = colors.buttonBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Declare ${state.batterName} as Last Man Standing?",
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "All other batting teammates will be declared All Out. ${state.batterName} will continue batting alone as the solo batter until dismissed or the innings concludes.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("cancel_last_man_standing")
            ) {
                Text(text = "Cancel", fontSize = 13.sp)
            }
            Button(
                onClick = { viewModel.declareLastManStanding(state.batterId) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.wicket, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("confirm_last_man_standing")
            ) {
                Text(text = "Declare Solo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun UnusualRunsDialogContent(
    initialBase: Int,
    viewModel: CricketViewModel,
    colors: CricketColorScheme
) {
    var baseRun by remember { mutableStateOf(if (initialBase in listOf(1, 2, 3, 4, 6)) initialBase else 4) }
    var extraRun by remember { mutableStateOf(1) }
    var isDirectCustom by remember { mutableStateOf(false) }
    var customTotal by remember { mutableStateOf(5) }

    val totalRuns = if (isDirectCustom) customTotal else (baseRun + extraRun)
    val eventLabel = if (isDirectCustom) customTotal.toString() else "$baseRun+$extraRun"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Unusual Runs & Overthrows",
            color = colors.textAccent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Record 5, 7, 8 or extra overthrow runs:",
            color = colors.textSecondary,
            fontSize = 12.sp
        )

        // Quick Preset Combinations
        Text(
            text = "Common Combinations:",
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                Triple(5, 4, 1),
                Triple(7, 6, 1),
                Triple(7, 4, 3),
                Triple(8, 4, 4),
                Triple(8, 6, 2),
                Triple(9, 6, 3)
            ).forEach { (total, b, e) ->
                val isSelected = !isDirectCustom && baseRun == b && extraRun == e
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.textAccent else colors.buttonBg)
                        .clickable {
                            isDirectCustom = false
                            baseRun = b
                            extraRun = e
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$total ($b+$e)",
                        color = if (isSelected) Color.Black else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Custom Run Builder Section
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.buttonBg.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Base Runs Completed:",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 4, 6).forEach { b ->
                        val isSelected = !isDirectCustom && baseRun == b
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.textAccent else colors.surface)
                                .clickable {
                                    isDirectCustom = false
                                    baseRun = b
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$b",
                                color = if (isSelected) Color.Black else colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Text(
                    text = "Overthrows / Extra Runs (+):",
                    color = colors.textPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(1, 2, 3, 4).forEach { e ->
                        val isSelected = !isDirectCustom && extraRun == e
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) colors.textAccent else colors.surface)
                                .clickable {
                                    isDirectCustom = false
                                    extraRun = e
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+$e",
                                color = if (isSelected) Color.Black else colors.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Total preview card
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$totalRuns Runs",
                    color = colors.textAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ball notation: $eventLabel • ${if (totalRuns % 2 != 0) "Strike rotates (Odd)" else "Strike retains (Even)"}",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { viewModel.dismissDialog() },
                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonBg, contentColor = colors.textPrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("cancel_unusual_runs")
            ) {
                Text(text = "Cancel", fontSize = 13.sp)
            }
            Button(
                onClick = { viewModel.addUnusualRuns(totalRuns, eventLabel) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.textAccent, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("confirm_unusual_runs")
            ) {
                Text(text = "Score $totalRuns Runs", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MatchRulesConfigSection(
    rules: MatchRules,
    onRulesChange: (MatchRules) -> Unit,
    colors: CricketColorScheme,
    onValidationChange: ((isValid: Boolean, errorMessage: String?) -> Unit)? = null
) {
    var maxPlayersInput by remember { mutableStateOf(rules.maxWickets.toString()) }
    var wicketErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rules.maxWickets) {
        val currentParsed = maxPlayersInput.toIntOrNull()
        if (currentParsed != rules.maxWickets && (currentParsed == null || currentParsed in 1..20)) {
            maxPlayersInput = rules.maxWickets.toString()
            wicketErrorMessage = null
        }
    }

    LaunchedEffect(rules.isUnlimitedWickets, wicketErrorMessage) {
        if (rules.isUnlimitedWickets) {
            onValidationChange?.invoke(true, null)
        } else {
            onValidationChange?.invoke(wicketErrorMessage == null, wicketErrorMessage)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // --- 1. No Ball [Enable / Disable] ---
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "No Ball (NB)",
                    color = colors.textAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (rules.isNoBallEnabled) "NB button will appear on the scoring keypad" else "NB button will be hidden from the scoring keypad",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Enable Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (rules.isNoBallEnabled) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isNoBallEnabled = true)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = rules.isNoBallEnabled,
                            onClick = { onRulesChange(rules.copy(isNoBallEnabled = true)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Enable",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (rules.isNoBallEnabled) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    // Disable Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!rules.isNoBallEnabled) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isNoBallEnabled = false)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = !rules.isNoBallEnabled,
                            onClick = { onRulesChange(rules.copy(isNoBallEnabled = false)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Disable",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!rules.isNoBallEnabled) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // --- 2. Wide Ball [Enable / Disable] ---
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Wide Ball (WD)",
                    color = colors.textAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (rules.isWideBallEnabled) "WD button will appear on the scoring keypad" else "WD button will be hidden from the scoring keypad",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Enable Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (rules.isWideBallEnabled) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isWideBallEnabled = true)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = rules.isWideBallEnabled,
                            onClick = { onRulesChange(rules.copy(isWideBallEnabled = true)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Enable",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (rules.isWideBallEnabled) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    // Disable Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!rules.isWideBallEnabled) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isWideBallEnabled = false)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = !rules.isWideBallEnabled,
                            onClick = { onRulesChange(rules.copy(isWideBallEnabled = false)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Disable",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!rules.isWideBallEnabled) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // --- 3. Wickets Mode [Limited Wicket with text box vs Unlimited Wicket] ---
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wickets & Players",
                    color = colors.textAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                // Limited Wickets Radio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val num = maxPlayersInput.toIntOrNull()
                            when {
                                num == null || num < 1 -> {
                                    wicketErrorMessage = "Wickets must be at least 1"
                                    onRulesChange(rules.copy(isUnlimitedWickets = false, maxWickets = 10))
                                }
                                num > 20 -> {
                                    wicketErrorMessage = "Max 20 wickets allowed. '$num' is not allowed!"
                                    onRulesChange(rules.copy(isUnlimitedWickets = false, maxWickets = 20))
                                }
                                else -> {
                                    wicketErrorMessage = null
                                    onRulesChange(rules.copy(isUnlimitedWickets = false, maxWickets = num))
                                }
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = !rules.isUnlimitedWickets,
                        onClick = {
                            val num = maxPlayersInput.toIntOrNull()
                            when {
                                num == null || num < 1 -> {
                                    wicketErrorMessage = "Wickets must be at least 1"
                                    onRulesChange(rules.copy(isUnlimitedWickets = false, maxWickets = 10))
                                }
                                num > 20 -> {
                                    wicketErrorMessage = "Max 20 wickets allowed. '$num' is not allowed!"
                                    onRulesChange(rules.copy(isUnlimitedWickets = false, maxWickets = 20))
                                }
                                else -> {
                                    wicketErrorMessage = null
                                    onRulesChange(rules.copy(isUnlimitedWickets = false, maxWickets = num))
                                }
                            }
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.textAccent,
                            unselectedColor = colors.textSecondary
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Limited Wicket",
                        color = colors.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = if (!rules.isUnlimitedWickets) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                if (!rules.isUnlimitedWickets) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 28.dp, bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = maxPlayersInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(3)
                                maxPlayersInput = filtered
                                if (filtered.isEmpty()) {
                                    wicketErrorMessage = "Please enter wickets limit (1 to 20)"
                                } else {
                                    val num = filtered.toIntOrNull()
                                    if (num == null || num < 1) {
                                        wicketErrorMessage = "Wickets must be at least 1"
                                    } else if (num > 20) {
                                        wicketErrorMessage = "Max 20 wickets allowed. '$num' is not allowed!"
                                    } else {
                                        wicketErrorMessage = null
                                        onRulesChange(rules.copy(maxWickets = num))
                                    }
                                }
                            },
                            isError = wicketErrorMessage != null,
                            label = { Text("Enter Wickets Limit (Max 20)") },
                            placeholder = { Text("e.g. 6, 10 or 11") },
                            supportingText = {
                                if (wicketErrorMessage != null) {
                                    Text(
                                        text = wicketErrorMessage ?: "",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Text(
                                        text = "Set max wickets allowed (1 to 20)",
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedBorderColor = if (wicketErrorMessage != null) MaterialTheme.colorScheme.error else colors.textAccent,
                                unfocusedBorderColor = if (wicketErrorMessage != null) MaterialTheme.colorScheme.error else colors.buttonBg,
                                errorBorderColor = MaterialTheme.colorScheme.error,
                                errorLabelColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = if (wicketErrorMessage != null) MaterialTheme.colorScheme.error else colors.textAccent,
                                unfocusedLabelColor = colors.textSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wicket_limit_input")
                        )

                        // If user typed > 20, offer quick fix button
                        if (wicketErrorMessage != null && (maxPlayersInput.toIntOrNull() ?: 0) > 20) {
                            Button(
                                onClick = {
                                    maxPlayersInput = "20"
                                    wicketErrorMessage = null
                                    onRulesChange(rules.copy(maxWickets = 20))
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.wicket,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Set to Max 20 Wickets", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick preset buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Quick:",
                                color = colors.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            listOf(6, 8, 10, 11, 15, 20).forEach { presetWickets ->
                                val isSelected = rules.maxWickets == presetWickets && wicketErrorMessage == null
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) colors.textAccent else colors.buttonBg)
                                        .clickable {
                                            maxPlayersInput = presetWickets.toString()
                                            wicketErrorMessage = null
                                            onRulesChange(rules.copy(maxWickets = presetWickets))
                                        }
                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "$presetWickets",
                                        color = if (isSelected) Color.Black else colors.textPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Unlimited Wickets Radio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            wicketErrorMessage = null
                            onRulesChange(rules.copy(isUnlimitedWickets = true))
                        }
                        .padding(vertical = 4.dp)
                ) {
                    RadioButton(
                        selected = rules.isUnlimitedWickets,
                        onClick = {
                            wicketErrorMessage = null
                            onRulesChange(rules.copy(isUnlimitedWickets = true))
                        },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = colors.textAccent,
                            unselectedColor = colors.textSecondary
                        ),
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.padding(start = 6.dp)) {
                        Text(
                            text = "Unlimited Wicket",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (rules.isUnlimitedWickets) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = "Innings continues until all overs are bowled regardless of wickets",
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // --- 4. Last Man Stand Out [Enable / Disable] ---
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Last Man Stand Out",
                    color = colors.textAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "When all teammates are dismissed, single remaining batter can bat alone from both ends",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Enable Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (rules.isLastManStanding) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isLastManStanding = true)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = rules.isLastManStanding,
                            onClick = { onRulesChange(rules.copy(isLastManStanding = true)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Enable",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (rules.isLastManStanding) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    // Disable Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!rules.isLastManStanding) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isLastManStanding = false)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = !rules.isLastManStanding,
                            onClick = { onRulesChange(rules.copy(isLastManStanding = false)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Disable",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!rules.isLastManStanding) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }

        // Card 5: Unusual Runs Rule
        Card(
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "5. Unusual Runs (5, 7, 8...)",
                    color = colors.textAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Allow hold/long-click on scoring buttons to record overthrows or 5, 7, 8 run combinations",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Allowed Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (rules.isUnusualRunsAllowed) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isUnusualRunsAllowed = true)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = rules.isUnusualRunsAllowed,
                            onClick = { onRulesChange(rules.copy(isUnusualRunsAllowed = true)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Allowed",
                            color = if (rules.isUnusualRunsAllowed) colors.textAccent else colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (rules.isUnusualRunsAllowed) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    // Disallowed Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!rules.isUnusualRunsAllowed) colors.buttonBg else Color.Transparent)
                            .clickable { onRulesChange(rules.copy(isUnusualRunsAllowed = false)) }
                            .padding(vertical = 6.dp, horizontal = 6.dp)
                    ) {
                        RadioButton(
                            selected = !rules.isUnusualRunsAllowed,
                            onClick = { onRulesChange(rules.copy(isUnusualRunsAllowed = false)) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.textAccent,
                                unselectedColor = colors.textSecondary
                            ),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "Disallowed",
                            color = if (!rules.isUnusualRunsAllowed) colors.textAccent else colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (!rules.isUnusualRunsAllowed) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSetupScreen(
    viewModel: CricketViewModel,
    colors: CricketColorScheme,
    onOpenHistory: () -> Unit = {}
) {
    var teamA by remember { mutableStateOf("") }
    var teamB by remember { mutableStateOf("") }
    var oversText by remember { mutableStateOf("10") }
    var tossWinner by remember { mutableStateOf("A") } // "A" or "B"
    var tossDecision by remember { mutableStateOf("bat") } // "bat" or "bowl"

    val parsedOvers = oversText.toIntOrNull()
    val isOversValid = parsedOvers != null && parsedOvers in 1..50
    val oversErrorMessage = when {
        oversText.isBlank() -> "Please enter total match overs"
        parsedOvers == null || parsedOvers < 1 -> "Total overs must be at least 1"
        parsedOvers > 50 -> "Max 50 overs allowed. More than 50 is not allowed."
        else -> null
    }

    val rulesState by viewModel.rules.collectAsState()
    var matchRules by remember { mutableStateOf(rulesState) }
    var isRulesExpanded by remember { mutableStateOf(true) }
    var isRulesValid by remember { mutableStateOf(true) }
    var rulesErrorMessage by remember { mutableStateOf<String?>(null) }

    val hasSavedActiveMatch by viewModel.hasSavedActiveMatch.collectAsState()
    val savedTeamAName by viewModel.teamAName.collectAsState()
    val savedTeamBName by viewModel.teamBName.collectAsState()
    val savedTotalOvers by viewModel.totalOvers.collectAsState()
    val savedRuns by viewModel.runs.collectAsState()
    val savedWickets by viewModel.wickets.collectAsState()
    val savedOvers by viewModel.overs.collectAsState()
    val savedBalls by viewModel.balls.collectAsState()
    val savedCurrentTeam by viewModel.currentTeam.collectAsState()
    val savedSecondInningsStarted by viewModel.secondInningsStarted.collectAsState()
    val savedTargetScore by viewModel.targetScore.collectAsState()
    val savedGameIsOver by viewModel.gameIsOver.collectAsState()
    val historyList by viewModel.matchHistoryList.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val statusPadding = WindowInsets.statusBars.asPaddingValues()
    val navPadding = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = statusPadding.calculateTopPadding(), bottom = navPadding.calculateBottomPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header / App Branding & History Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "YOUVA GULLY CRICKET",
                        color = colors.textAccent,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "Set up your match & custom gully rules",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleTheme() },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(colors.surface)
                            .testTag("home_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (themeMode == "dark") Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme Toggle",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Button(
                        onClick = onOpenHistory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surface,
                            contentColor = colors.textAccent
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Match History",
                            tint = colors.textAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "History (${historyList.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- Resume In-Progress Match Card (If active game exists) ---
            if (hasSavedActiveMatch) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = colors.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.5.dp, colors.textAccent.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "IN-PROGRESS MATCH",
                                    color = colors.textAccent,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Surface(
                                color = colors.buttonBg,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (savedGameIsOver) "Match Ended" else (if (savedSecondInningsStarted) "2nd Innings" else "1st Innings"),
                                    color = colors.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Do you want to continue this match?",
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        // Match overview preview
                        Surface(
                            color = colors.buttonBg.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "$savedTeamAName vs $savedTeamBName ($savedTotalOvers Overs)",
                                    color = colors.textPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                val currentBatTeam = if (savedCurrentTeam == "A") savedTeamAName else savedTeamBName
                                Text(
                                    text = "Current: $currentBatTeam • $savedRuns/$savedWickets in $savedOvers.$savedBalls ov" +
                                            if (savedSecondInningsStarted && savedTargetScore > 0) " (Target: $savedTargetScore)" else "",
                                    color = colors.textAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Continue / Discard Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.discardSavedActiveMatchAndReset() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.buttonBg,
                                    contentColor = colors.wicket
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "Discard", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.continueActiveMatch() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.textAccent,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1.3f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Continue",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Continue Match",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Team Names Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Team Profiles",
                        color = colors.textAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = teamA,
                        onValueChange = { teamA = it },
                        label = { Text("Team A Name") },
                        placeholder = { Text("e.g. Warriors") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("team_a_name_input"),
                        textStyle = TextStyle(color = colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.teamA,
                            unfocusedBorderColor = colors.buttonBg,
                            focusedLabelColor = colors.teamA,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    OutlinedTextField(
                        value = teamB,
                        onValueChange = { teamB = it },
                        label = { Text("Team B Name") },
                        placeholder = { Text("e.g. Titans") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("team_b_name_input"),
                        textStyle = TextStyle(color = colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.teamB,
                            unfocusedBorderColor = colors.buttonBg,
                            focusedLabelColor = colors.teamB,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )
                }
            }

            // Overs Selection Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Match Duration",
                        color = colors.textAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Select total overs per innings:",
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )

                    // Quick Chips for Overs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("5", "10", "20", "50").forEach { item ->
                            val isSelected = oversText == item
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.textAccent else colors.buttonBg)
                                    .clickable { oversText = item }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item,
                                    color = if (isSelected) Color.Black else colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Custom overs input
                    OutlinedTextField(
                        value = oversText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) oversText = it.take(3) },
                        label = { Text("Custom Overs (1 - 50)") },
                        isError = oversErrorMessage != null,
                        supportingText = {
                            if (oversErrorMessage != null) {
                                Text(
                                    text = oversErrorMessage ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("custom_overs_input"),
                        textStyle = TextStyle(color = colors.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (oversErrorMessage != null) MaterialTheme.colorScheme.error else colors.textAccent,
                            unfocusedBorderColor = if (oversErrorMessage != null) MaterialTheme.colorScheme.error else colors.buttonBg,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorLabelColor = MaterialTheme.colorScheme.error,
                            focusedLabelColor = if (oversErrorMessage != null) MaterialTheme.colorScheme.error else colors.textAccent,
                            unfocusedLabelColor = colors.textSecondary,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary
                        )
                    )

                    if (oversErrorMessage != null && (parsedOvers ?: 0) > 50) {
                        Button(
                            onClick = { oversText = "50" },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.wicket,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Set to Max 50 Overs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Toss and Decision Card
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Toss Details",
                        color = colors.textAccent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Who won the toss?",
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val displayA = if (teamA.isBlank()) "Team A" else teamA
                        val displayB = if (teamB.isBlank()) "Team B" else teamB

                        listOf("A" to displayA, "B" to displayB).forEach { (key, display) ->
                            val isSelected = tossWinner == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) (if (key == "A") colors.teamA else colors.teamB) else colors.buttonBg)
                                    .clickable { tossWinner = key }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = display,
                                    color = if (isSelected) Color.White else colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Toss winner selected to:",
                        color = colors.textPrimary,
                        fontSize = 13.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("bat" to "BATTING", "bowl" to "BOWLING").forEach { (key, label) ->
                            val isSelected = tossDecision == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.textAccent else colors.buttonBg)
                                    .clickable { tossDecision = key }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.Black else colors.textPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // --- Custom Match Rules Card ---
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().testTag("custom_rules_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRulesExpanded = !isRulesExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Custom Rules",
                                tint = colors.textAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Custom Gully Rules",
                                color = colors.textAccent,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (isRulesExpanded) "Hide Rules" else "Show Rules",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (isRulesExpanded) {
                        MatchRulesConfigSection(
                            rules = matchRules,
                            onRulesChange = { matchRules = it },
                            onValidationChange = { isValid, error ->
                                isRulesValid = isValid
                                rulesErrorMessage = error
                            },
                            colors = colors
                        )
                    } else {
                        // Summary chip preview
                        Text(
                            text = buildAnnotatedString {
                                append("NB: ${if (matchRules.isNoBallEnabled) "Enabled" else "Disabled"} • WD: ${if (matchRules.isWideBallEnabled) "Enabled" else "Disabled"} • ${if (matchRules.isUnlimitedWickets) "Unlimited Wickets" else "${matchRules.maxWickets} Wickets"} • LMS: ${if (matchRules.isLastManStanding) "ON" else "OFF"} • ")
                                withStyle(
                                    SpanStyle(
                                        color = colors.textAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append("Unusual Runs: ${if (matchRules.isUnusualRunsAllowed) "ON" else "OFF"}")
                                }
                            },
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val combinedError = when {
                !isOversValid -> oversErrorMessage
                !isRulesValid -> rulesErrorMessage
                else -> null
            }
            if (combinedError != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Validation Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = combinedError,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Start Match Button
            Button(
                onClick = {
                    if (isRulesValid && isOversValid) {
                        val finalOvers = (oversText.toIntOrNull() ?: 10).coerceIn(1, 50)
                        val finalTeamA = if (teamA.isBlank()) "Team A" else teamA
                        val finalTeamB = if (teamB.isBlank()) "Team B" else teamB
                        val safeMatchRules = matchRules.copy(
                            maxWickets = if (matchRules.isUnlimitedWickets) matchRules.maxWickets else matchRules.maxWickets.coerceIn(1, 20)
                        )
                        viewModel.startMatch(
                            teamA = finalTeamA,
                            teamB = finalTeamB,
                            overs = finalOvers,
                            winner = tossWinner,
                            decision = tossDecision,
                            customRules = safeMatchRules
                        )
                    }
                },
                enabled = isRulesValid && isOversValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textAccent,
                    contentColor = Color.Black,
                    disabledContainerColor = colors.buttonBg,
                    disabledContentColor = colors.textSecondary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("start_match_button")
            ) {
                Text(
                    text = "START MATCH",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
