package com.youva.gullycricket.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.youva.gullycricket.model.BallHistoryEntry
import com.youva.gullycricket.model.MatchRules
import com.youva.gullycricket.model.RunProgressPoint
import com.youva.gullycricket.model.TeamScorecard
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object CricketPdfGenerator {

    data class MatchPdfData(
        val teamAName: String,
        val teamBName: String,
        val teamAScore: Int,
        val teamBScore: Int,
        val liveRuns: Int,
        val liveWickets: Int,
        val liveOvers: Int,
        val liveBalls: Int,
        val totalOvers: Int,
        val currentTeam: String,
        val secondInningsStarted: Boolean,
        val gameIsOver: Boolean,
        val targetScore: Int,
        val tossWinner: String,
        val tossDecision: String,
        val teamAScorecard: TeamScorecard,
        val teamBScorecard: TeamScorecard,
        val rules: MatchRules,
        val teamARunProgress: List<RunProgressPoint> = emptyList(),
        val teamBRunProgress: List<RunProgressPoint> = emptyList(),
        val teamARunsPerOver: List<Int> = emptyList(),
        val teamBRunsPerOver: List<Int> = emptyList(),
        val ballHistory: List<BallHistoryEntry> = emptyList()
    )

    fun generateAndSharePdf(context: Context, data: MatchPdfData) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Colors Palette
            val cHeaderBg = Color.rgb(15, 37, 55)       // Deep Slate / Midnight Navy #0F2537
            val cHeaderAccent = Color.rgb(234, 138, 0)   // Amber Gold #EA8A00
            val cTeamA = Color.rgb(37, 99, 235)          // Royal Blue #2563EB
            val cTeamB = Color.rgb(217, 119, 6)          // Amber Orange #D97706
            val cDarkText = Color.rgb(15, 23, 42)        // Slate 900 #0F172A
            val cGrayText = Color.rgb(100, 116, 139)     // Slate 500 #64748B
            val cLightBg = Color.rgb(248, 250, 252)      // Slate 50 #F8FAFC
            val cBorder = Color.rgb(226, 232, 240)       // Slate 200 #E2E8F0
            val cTableHead = Color.rgb(237, 242, 247)    // Clean header fill
            val cRowAlt = Color.rgb(250, 252, 255)       // Soft alternating row
            val cRedWicket = Color.rgb(220, 38, 38)      // Red #DC2626
            val cGreenWin = Color.rgb(16, 150, 80)       // Emerald #109650

            // Paints
            val paint = Paint().apply { isAntiAlias = true }
            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 18f
                color = Color.WHITE
            }
            val subtitlePaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9.5f
                color = Color.rgb(210, 225, 240)
            }
            val boldTextPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = 9.5f
                color = cDarkText
            }
            val regularTextPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 9f
                color = cDarkText
            }
            val smallTextPaint = Paint().apply {
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textSize = 8f
                color = cGrayText
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateStr = dateFormat.format(Date())

            // Determine First vs Second Innings Team & Toss Info
            val isTossBatting = data.tossDecision.startsWith("bat", ignoreCase = true)
            val isTeamAWinner = data.tossWinner.equals("A", ignoreCase = true) || data.tossWinner.equals(data.teamAName, ignoreCase = true)
            val isTeamBWinner = data.tossWinner.equals("B", ignoreCase = true) || data.tossWinner.equals(data.teamBName, ignoreCase = true)

            val tossWinKey = if (isTeamBWinner) "B" else "A"
            val tossWinName = when {
                isTeamAWinner -> data.teamAName
                isTeamBWinner -> data.teamBName
                data.tossWinner.isNotBlank() -> data.tossWinner
                else -> ""
            }

            val firstBattingTeamKey = if (data.tossDecision.isNotBlank()) {
                if (isTossBatting) tossWinKey else (if (tossWinKey == "A") "B" else "A")
            } else {
                if (data.currentTeam.isNotBlank() && !data.secondInningsStarted) data.currentTeam else "A"
            }

            val team1Key = firstBattingTeamKey
            val team2Key = if (team1Key == "A") "B" else "A"

            val team1Name = if (team1Key == "A") data.teamAName else data.teamBName
            val team2Name = if (team2Key == "A") data.teamAName else data.teamBName

            val team1Card = if (team1Key == "A") data.teamAScorecard else data.teamBScorecard
            val team2Card = if (team2Key == "A") data.teamAScorecard else data.teamBScorecard

            val isTeam1Current = data.currentTeam == team1Key && !data.secondInningsStarted
            val team1Runs = if (isTeam1Current) data.liveRuns else (if (team1Key == "A") data.teamAScore else data.teamBScore)
            val team1Wickets = if (isTeam1Current) data.liveWickets else team1Card.fallOfWickets.size
            val team1Overs = if (isTeam1Current) "${data.liveOvers}.${data.liveBalls}" else "${data.totalOvers}"

            val isTeam2Current = data.currentTeam == team2Key && data.secondInningsStarted
            val team2Runs = if (isTeam2Current) data.liveRuns else (if (team2Key == "A") data.teamAScore else data.teamBScore)
            val team2Wickets = if (isTeam2Current) data.liveWickets else team2Card.fallOfWickets.size
            val team2Overs = if (isTeam2Current) "${data.liveOvers}.${data.liveBalls}" else if (data.gameIsOver && data.secondInningsStarted) "Ended" else "Yet to bat"

            // Match Result Text
            val resultStatusText = when {
                data.gameIsOver -> {
                    if (data.secondInningsStarted) {
                        if (team2Runs >= data.targetScore && data.targetScore > 0) {
                            if (data.rules.isUnlimitedWickets) {
                                "$team2Name won the game"
                            } else {
                                val remainingWickets = max(1, data.rules.maxWickets - team2Wickets)
                                "$team2Name won by $remainingWickets wicket${if (remainingWickets > 1) "s" else ""}"
                            }
                        } else if (team2Runs == team1Runs && data.targetScore > 0) {
                            "Match Tied ($team1Runs - $team2Runs)"
                        } else {
                            val diff = team1Runs - team2Runs
                            "$team1Name won by $diff runs"
                        }
                    } else {
                        "1st Innings completed ($team1Name: $team1Runs runs)"
                    }
                }
                data.secondInningsStarted -> {
                    val need = data.targetScore - data.liveRuns
                    val totalBalls = data.totalOvers * 6
                    val remBalls = max(0, totalBalls - (data.liveOvers * 6 + data.liveBalls))
                    "$team2Name need $need runs in $remBalls balls"
                }
                else -> "In Progress - $team1Name Batting"
            }

            fun drawHeaderBanner(
                canvas: Canvas,
                title: String,
                subtitle: String,
                showMatchBadge: Boolean = true,
                bannerWidth: Float = pageWidth.toFloat(),
                badgeCustomText: String? = null
            ) {
                // Header Background
                paint.color = cHeaderBg
                paint.style = Paint.Style.FILL
                val bannerH = if (bannerWidth > 600f) 56f else 72f
                canvas.drawRect(0f, 0f, bannerWidth, bannerH, paint)

                // Accent Line
                paint.color = cHeaderAccent
                canvas.drawRect(0f, bannerH, bannerWidth, bannerH + 3f, paint)

                // App / Document Title
                canvas.drawText(title, 28f, if (bannerWidth > 600f) 25f else 32f, titlePaint.apply { textSize = if (bannerWidth > 600f) 15f else 17f; color = Color.WHITE })
                canvas.drawText(subtitle, 28f, if (bannerWidth > 600f) 41f else 48f, subtitlePaint.apply { textSize = if (bannerWidth > 600f) 8.5f else 9.5f })
                if (bannerWidth <= 600f) {
                    canvas.drawText("Generated: $dateStr", 28f, 62f, smallTextPaint.apply { color = Color.rgb(180, 205, 230) })
                }

                if (showMatchBadge || badgeCustomText != null) {
                    val statusBg = if (badgeCustomText != null) cHeaderAccent else if (data.gameIsOver) cGreenWin else cHeaderAccent
                    paint.color = statusBg
                    val badgeText = badgeCustomText ?: (if (data.gameIsOver) "COMPLETED" else "LIVE MATCH")
                    val badgeW = boldTextPaint.measureText(badgeText) + 20f
                    val badgeRect = RectF(bannerWidth - 28f - badgeW, if (bannerWidth > 600f) 16f else 24f, bannerWidth - 28f, if (bannerWidth > 600f) 42f else 50f)
                    canvas.drawRoundRect(badgeRect, 13f, 13f, paint)
                    canvas.drawText(badgeText, bannerWidth - 28f - badgeW + 10f, if (bannerWidth > 600f) 33f else 41f, boldTextPaint.apply { color = Color.WHITE; textSize = 9f })
                }
            }

            fun drawFooter(
                canvas: Canvas,
                pageNum: Int,
                totalPages: Int,
                customWidth: Float = pageWidth.toFloat(),
                customHeight: Float = pageHeight.toFloat()
            ) {
                paint.color = cBorder
                canvas.drawLine(28f, customHeight - 26f, customWidth - 28f, customHeight - 26f, paint)
                canvas.drawText("Youva Gully Cricket • Official Match & Analytics Report", 28f, customHeight - 12f, smallTextPaint)
                val pageStr = "Page $pageNum of $totalPages"
                val pageW = smallTextPaint.measureText(pageStr)
                canvas.drawText(pageStr, customWidth - 28f - pageW, customHeight - 12f, smallTextPaint)
            }

            // ================= PAGE 1: MATCH OVERVIEW & 1ST INNINGS =================
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            var canvas: Canvas = page1.canvas

            drawHeaderBanner(canvas, "YOUVA GULLY CRICKET", "OFFICIAL MATCH REPORT & SCORECARD")

            var currentY = 90f

            // 1. Match Summary Box
            val summaryBoxRect = RectF(28f, currentY, (pageWidth - 28).toFloat(), currentY + 76f)
            paint.color = cLightBg
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(summaryBoxRect, 8f, 8f, paint)
            strokePaint.color = cBorder
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(summaryBoxRect, 8f, 8f, strokePaint)

            // Match Info Content - Toss Winner displayed in simple text & small size
            val tossStr = when {
                tossWinName.isNotEmpty() && data.tossDecision.isNotBlank() -> {
                    val decision = if (isTossBatting) "bat" else "bowl"
                    "Toss: $tossWinName won toss & opted to $decision"
                }
                tossWinName.isNotEmpty() -> "Toss: Won by $tossWinName"
                else -> "Toss: Not recorded"
            }

            canvas.drawText("MATCH: $team1Name  vs  $team2Name (${data.totalOvers} Overs)", 42f, currentY + 20f, boldTextPaint.apply { color = cDarkText; textSize = 11f })
            canvas.drawText(tossStr, 42f, currentY + 36f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
            canvas.drawText("RESULT: $resultStatusText", 42f, currentY + 54f, boldTextPaint.apply { color = if (data.gameIsOver) cGreenWin else cTeamA; textSize = 10f })

            // Scores on Right side of Box
            val scoreBoxRight = pageWidth - 42f
            val score1Brief = "$team1Name: $team1Runs/$team1Wickets ($team1Overs ov)"
            val score1BW = boldTextPaint.measureText(score1Brief)
            canvas.drawText(score1Brief, scoreBoxRight - score1BW, currentY + 28f, boldTextPaint.apply { color = cDarkText; textSize = 10f })

            val score2Brief = if (data.secondInningsStarted) "$team2Name: $team2Runs/$team2Wickets ($team2Overs ov)" else "$team2Name: Yet to Bat"
            val score2BW = regularTextPaint.measureText(score2Brief)
            canvas.drawText(score2Brief, scoreBoxRight - score2BW, currentY + 48f, regularTextPaint.apply { color = cGrayText; textSize = 9.5f })

            currentY += 90f

            // 2. 1st Innings Header Bar
            paint.color = cHeaderBg
            canvas.drawRoundRect(RectF(28f, currentY, (pageWidth - 28).toFloat(), currentY + 24f), 5f, 5f, paint)
            canvas.drawText("1ST INNINGS — $team1Name", 38f, currentY + 16f, boldTextPaint.apply { color = Color.WHITE; textSize = 10.5f })
            val score1Str = "$team1Runs/$team1Wickets ($team1Overs ov)"
            val score1Width = boldTextPaint.measureText(score1Str)
            canvas.drawText(score1Str, (pageWidth - 38 - score1Width), currentY + 16f, boldTextPaint.apply { color = cHeaderAccent; textSize = 10.5f })

            currentY += 30f

            // Batting Table Header
            paint.color = cTableHead
            canvas.drawRect(28f, currentY, (pageWidth - 28).toFloat(), currentY + 18f, paint)
            canvas.drawText("BATSMAN", 34f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("DISMISSAL / STATUS", 160f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("R", 365f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("B", 405f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("4s", 445f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("6s", 485f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("SR", 525f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })

            currentY += 20f

            // Batting Rows
            if (team1Card.batters.isEmpty()) {
                canvas.drawText("No batting records available", 34f, currentY + 12f, smallTextPaint)
                currentY += 18f
            } else {
                team1Card.batters.forEachIndexed { idx, b ->
                    if (idx % 2 == 1) {
                        paint.color = cRowAlt
                        canvas.drawRect(28f, currentY - 2f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    }
                    val status = if (b.isOut) b.dismissalInfo else "not out"
                    val sr = if (b.balls > 0) String.format(Locale.US, "%.1f", (b.runs * 100f / b.balls)) else "0.0"

                    canvas.drawText(b.name.take(20), 34f, currentY + 11f, boldTextPaint.apply { color = cDarkText; textSize = 9f })
                    canvas.drawText(status.take(30), 160f, currentY + 11f, if (b.isOut) smallTextPaint.apply { color = cRedWicket } else smallTextPaint.apply { color = cGrayText })
                    canvas.drawText("${b.runs}", 365f, currentY + 11f, boldTextPaint.apply { color = cDarkText; textSize = 9.5f })
                    canvas.drawText("${b.balls}", 405f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${b.fours}", 445f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${b.sixes}", 485f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText(sr, 525f, currentY + 11f, regularTextPaint.apply { textSize = 9f })

                    paint.color = cBorder
                    canvas.drawLine(28f, currentY + 16f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    currentY += 18f
                }
            }

            // Extras & Total
            val totalExtras1 = team1Card.extrasWides + team1Card.extrasNoBalls + team1Card.extrasByes
            paint.color = cLightBg
            canvas.drawRect(28f, currentY, (pageWidth - 28).toFloat(), currentY + 18f, paint)
            val extras1Str = "Extras: $totalExtras1 (wd ${team1Card.extrasWides}, nb ${team1Card.extrasNoBalls}, b/lb ${team1Card.extrasByes})"
            canvas.drawText(extras1Str, 34f, currentY + 13f, regularTextPaint.apply { textSize = 8.5f })
            val totStr = "Total: $team1Runs/$team1Wickets in $team1Overs ov"
            val totW = boldTextPaint.measureText(totStr)
            canvas.drawText(totStr, pageWidth - 34f - totW, currentY + 13f, boldTextPaint.apply { color = cDarkText; textSize = 9f })
            currentY += 24f

            // 3. Innings 1 Bowling Table Header
            paint.color = cTableHead
            canvas.drawRect(28f, currentY, (pageWidth - 28).toFloat(), currentY + 18f, paint)
            canvas.drawText("BOWLER", 34f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("OVERS", 365f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("RUNS", 405f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
            canvas.drawText("WKTS", 445f, currentY + 13f, boldTextPaint.apply { color = cRedWicket; textSize = 8.5f })
            canvas.drawText("ECON", 500f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })

            currentY += 20f

            val team2Bowlers = team2Card.bowlers
            if (team2Bowlers.isEmpty()) {
                canvas.drawText("No bowling records available", 34f, currentY + 12f, smallTextPaint)
                currentY += 18f
            } else {
                team2Bowlers.forEachIndexed { idx, bw ->
                    if (idx % 2 == 1) {
                        paint.color = cRowAlt
                        canvas.drawRect(28f, currentY - 2f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    }
                    val ov = "${bw.oversLegalBalls / 6}.${bw.oversLegalBalls % 6}"
                    val econ = if (bw.oversLegalBalls > 0) String.format(Locale.US, "%.1f", (bw.runsConceded * 6f / bw.oversLegalBalls)) else "0.0"

                    canvas.drawText(bw.name.take(24), 34f, currentY + 11f, boldTextPaint.apply { color = cDarkText; textSize = 9f })
                    canvas.drawText(ov, 365f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${bw.runsConceded}", 405f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${bw.wickets}", 445f, currentY + 11f, boldTextPaint.apply { color = cRedWicket; textSize = 9.5f })
                    canvas.drawText(econ, 500f, currentY + 11f, regularTextPaint.apply { textSize = 9f })

                    paint.color = cBorder
                    canvas.drawLine(28f, currentY + 16f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    currentY += 18f
                }
            }

            // 4. Fall of Wickets
            if (team1Card.fallOfWickets.isNotEmpty()) {
                currentY += 8f
                canvas.drawText("Fall of Wickets:", 34f, currentY + 10f, boldTextPaint.apply { color = cHeaderBg; textSize = 9f })
                currentY += 14f
                val fowText = team1Card.fallOfWickets.joinToString(", ") { "${it.runs}-${it.wicketNumber} (${it.batterName}, ${it.overs} ov)" }
                canvas.drawText(fowText.take(100), 34f, currentY + 10f, smallTextPaint)
                currentY += 18f
            }

            drawFooter(canvas, 1, 4)
            pdfDocument.finishPage(page1)

            // ================= PAGE 2: 2ND INNINGS & MATCH ANALYSIS =================
            val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = pdfDocument.startPage(pageInfo2)
            canvas = page2.canvas

            drawHeaderBanner(canvas, "2ND INNINGS & ANALYTICS", "OFFICIAL MATCH REPORT & SCORECARD", showMatchBadge = false)

            currentY = 90f

            // 1. Innings 2 Header Bar
            paint.color = cHeaderBg
            canvas.drawRoundRect(RectF(28f, currentY, (pageWidth - 28).toFloat(), currentY + 24f), 5f, 5f, paint)
            canvas.drawText("2ND INNINGS — $team2Name", 38f, currentY + 16f, boldTextPaint.apply { color = Color.WHITE; textSize = 10.5f })
            val score2Str = if (data.secondInningsStarted) "$team2Runs/$team2Wickets ($team2Overs ov)" else "Yet to Bat"
            val score2Width = boldTextPaint.measureText(score2Str)
            canvas.drawText(score2Str, (pageWidth - 38 - score2Width), currentY + 16f, boldTextPaint.apply { color = cHeaderAccent; textSize = 10.5f })

            currentY += 30f

            if (data.secondInningsStarted) {
                // Innings 2 Batting Table Header
                paint.color = cTableHead
                canvas.drawRect(28f, currentY, (pageWidth - 28).toFloat(), currentY + 18f, paint)
                canvas.drawText("BATSMAN", 34f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("DISMISSAL / STATUS", 160f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("R", 365f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("B", 405f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("4s", 445f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("6s", 485f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("SR", 525f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })

                currentY += 20f

                // Batting Rows
                team2Card.batters.forEachIndexed { idx, b ->
                    if (idx % 2 == 1) {
                        paint.color = cRowAlt
                        canvas.drawRect(28f, currentY - 2f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    }
                    val status = if (b.isOut) b.dismissalInfo else "not out"
                    val sr = if (b.balls > 0) String.format(Locale.US, "%.1f", (b.runs * 100f / b.balls)) else "0.0"

                    canvas.drawText(b.name.take(20), 34f, currentY + 11f, boldTextPaint.apply { color = cDarkText; textSize = 9f })
                    canvas.drawText(status.take(30), 160f, currentY + 11f, if (b.isOut) smallTextPaint.apply { color = cRedWicket } else smallTextPaint.apply { color = cGrayText })
                    canvas.drawText("${b.runs}", 365f, currentY + 11f, boldTextPaint.apply { color = cDarkText; textSize = 9.5f })
                    canvas.drawText("${b.balls}", 405f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${b.fours}", 445f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${b.sixes}", 485f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText(sr, 525f, currentY + 11f, regularTextPaint.apply { textSize = 9f })

                    paint.color = cBorder
                    canvas.drawLine(28f, currentY + 16f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    currentY += 18f
                }

                val totalExtras2 = team2Card.extrasWides + team2Card.extrasNoBalls + team2Card.extrasByes
                paint.color = cLightBg
                canvas.drawRect(28f, currentY, (pageWidth - 28).toFloat(), currentY + 18f, paint)
                val extras2Str = "Extras: $totalExtras2 (wd ${team2Card.extrasWides}, nb ${team2Card.extrasNoBalls}, b/lb ${team2Card.extrasByes})"
                canvas.drawText(extras2Str, 34f, currentY + 13f, regularTextPaint.apply { textSize = 8.5f })
                val totStr2 = "Total: $team2Runs/$team2Wickets in $team2Overs ov"
                val totW2 = boldTextPaint.measureText(totStr2)
                canvas.drawText(totStr2, pageWidth - 34f - totW2, currentY + 13f, boldTextPaint.apply { color = cDarkText; textSize = 9f })
                currentY += 24f

                // Bowling Rows (Team 1 Bowlers in Innings 2)
                paint.color = cTableHead
                canvas.drawRect(28f, currentY, (pageWidth - 28).toFloat(), currentY + 18f, paint)
                canvas.drawText("BOWLER", 34f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("OVERS", 365f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("RUNS", 405f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                canvas.drawText("WKTS", 445f, currentY + 13f, boldTextPaint.apply { color = cRedWicket; textSize = 8.5f })
                canvas.drawText("ECON", 500f, currentY + 13f, boldTextPaint.apply { color = cHeaderBg; textSize = 8.5f })
                currentY += 20f

                team1Card.bowlers.forEachIndexed { idx, bw ->
                    if (idx % 2 == 1) {
                        paint.color = cRowAlt
                        canvas.drawRect(28f, currentY - 2f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    }
                    val ov = "${bw.oversLegalBalls / 6}.${bw.oversLegalBalls % 6}"
                    val econ = if (bw.oversLegalBalls > 0) String.format(Locale.US, "%.1f", (bw.runsConceded * 6f / bw.oversLegalBalls)) else "0.0"

                    canvas.drawText(bw.name.take(24), 34f, currentY + 11f, boldTextPaint.apply { color = cDarkText; textSize = 9f })
                    canvas.drawText(ov, 365f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${bw.runsConceded}", 405f, currentY + 11f, regularTextPaint.apply { textSize = 9f })
                    canvas.drawText("${bw.wickets}", 445f, currentY + 11f, boldTextPaint.apply { color = cRedWicket; textSize = 9.5f })
                    canvas.drawText(econ, 500f, currentY + 11f, regularTextPaint.apply { textSize = 9f })

                    paint.color = cBorder
                    canvas.drawLine(28f, currentY + 16f, (pageWidth - 28).toFloat(), currentY + 16f, paint)
                    currentY += 18f
                }

                if (team2Card.fallOfWickets.isNotEmpty()) {
                    currentY += 6f
                    canvas.drawText("Fall of Wickets:", 34f, currentY + 10f, boldTextPaint.apply { color = cHeaderBg; textSize = 9f })
                    currentY += 14f
                    val fowText = team2Card.fallOfWickets.joinToString(", ") { "${it.runs}-${it.wicketNumber} (${it.batterName}, ${it.overs} ov)" }
                    canvas.drawText(fowText.take(100), 34f, currentY + 10f, smallTextPaint)
                    currentY += 18f
                }
            } else {
                canvas.drawText("Second innings has not commenced yet.", 34f, currentY + 12f, smallTextPaint)
                currentY += 24f
            }

            currentY += 12f

            // ================= 2. MATCH HIGHLIGHTS & ANALYTICS CARDS =================
            paint.color = cHeaderBg
            canvas.drawRoundRect(RectF(28f, currentY, (pageWidth - 28).toFloat(), currentY + 22f), 5f, 5f, paint)
            canvas.drawText("MATCH ANALYTICS & HIGHLIGHTS", 38f, currentY + 15f, boldTextPaint.apply { color = Color.WHITE; textSize = 10f })

            currentY += 28f

            // Analytics Calculations
            val allBatters = team1Card.batters + team2Card.batters
            val topScorer = allBatters.maxByOrNull { it.runs }
            val topStrikeRate = allBatters.filter { it.balls >= 3 }.maxByOrNull { it.strikeRate }

            val allBowlers = team1Card.bowlers + team2Card.bowlers
            val topWicketBowler = allBowlers.maxByOrNull { it.wickets }
            val bestEconBowler = allBowlers.filter { it.oversLegalBalls >= 6 }.minByOrNull { it.economyRate }

            val totalFours = allBatters.sumOf { it.fours }
            val totalSixes = allBatters.sumOf { it.sixes }
            val boundaryRuns = totalFours * 4 + totalSixes * 6
            val totalMatchRuns = team1Runs + team2Runs
            val boundaryPercent = if (totalMatchRuns > 0) (boundaryRuns * 100 / totalMatchRuns) else 0

            // 4 Mini Metric Cards (2x2 Grid)
            val cardW = (pageWidth - 56f - 12f) / 2f
            val cardH = 46f

            fun drawMetricCard(x: Float, y: Float, label: String, value: String, subtext: String, badgeColor: Int) {
                val cardRect = RectF(x, y, x + cardW, y + cardH)
                paint.color = cLightBg
                canvas.drawRoundRect(cardRect, 6f, 6f, paint)
                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1f
                canvas.drawRoundRect(cardRect, 6f, 6f, strokePaint)

                // Left Accent Strip
                paint.color = badgeColor
                canvas.drawRoundRect(RectF(x, y, x + 4f, y + cardH), 4f, 4f, paint)

                canvas.drawText(label, x + 10f, y + 14f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                canvas.drawText(value.take(24), x + 10f, y + 28f, boldTextPaint.apply { color = cDarkText; textSize = 10f })
                canvas.drawText(subtext, x + 10f, y + 40f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
            }

            drawMetricCard(
                28f, currentY,
                "TOP BATTER",
                topScorer?.let { "${it.name} (${it.runs})" } ?: "N/A",
                topScorer?.let { "${it.balls} balls, ${it.fours}x4, ${it.sixes}x6" } ?: "-",
                cTeamA
            )

            drawMetricCard(
                28f + cardW + 12f, currentY,
                "TOP BOWLER",
                topWicketBowler?.let { "${it.name} (${it.wickets} W)" } ?: "N/A",
                topWicketBowler?.let { "${it.runsConceded} runs conceded" } ?: "-",
                cRedWicket
            )

            currentY += cardH + 8f

            drawMetricCard(
                28f, currentY,
                "HIGHEST STRIKE RATE",
                topStrikeRate?.let { "${it.name} (${String.format(Locale.US, "%.1f", it.strikeRate)})" } ?: "N/A",
                topStrikeRate?.let { "${it.runs} runs in ${it.balls} balls" } ?: "-",
                cHeaderAccent
            )

            drawMetricCard(
                28f + cardW + 12f, currentY,
                "MOST ECONOMICAL",
                bestEconBowler?.let { "${it.name} (${String.format(Locale.US, "%.1f", it.economyRate)})" } ?: "N/A",
                bestEconBowler?.let { "${it.oversLegalBalls / 6}.${it.oversLegalBalls % 6} overs" } ?: "-",
                cGreenWin
            )

            currentY += cardH + 12f

            // Boundary Breakdown Visual Bar
            val bCardRect = RectF(28f, currentY, (pageWidth - 28).toFloat(), currentY + 54f)
            paint.color = cLightBg
            canvas.drawRoundRect(bCardRect, 6f, 6f, paint)
            strokePaint.color = cBorder
            canvas.drawRoundRect(bCardRect, 6f, 6f, strokePaint)

            canvas.drawText("BOUNDARY CONTRIBUTION: $boundaryRuns runs ($boundaryPercent% of total $totalMatchRuns runs)", 38f, currentY + 16f, boldTextPaint.apply { color = cDarkText; textSize = 9f })

            // Visual Progress Bar
            val barX = 38f
            val barY = currentY + 24f
            val barW = pageWidth - 76f
            val barH = 10f
            val fourRuns = totalFours * 4
            val sixRuns = totalSixes * 6
            val otherRuns = max(0, totalMatchRuns - boundaryRuns)

            val fourW = if (totalMatchRuns > 0) (fourRuns.toFloat() / totalMatchRuns) * barW else 0f
            val sixW = if (totalMatchRuns > 0) (sixRuns.toFloat() / totalMatchRuns) * barW else 0f
            val otherW = if (totalMatchRuns > 0) (otherRuns.toFloat() / totalMatchRuns) * barW else barW

            // 4s segment
            paint.color = cTeamA
            canvas.drawRect(barX, barY, barX + fourW, barY + barH, paint)
            // 6s segment
            paint.color = cHeaderAccent
            canvas.drawRect(barX + fourW, barY, barX + fourW + sixW, barY + barH, paint)
            // others segment
            paint.color = Color.rgb(203, 213, 225)
            canvas.drawRect(barX + fourW + sixW, barY, barX + barW, barY + barH, paint)

            // Bar Legend
            val legY = barY + barH + 12f
            canvas.drawText("• 4s: $totalFours ($fourRuns R)", barX, legY, smallTextPaint.apply { color = cTeamA })
            canvas.drawText("• 6s: $totalSixes ($sixRuns R)", barX + 110f, legY, smallTextPaint.apply { color = cHeaderAccent })
            canvas.drawText("• Running/Extras: $otherRuns R", barX + 220f, legY, smallTextPaint.apply { color = cGrayText })

            drawFooter(canvas, 2, 4)
            pdfDocument.finishPage(page2)

            // ================= PAGE 3: OFFICIAL MATCH GRAPHS - WORM CHART =================
            val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
            val page3 = pdfDocument.startPage(pageInfo3)
            canvas = page3.canvas

            val isRotatedGraphs = data.totalOvers > 20 || data.teamARunsPerOver.size > 20 || data.teamBRunsPerOver.size > 20

            val totalOversCount = max(1, data.totalOvers)
            val maxBalls = totalOversCount * 6
            val team1Prog = if (team1Key == "A") data.teamARunProgress else data.teamBRunProgress
            val team2Prog = if (team2Key == "A") data.teamARunProgress else data.teamBRunProgress
            val t1PerOver = if (team1Key == "A") data.teamARunsPerOver else data.teamBRunsPerOver
            val t2PerOver = if (team2Key == "A") data.teamARunsPerOver else data.teamBRunsPerOver
            val totalOversToPlot = max(totalOversCount, max(t1PerOver.size, t2PerOver.size))

            val maxRunsRecorded = max(
                max(team1Runs, team2Runs),
                max((data.teamARunProgress.maxOfOrNull { it.runs } ?: 0), (data.teamBRunProgress.maxOfOrNull { it.runs } ?: 0))
            )
            val maxRunsAxis = if (maxRunsRecorded > 0) ((maxRunsRecorded / 20) + 1) * 20 else 50

            val maxRunsPerOver = max(
                max(t1PerOver.maxOrNull() ?: 0, t2PerOver.maxOrNull() ?: 0),
                12
            )
            val maxManhattanY = ((maxRunsPerOver / 6) + 1) * 6

            if (isRotatedGraphs) {
                // Rotated 90° on portrait A4 page for matches with > 20 overs
                canvas.save()
                canvas.translate(pageWidth.toFloat(), 0f)
                canvas.rotate(90f)

                val rotWidth = pageHeight.toFloat()  // 842f
                val rotHeight = pageWidth.toFloat() // 595f

                drawHeaderBanner(
                    canvas,
                    "MATCH VISUAL ANALYSIS",
                    "RUN ACCUMULATION PROGRESSION (WORM CHART)",
                    showMatchBadge = false,
                    bannerWidth = rotWidth,
                    badgeCustomText = "EXPANDED VIEW (${data.totalOvers} OVERS)"
                )

                val rotY = 66f
                val graph1H = rotHeight - rotY - 42f
                val graphBoxRect = RectF(28f, rotY, rotWidth - 28f, rotY + graph1H)
                paint.color = cLightBg
                canvas.drawRoundRect(graphBoxRect, 8f, 8f, paint)
                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1f
                canvas.drawRoundRect(graphBoxRect, 8f, 8f, strokePaint)

                canvas.drawText("RUN PROGRESSION (WORM CHART)", 40f, rotY + 22f, boldTextPaint.apply { color = cHeaderBg; textSize = 11f })

                val leg1X = rotWidth - 280f
                paint.color = cTeamA
                canvas.drawCircle(leg1X, rotY + 18f, 4.5f, paint)
                canvas.drawText("$team1Name ($team1Runs R)", leg1X + 9f, rotY + 22f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val leg2X = leg1X + 130f
                paint.color = cTeamB
                canvas.drawCircle(leg2X, rotY + 18f, 4.5f, paint)
                canvas.drawText("$team2Name ($team2Runs R)", leg2X + 9f, rotY + 22f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val plotLeft = 56f
                val plotRight = rotWidth - 36f
                val plotTop = rotY + 44f
                val plotBottom = rotY + graph1H - 34f
                val plotW = plotRight - plotLeft
                val plotH = plotBottom - plotTop

                val ySteps = 5
                for (i in 0..ySteps) {
                    val rVal = (maxRunsAxis / ySteps) * i
                    val yPos = plotBottom - (i.toFloat() / ySteps) * plotH
                    strokePaint.color = Color.rgb(230, 235, 242)
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawLine(plotLeft, yPos, plotRight, yPos, strokePaint)
                    canvas.drawText("$rVal", plotLeft - 24f, yPos + 3.5f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                }

                val overStep = when {
                    totalOversCount <= 20 -> 2
                    totalOversCount <= 40 -> 5
                    totalOversCount <= 80 -> 10
                    else -> 20
                }
                for (ov in 0..totalOversCount step overStep) {
                    val xPos = plotLeft + (ov.toFloat() / totalOversCount) * plotW
                    strokePaint.color = Color.rgb(235, 240, 246)
                    canvas.drawLine(xPos, plotTop, xPos, plotBottom, strokePaint)
                    canvas.drawText("${ov} ov", xPos - 10f, plotBottom + 16f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                }

                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1.2f
                canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, strokePaint)
                canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, strokePaint)

                fun drawWorm(points: List<RunProgressPoint>, card: TeamScorecard, lineColor: Int) {
                    if (points.isEmpty()) return
                    val validPoints = points.sortedBy { it.ball }
                    val path = Path()
                    var first = true

                    validPoints.forEach { pt ->
                        val x = plotLeft + (pt.ball.toFloat() / maxBalls).coerceIn(0f, 1f) * plotW
                        val y = plotBottom - (pt.runs.toFloat() / maxRunsAxis).coerceIn(0f, 1f) * plotH
                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    strokePaint.color = lineColor
                    strokePaint.strokeWidth = 2.5f
                    canvas.drawPath(path, strokePaint)

                    paint.color = lineColor
                    validPoints.forEach { pt ->
                        val x = plotLeft + (pt.ball.toFloat() / maxBalls).coerceIn(0f, 1f) * plotW
                        val y = plotBottom - (pt.runs.toFloat() / maxRunsAxis).coerceIn(0f, 1f) * plotH
                        canvas.drawCircle(x, y, 2.2f, paint)
                    }

                    card.fallOfWickets.forEach { fow ->
                        val fowOvParts = fow.overs.split(".")
                        val fowOv = fowOvParts.getOrNull(0)?.toIntOrNull() ?: 0
                        val fowB = fowOvParts.getOrNull(1)?.toIntOrNull() ?: 0
                        val fowBall = fowOv * 6 + fowB

                        val wx = plotLeft + (fowBall.toFloat() / maxBalls).coerceIn(0f, 1f) * plotW
                        val wy = plotBottom - (fow.runs.toFloat() / maxRunsAxis).coerceIn(0f, 1f) * plotH

                        paint.color = cRedWicket
                        canvas.drawCircle(wx, wy, 4.5f, paint)
                        canvas.drawText("W", wx - 2.8f, wy + 2.8f, boldTextPaint.apply { color = Color.WHITE; textSize = 7f })
                    }
                }

                drawWorm(team1Prog, team1Card, cTeamA)
                if (data.secondInningsStarted) {
                    drawWorm(team2Prog, team2Card, cTeamB)
                }

                drawFooter(canvas, 3, 4, customWidth = rotWidth, customHeight = rotHeight)
                canvas.restore()
            } else {
                // Portrait Worm Chart
                drawHeaderBanner(canvas, "MATCH VISUAL ANALYSIS", "RUN ACCUMULATION PROGRESSION (WORM CHART)", showMatchBadge = false)

                val startY = 88f
                val graphH = pageHeight - startY - 48f
                val graphBoxRect = RectF(28f, startY, (pageWidth - 28).toFloat(), startY + graphH)
                paint.color = cLightBg
                canvas.drawRoundRect(graphBoxRect, 8f, 8f, paint)
                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1f
                canvas.drawRoundRect(graphBoxRect, 8f, 8f, strokePaint)

                canvas.drawText("RUN PROGRESSION (WORM CHART)", 40f, startY + 24f, boldTextPaint.apply { color = cHeaderBg; textSize = 11f })

                val leg1X = pageWidth - 230f
                paint.color = cTeamA
                canvas.drawCircle(leg1X, startY + 20f, 4.5f, paint)
                canvas.drawText("$team1Name ($team1Runs R)", leg1X + 9f, startY + 24f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val leg2X = leg1X + 105f
                paint.color = cTeamB
                canvas.drawCircle(leg2X, startY + 20f, 4.5f, paint)
                canvas.drawText("$team2Name ($team2Runs R)", leg2X + 9f, startY + 24f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val plotLeft = 60f
                val plotRight = pageWidth - 42f
                val plotTop = startY + 48f
                val plotBottom = startY + graphH - 38f
                val plotW = plotRight - plotLeft
                val plotH = plotBottom - plotTop

                val ySteps = 5
                for (i in 0..ySteps) {
                    val rVal = (maxRunsAxis / ySteps) * i
                    val yPos = plotBottom - (i.toFloat() / ySteps) * plotH
                    strokePaint.color = Color.rgb(230, 235, 242)
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawLine(plotLeft, yPos, plotRight, yPos, strokePaint)
                    canvas.drawText("$rVal", plotLeft - 24f, yPos + 3.5f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                }

                val overStep = if (totalOversCount <= 10) 1 else 2
                for (ov in 0..totalOversCount step overStep) {
                    val xPos = plotLeft + (ov.toFloat() / totalOversCount) * plotW
                    strokePaint.color = Color.rgb(235, 240, 246)
                    canvas.drawLine(xPos, plotTop, xPos, plotBottom, strokePaint)
                    canvas.drawText("${ov} ov", xPos - 10f, plotBottom + 16f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                }

                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1.2f
                canvas.drawLine(plotLeft, plotBottom, plotRight, plotBottom, strokePaint)
                canvas.drawLine(plotLeft, plotTop, plotLeft, plotBottom, strokePaint)

                fun drawTeamWorm(points: List<RunProgressPoint>, card: TeamScorecard, lineColor: Int) {
                    if (points.isEmpty()) return
                    val validPoints = points.sortedBy { it.ball }
                    val path = Path()
                    var first = true

                    validPoints.forEach { pt ->
                        val x = plotLeft + (pt.ball.toFloat() / maxBalls).coerceIn(0f, 1f) * plotW
                        val y = plotBottom - (pt.runs.toFloat() / maxRunsAxis).coerceIn(0f, 1f) * plotH
                        if (first) {
                            path.moveTo(x, y)
                            first = false
                        } else {
                            path.lineTo(x, y)
                        }
                    }

                    strokePaint.color = lineColor
                    strokePaint.strokeWidth = 2.5f
                    canvas.drawPath(path, strokePaint)

                    paint.color = lineColor
                    validPoints.forEach { pt ->
                        val x = plotLeft + (pt.ball.toFloat() / maxBalls).coerceIn(0f, 1f) * plotW
                        val y = plotBottom - (pt.runs.toFloat() / maxRunsAxis).coerceIn(0f, 1f) * plotH
                        canvas.drawCircle(x, y, 2.5f, paint)
                    }

                    card.fallOfWickets.forEach { fow ->
                        val fowOvParts = fow.overs.split(".")
                        val fowOv = fowOvParts.getOrNull(0)?.toIntOrNull() ?: 0
                        val fowB = fowOvParts.getOrNull(1)?.toIntOrNull() ?: 0
                        val fowBall = fowOv * 6 + fowB

                        val wx = plotLeft + (fowBall.toFloat() / maxBalls).coerceIn(0f, 1f) * plotW
                        val wy = plotBottom - (fow.runs.toFloat() / maxRunsAxis).coerceIn(0f, 1f) * plotH

                        paint.color = cRedWicket
                        canvas.drawCircle(wx, wy, 4.5f, paint)
                        canvas.drawText("W", wx - 2.8f, wy + 2.8f, boldTextPaint.apply { color = Color.WHITE; textSize = 7f })
                    }
                }

                drawTeamWorm(team1Prog, team1Card, cTeamA)
                if (data.secondInningsStarted) {
                    drawTeamWorm(team2Prog, team2Card, cTeamB)
                }

                drawFooter(canvas, 3, 4)
            }
            pdfDocument.finishPage(page3)

            // ================= PAGE 4: OFFICIAL MATCH GRAPHS - MANHATTAN CHART =================
            val pageInfo4 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
            val page4 = pdfDocument.startPage(pageInfo4)
            canvas = page4.canvas

            if (isRotatedGraphs) {
                // Rotated 90° on portrait A4 page for matches with > 20 overs
                canvas.save()
                canvas.translate(pageWidth.toFloat(), 0f)
                canvas.rotate(90f)

                val rotWidth = pageHeight.toFloat()  // 842f
                val rotHeight = pageWidth.toFloat() // 595f

                drawHeaderBanner(
                    canvas,
                    "MATCH VISUAL ANALYSIS",
                    "OVER-BY-OVER SCORING PROGRESSION (MANHATTAN CHART)",
                    showMatchBadge = false,
                    bannerWidth = rotWidth,
                    badgeCustomText = "EXPANDED VIEW (${data.totalOvers} OVERS)"
                )

                val rotY = 66f
                val graph2H = rotHeight - rotY - 42f
                val graph2BoxRect = RectF(28f, rotY, rotWidth - 28f, rotY + graph2H)
                paint.color = cLightBg
                canvas.drawRoundRect(graph2BoxRect, 8f, 8f, paint)
                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1f
                canvas.drawRoundRect(graph2BoxRect, 8f, 8f, strokePaint)

                canvas.drawText("OVER-BY-OVER SCORING (MANHATTAN CHART)", 40f, rotY + 22f, boldTextPaint.apply { color = cHeaderBg; textSize = 11f })

                val mLeg1X = rotWidth - 280f
                paint.color = cTeamA
                canvas.drawRect(mLeg1X, rotY + 13f, mLeg1X + 11f, rotY + 24f, paint)
                canvas.drawText(team1Name, mLeg1X + 15f, rotY + 22f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val mLeg2X = mLeg1X + 130f
                paint.color = cTeamB
                canvas.drawRect(mLeg2X, rotY + 13f, mLeg2X + 11f, rotY + 24f, paint)
                canvas.drawText(team2Name, mLeg2X + 15f, rotY + 22f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val mPlotLeft = 56f
                val mPlotRight = rotWidth - 36f
                val mPlotTop = rotY + 44f
                val mPlotBottom = rotY + graph2H - 34f
                val mPlotW = mPlotRight - mPlotLeft
                val mPlotH = mPlotBottom - mPlotTop

                val mYSteps = 5
                for (i in 0..mYSteps) {
                    val rVal = (maxManhattanY / mYSteps) * i
                    val yPos = mPlotBottom - (i.toFloat() / mYSteps) * mPlotH
                    strokePaint.color = Color.rgb(230, 235, 242)
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawLine(mPlotLeft, yPos, mPlotRight, yPos, strokePaint)
                    canvas.drawText("$rVal", mPlotLeft - 24f, yPos + 3.5f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                }

                val slotW = mPlotW / totalOversToPlot
                val barWidth = if (data.secondInningsStarted) {
                    (slotW * 0.38f).coerceIn(3f, 18f)
                } else {
                    (slotW * 0.6f).coerceIn(4f, 28f)
                }

                val overLabelStep = when {
                    totalOversToPlot <= 20 -> 2
                    totalOversToPlot <= 35 -> 2
                    totalOversToPlot <= 60 -> 5
                    else -> 10
                }

                for (ovIdx in 0 until totalOversToPlot) {
                    val slotCenterX = mPlotLeft + (ovIdx + 0.5f) * slotW
                    val r1 = t1PerOver.getOrNull(ovIdx) ?: 0
                    val r2 = t2PerOver.getOrNull(ovIdx) ?: 0

                    if (data.secondInningsStarted) {
                        val h1 = (r1.toFloat() / maxManhattanY).coerceIn(0f, 1f) * mPlotH
                        val b1Left = slotCenterX - barWidth - 1.5f
                        val b1Top = mPlotBottom - h1
                        paint.color = cTeamA
                        canvas.drawRoundRect(RectF(b1Left, b1Top, b1Left + barWidth, mPlotBottom), 2f, 2f, paint)

                        if (r1 > 0 && slotW >= 16f) {
                            val r1Str = "$r1"
                            val tw = smallTextPaint.measureText(r1Str)
                            canvas.drawText(r1Str, b1Left + (barWidth - tw) / 2f, b1Top - 3f, smallTextPaint.apply { color = cTeamA; textSize = 7f })
                        }

                        if (ovIdx < t2PerOver.size) {
                            val h2 = (r2.toFloat() / maxManhattanY).coerceIn(0f, 1f) * mPlotH
                            val b2Left = slotCenterX + 1.5f
                            val b2Top = mPlotBottom - h2
                            paint.color = cTeamB
                            canvas.drawRoundRect(RectF(b2Left, b2Top, b2Left + barWidth, mPlotBottom), 2f, 2f, paint)

                            if (r2 > 0 && slotW >= 16f) {
                                val r2Str = "$r2"
                                val tw = smallTextPaint.measureText(r2Str)
                                canvas.drawText(r2Str, b2Left + (barWidth - tw) / 2f, b2Top - 3f, smallTextPaint.apply { color = cTeamB; textSize = 7f })
                            }
                        }
                    } else {
                        val h1 = (r1.toFloat() / maxManhattanY).coerceIn(0f, 1f) * mPlotH
                        val b1Left = slotCenterX - barWidth / 2f
                        val b1Top = mPlotBottom - h1
                        paint.color = cTeamA
                        canvas.drawRoundRect(RectF(b1Left, b1Top, b1Left + barWidth, mPlotBottom), 2f, 2f, paint)

                        if (r1 > 0 && slotW >= 14f) {
                            val r1Str = "$r1"
                            val tw = smallTextPaint.measureText(r1Str)
                            canvas.drawText(r1Str, b1Left + (barWidth - tw) / 2f, b1Top - 3f, smallTextPaint.apply { color = cTeamA; textSize = 7f })
                        }
                    }

                    val overNum = ovIdx + 1
                    if (overNum == 1 || overNum % overLabelStep == 0 || overNum == totalOversToPlot) {
                        val lbl = "O$overNum"
                        val lw = smallTextPaint.measureText(lbl)
                        canvas.drawText(lbl, slotCenterX - lw / 2f, mPlotBottom + 16f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                    }
                }

                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1.2f
                canvas.drawLine(mPlotLeft, mPlotBottom, mPlotRight, mPlotBottom, strokePaint)
                canvas.drawLine(mPlotLeft, mPlotTop, mPlotLeft, mPlotBottom, strokePaint)

                drawFooter(canvas, 4, 4, customWidth = rotWidth, customHeight = rotHeight)
                canvas.restore()
            } else {
                // Portrait Manhattan Chart
                drawHeaderBanner(canvas, "MATCH VISUAL ANALYSIS", "OVER-BY-OVER SCORING PROGRESSION (MANHATTAN CHART)", showMatchBadge = false)

                val startY = 88f
                val graph2H = pageHeight - startY - 48f
                val graph2BoxRect = RectF(28f, startY, (pageWidth - 28).toFloat(), startY + graph2H)
                paint.color = cLightBg
                canvas.drawRoundRect(graph2BoxRect, 8f, 8f, paint)
                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1f
                canvas.drawRoundRect(graph2BoxRect, 8f, 8f, strokePaint)

                canvas.drawText("OVER-BY-OVER SCORING (MANHATTAN CHART)", 40f, startY + 24f, boldTextPaint.apply { color = cHeaderBg; textSize = 11f })

                val mLeg1X = pageWidth - 230f
                paint.color = cTeamA
                canvas.drawRect(mLeg1X, startY + 15f, mLeg1X + 11f, startY + 26f, paint)
                canvas.drawText(team1Name, mLeg1X + 15f, startY + 24f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val mLeg2X = mLeg1X + 105f
                paint.color = cTeamB
                canvas.drawRect(mLeg2X, startY + 15f, mLeg2X + 11f, startY + 26f, paint)
                canvas.drawText(team2Name, mLeg2X + 15f, startY + 24f, smallTextPaint.apply { color = cDarkText; textSize = 8.5f })

                val mPlotLeft = 60f
                val mPlotRight = pageWidth - 42f
                val mPlotTop = startY + 48f
                val mPlotBottom = startY + graph2H - 38f
                val mPlotW = mPlotRight - mPlotLeft
                val mPlotH = mPlotBottom - mPlotTop

                val mYSteps = 5
                for (i in 0..mYSteps) {
                    val rVal = (maxManhattanY / mYSteps) * i
                    val yPos = mPlotBottom - (i.toFloat() / mYSteps) * mPlotH
                    strokePaint.color = Color.rgb(230, 235, 242)
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawLine(mPlotLeft, yPos, mPlotRight, yPos, strokePaint)
                    canvas.drawText("$rVal", mPlotLeft - 24f, yPos + 3.5f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                }

                val slotW = mPlotW / totalOversToPlot
                val barWidth = if (data.secondInningsStarted) {
                    (slotW * 0.38f).coerceIn(3.5f, 20f)
                } else {
                    (slotW * 0.6f).coerceIn(5f, 30f)
                }

                val overLabelStep = if (totalOversToPlot <= 12) 1 else 2

                for (ovIdx in 0 until totalOversToPlot) {
                    val slotCenterX = mPlotLeft + (ovIdx + 0.5f) * slotW
                    val r1 = t1PerOver.getOrNull(ovIdx) ?: 0
                    val r2 = t2PerOver.getOrNull(ovIdx) ?: 0

                    if (data.secondInningsStarted) {
                        val h1 = (r1.toFloat() / maxManhattanY).coerceIn(0f, 1f) * mPlotH
                        val b1Left = slotCenterX - barWidth - 1.5f
                        val b1Top = mPlotBottom - h1
                        paint.color = cTeamA
                        canvas.drawRoundRect(RectF(b1Left, b1Top, b1Left + barWidth, mPlotBottom), 2f, 2f, paint)

                        if (r1 > 0 && slotW >= 16f) {
                            val r1Str = "$r1"
                            val tw = smallTextPaint.measureText(r1Str)
                            canvas.drawText(r1Str, b1Left + (barWidth - tw) / 2f, b1Top - 3f, smallTextPaint.apply { color = cTeamA; textSize = 7f })
                        }

                        if (ovIdx < t2PerOver.size) {
                            val h2 = (r2.toFloat() / maxManhattanY).coerceIn(0f, 1f) * mPlotH
                            val b2Left = slotCenterX + 1.5f
                            val b2Top = mPlotBottom - h2
                            paint.color = cTeamB
                            canvas.drawRoundRect(RectF(b2Left, b2Top, b2Left + barWidth, mPlotBottom), 2f, 2f, paint)

                            if (r2 > 0 && slotW >= 16f) {
                                val r2Str = "$r2"
                                val tw = smallTextPaint.measureText(r2Str)
                                canvas.drawText(r2Str, b2Left + (barWidth - tw) / 2f, b2Top - 3f, smallTextPaint.apply { color = cTeamB; textSize = 7f })
                            }
                        }
                    } else {
                        val h1 = (r1.toFloat() / maxManhattanY).coerceIn(0f, 1f) * mPlotH
                        val b1Left = slotCenterX - barWidth / 2f
                        val b1Top = mPlotBottom - h1
                        paint.color = cTeamA
                        canvas.drawRoundRect(RectF(b1Left, b1Top, b1Left + barWidth, mPlotBottom), 2f, 2f, paint)

                        if (r1 > 0 && slotW >= 14f) {
                            val r1Str = "$r1"
                            val tw = smallTextPaint.measureText(r1Str)
                            canvas.drawText(r1Str, b1Left + (barWidth - tw) / 2f, b1Top - 3f, smallTextPaint.apply { color = cTeamA; textSize = 7f })
                        }
                    }

                    val overNum = ovIdx + 1
                    if (overNum == 1 || overNum % overLabelStep == 0 || overNum == totalOversToPlot) {
                        val lbl = "O$overNum"
                        val lw = smallTextPaint.measureText(lbl)
                        canvas.drawText(lbl, slotCenterX - lw / 2f, mPlotBottom + 16f, smallTextPaint.apply { color = cGrayText; textSize = 8f })
                    }
                }

                strokePaint.color = cBorder
                strokePaint.strokeWidth = 1.2f
                canvas.drawLine(mPlotLeft, mPlotBottom, mPlotRight, mPlotBottom, strokePaint)
                canvas.drawLine(mPlotLeft, mPlotTop, mPlotLeft, mPlotBottom, strokePaint)

                drawFooter(canvas, 4, 4)
            }
            pdfDocument.finishPage(page4)

            // Write to file in app cache directory
            val pdfDir = File(context.cacheDir, "match_reports")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val fileName = "Match_Report_${team1Name.replace(" ", "_")}_vs_${team2Name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(pdfDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            // Share / Open PDF
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "Match Report: $team1Name vs $team2Name")
                putExtra(Intent.EXTRA_TEXT, "Official match report, scorecard, and analytics charts for $team1Name vs $team2Name.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Download / Share Match PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)

            Toast.makeText(context, "Match PDF report generated!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
