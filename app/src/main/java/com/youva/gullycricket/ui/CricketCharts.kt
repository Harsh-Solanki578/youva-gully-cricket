package com.youva.gullycricket.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youva.gullycricket.model.RunProgressPoint
import com.youva.gullycricket.model.RunRatePoint
import com.youva.gullycricket.model.BallHistoryEntry
import com.youva.gullycricket.ui.theme.CricketColorScheme

@Composable
fun ManhattanChart(
    teamARuns: List<Int>,
    teamBRuns: List<Int>,
    totalOvers: Int,
    colors: CricketColorScheme,
    modifier: Modifier = Modifier,
    ballHistory: List<BallHistoryEntry> = emptyList(),
    teamAName: String = "Team A",
    teamBName: String = "Team B"
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedOver by remember { mutableStateOf<Int?>(null) }
    var selectedTeam by remember { mutableStateOf<String?>(null) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val paddingLeft = with(LocalDensity.current) { 35.dp.toPx() }
    val paddingBottom = with(LocalDensity.current) { 25.dp.toPx() }
    val paddingTop = with(LocalDensity.current) { 15.dp.toPx() }
    val paddingRight = with(LocalDensity.current) { 10.dp.toPx() }

    val maxOversData = maxOf(teamARuns.size, teamBRuns.size)
    val oversToDraw = maxOf(maxOversData, if (totalOvers > 0) totalOvers else 5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(12.dp)
            .animateContentSize()
    ) {
        // Legend and Interactivity Status Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.teamA)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = teamAName,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    style = TextStyle(lineHeight = 10.sp)
                )
                if (teamBRuns.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.teamB)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = teamBName,
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        style = TextStyle(lineHeight = 10.sp)
                    )
                }
            }

            // Interactive instruction
            Text(
                text = if (selectedOver == null) "Tap bars to inspect" else "Tap again to clear",
                color = colors.textAccent.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Chart area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 150.dp)
        ) {
            val totalWidth = maxWidth
            val yAxisWidth = 35.dp
            val scrollableAreaWidth = maxOf(totalWidth - yAxisWidth, (oversToDraw * 55).dp)

            val density = LocalDensity.current
            val scrollPaddingLeft = with(density) { 15.dp.toPx() }
            val scrollableAreaWidthPx = with(density) { scrollableAreaWidth.toPx() }

            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Pinned Y-Axis
                Canvas(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .fillMaxHeight()
                ) {
                    val height = size.height - paddingTop - paddingBottom
                    val maxRunsInAnyOver = maxOf(
                        teamARuns.maxOrNull() ?: 0,
                        teamBRuns.maxOrNull() ?: 0,
                        15
                    )
                    val maxValY = ((maxRunsInAnyOver + 4) / 5) * 5

                    val gridLineCount = 4
                    for (i in 0..gridLineCount) {
                        val ratio = i.toFloat() / gridLineCount
                        val y = paddingTop + height * (1f - ratio)
                        val value = (ratio * maxValY).toInt()

                        val textLayoutResult = textMeasurer.measure(
                            text = value.toString(),
                            style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(size.width - textLayoutResult.size.width - 5f, y - textLayoutResult.size.height / 2f)
                        )
                    }
                }

                // 2. Horizontally scrollable content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(scrollableAreaWidth)
                            .fillMaxHeight()
                            .onSizeChanged {
                                canvasWidth = it.width.toFloat()
                                canvasHeight = it.height.toFloat()
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(oversToDraw, scrollableAreaWidthPx) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0) {
                                                val relativeX = offset.x - scrollPaddingLeft
                                                val stepX = width / oversToDraw
                                                val overIdx = (relativeX / stepX).toInt().coerceIn(0, oversToDraw - 1)
                                                val localX = relativeX % stepX
                                                val team = if (teamBRuns.isNotEmpty()) {
                                                    if (localX < stepX / 2f) "A" else "B"
                                                } else {
                                                    "A"
                                                }
                                                if (selectedOver == overIdx && selectedTeam == team) {
                                                    selectedOver = null
                                                    selectedTeam = null
                                                } else {
                                                    selectedOver = overIdx
                                                    selectedTeam = team
                                                }
                                            }
                                        }
                                    )
                                }
                                .pointerInput(oversToDraw, scrollableAreaWidthPx) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0) {
                                                val relativeX = offset.x - scrollPaddingLeft
                                                val stepX = width / oversToDraw
                                                val overIdx = (relativeX / stepX).toInt().coerceIn(0, oversToDraw - 1)
                                                val localX = relativeX % stepX
                                                val team = if (teamBRuns.isNotEmpty()) {
                                                    if (localX < stepX / 2f) "A" else "B"
                                                } else {
                                                    "A"
                                                }
                                                selectedOver = overIdx
                                                selectedTeam = team
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0) {
                                                val relativeX = change.position.x - scrollPaddingLeft
                                                val stepX = width / oversToDraw
                                                val overIdx = (relativeX / stepX).toInt().coerceIn(0, oversToDraw - 1)
                                                val localX = relativeX % stepX
                                                val team = if (teamBRuns.isNotEmpty()) {
                                                    if (localX < stepX / 2f) "A" else "B"
                                                } else {
                                                    "A"
                                                }
                                                selectedOver = overIdx
                                                selectedTeam = team
                                            }
                                        },
                                        onDragEnd = {}
                                    )
                                }
                        ) {
                            val width = size.width - scrollPaddingLeft - paddingRight
                            val height = size.height - paddingTop - paddingBottom

                            val maxRunsInAnyOver = maxOf(
                                teamARuns.maxOrNull() ?: 0,
                                teamBRuns.maxOrNull() ?: 0,
                                15
                            )
                            val maxValY = ((maxRunsInAnyOver + 4) / 5) * 5

                            // Draw horizontal grid lines
                            val gridLineCount = 4
                            for (i in 0..gridLineCount) {
                                val ratio = i.toFloat() / gridLineCount
                                val y = paddingTop + height * (1f - ratio)

                                drawLine(
                                    color = colors.grid,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                )
                            }

                            // Draw Highlight guide band
                            selectedOver?.let { overIdx ->
                                if (overIdx < oversToDraw) {
                                    val stepX = width / oversToDraw
                                    val barWidth = stepX * 0.32f
                                    val innerGap = stepX * 0.08f
                                    val centerX = scrollPaddingLeft + (overIdx * stepX) + stepX / 2f

                                    val highlightLeft = if (selectedTeam == "B") {
                                        centerX + innerGap / 2f
                                    } else {
                                        if (teamBRuns.isNotEmpty()) centerX - barWidth - innerGap / 2f else centerX - barWidth / 2f
                                    }

                                    drawRect(
                                        color = colors.textAccent.copy(alpha = 0.08f),
                                        topLeft = Offset(highlightLeft, paddingTop),
                                        size = Size(barWidth, height)
                                    )
                                }
                            }

                            // Draw Bars
                            val stepX = width / oversToDraw
                            val barWidth = stepX * 0.32f
                            val innerGap = stepX * 0.08f

                            for (overIdx in 0 until oversToDraw) {
                                val centerX = scrollPaddingLeft + (overIdx * stepX) + stepX / 2f
                                val isSelected = selectedOver == overIdx

                                // Draw Team A Bar
                                if (overIdx < teamARuns.size) {
                                    val runsA = teamARuns[overIdx]
                                    val barHeight = (runsA.toFloat() / maxValY) * height
                                    val barLeft = if (teamBRuns.isNotEmpty()) centerX - barWidth - innerGap / 2f else centerX - barWidth / 2f
                                    val isASelected = isSelected && selectedTeam == "A"
                                    val alphaToApply = if (selectedOver == null) 1f else if (isASelected) 1f else 0.35f

                                    drawRect(
                                        color = colors.teamA.copy(alpha = alphaToApply),
                                        topLeft = Offset(barLeft, paddingTop + height - barHeight),
                                        size = Size(barWidth, barHeight)
                                    )
                                }

                                // Draw Team B Bar
                                if (overIdx < teamBRuns.size) {
                                    val runsB = teamBRuns[overIdx]
                                    val barHeight = (runsB.toFloat() / maxValY) * height
                                    val barLeft = centerX + innerGap / 2f
                                    val isBSelected = isSelected && selectedTeam == "B"
                                    val alphaToApply = if (selectedOver == null) 1f else if (isBSelected) 1f else 0.35f

                                    drawRect(
                                        color = colors.teamB.copy(alpha = alphaToApply),
                                        topLeft = Offset(barLeft, paddingTop + height - barHeight),
                                        size = Size(barWidth, barHeight)
                                    )
                                }

                                // Draw vertical guide dashed line
                                if (isSelected) {
                                    val barCenterX = if (selectedTeam == "B") {
                                        centerX + barWidth / 2f + innerGap / 2f
                                    } else {
                                        if (teamBRuns.isNotEmpty()) centerX - barWidth / 2f - innerGap / 2f else centerX
                                    }
                                    drawLine(
                                        color = colors.textAccent.copy(alpha = 0.8f),
                                        start = Offset(barCenterX, paddingTop),
                                        end = Offset(barCenterX, paddingTop + height),
                                        strokeWidth = 1.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                    )
                                }

                                // Draw X Labels (Overs)
                                val labelText = (overIdx + 1).toString()
                                val textLayoutResult = textMeasurer.measure(
                                    text = labelText,
                                    style = TextStyle(
                                        color = if (isSelected) colors.textAccent else colors.textSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 8.sp
                                    )
                                )
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(centerX - textLayoutResult.size.width / 2f, size.height - paddingBottom + 4f)
                                )
                            }

                            // Title
                            val titleLayout = textMeasurer.measure(
                                text = "Overs",
                                style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                            )
                            drawText(
                                textLayoutResult = titleLayout,
                                topLeft = Offset(scrollPaddingLeft + width / 2f - titleLayout.size.width / 2f, size.height - 12f)
                            )
                        }

                        // Small Floating Value Tooltip
                        selectedOver?.let { over ->
                            val runsA = teamARuns.getOrNull(over) ?: 0
                            val runsB = teamBRuns.getOrNull(over) ?: 0
                            val hasB = teamBRuns.isNotEmpty()

                            val text = if (selectedTeam == "B") {
                                "$teamBName (Over ${over + 1}): $runsB runs"
                            } else {
                                if (hasB) "$teamAName (Over ${over + 1}): $runsA runs" else "Over ${over + 1}: $runsA runs"
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        val width = canvasWidth - scrollPaddingLeft - paddingRight
                                        val stepX = if (oversToDraw > 0) width / oversToDraw else width
                                        val centerX = scrollPaddingLeft + (over * stepX) + stepX / 2f

                                        val barCenterX = if (selectedTeam == "B") {
                                            val barWidth = stepX * 0.32f
                                            val innerGap = stepX * 0.08f
                                            centerX + barWidth / 2f + innerGap / 2f
                                        } else {
                                            if (hasB) {
                                                val barWidth = stepX * 0.32f
                                                val innerGap = stepX * 0.08f
                                                centerX - barWidth / 2f - innerGap / 2f
                                            } else centerX
                                        }

                                        val tooltipWidthPx = 130.dp.toPx()
                                        val xOffset = (barCenterX - tooltipWidthPx / 2f).coerceIn(
                                            scrollPaddingLeft,
                                            maxOf(scrollPaddingLeft, canvasWidth - paddingRight - tooltipWidthPx)
                                        )
                                        IntOffset(xOffset.toInt(), 2.dp.toPx().toInt())
                                    }
                                    .width(130.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surface.copy(alpha = 0.9f))
                                    .border(1.dp, colors.textAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = text,
                                    color = colors.textAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        if (scrollState.maxValue > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "◀",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val targetValue = (scrollState.value - 100).coerceAtLeast(0)
                                scrollState.animateScrollTo(targetValue)
                            }
                        }
                        .padding(4.dp)
                )
                Slider(
                    value = scrollState.value.toFloat().coerceIn(0f, scrollState.maxValue.toFloat()),
                    onValueChange = { newValue ->
                        coroutineScope.launch {
                            scrollState.scrollTo(newValue.toInt())
                        }
                    },
                    valueRange = 0f..scrollState.maxValue.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.textAccent,
                        activeTrackColor = colors.textAccent.copy(alpha = 0.5f),
                        inactiveTrackColor = colors.grid.copy(alpha = 0.2f)
                    )
                )
                Text(
                    text = "▶",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val targetValue = (scrollState.value + 100).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(targetValue)
                            }
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun RunRateLineChart(
    runRatePoints: List<RunRatePoint>,
    colors: CricketColorScheme,
    modifier: Modifier = Modifier,
    ballHistory: List<BallHistoryEntry> = emptyList()
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val paddingLeft = with(LocalDensity.current) { 35.dp.toPx() }
    val paddingBottom = with(LocalDensity.current) { 25.dp.toPx() }
    val paddingTop = with(LocalDensity.current) { 15.dp.toPx() }
    val paddingRight = with(LocalDensity.current) { 15.dp.toPx() }

    val maxBallsToShow = 25
    val recentPoints = runRatePoints.takeLast(maxBallsToShow)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(12.dp)
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Run Rate Trend",
                color = colors.textPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            if (recentPoints.isNotEmpty()) {
                Text(
                    text = if (selectedPointIndex == null) "Slide to inspect" else "Tap again to clear",
                    color = colors.textAccent.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 150.dp)
        ) {
            val totalWidth = maxWidth
            val yAxisWidth = 35.dp
            val pointsCount = recentPoints.size
            val scrollableAreaWidth = if (pointsCount > 0) {
                maxOf(totalWidth - yAxisWidth, (pointsCount * 40).dp)
            } else {
                totalWidth - yAxisWidth
            }

            val density = LocalDensity.current
            val scrollPaddingLeft = with(density) { 15.dp.toPx() }
            val scrollableAreaWidthPx = with(density) { scrollableAreaWidth.toPx() }

            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Pinned Y-Axis
                Canvas(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .fillMaxHeight()
                ) {
                    val height = size.height - paddingTop - paddingBottom
                    val maxRate = recentPoints.maxOfOrNull { it.rate } ?: 0f
                    val maxValY = maxOf(((maxRate + 1.99f) / 2).toInt() * 2f, 6f)

                    val gridLineCount = 4
                    for (i in 0..gridLineCount) {
                        val ratio = i.toFloat() / gridLineCount
                        val y = paddingTop + height * (1f - ratio)
                        val value = ratio * maxValY

                        val textLayoutResult = textMeasurer.measure(
                            text = String.format("%.1f", value),
                            style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(size.width - textLayoutResult.size.width - 5f, y - textLayoutResult.size.height / 2f)
                        )
                    }
                }

                // 2. Horizontally scrollable content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(scrollableAreaWidth)
                            .fillMaxHeight()
                            .onSizeChanged {
                                canvasWidth = it.width.toFloat()
                                canvasHeight = it.height.toFloat()
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(recentPoints.size, scrollableAreaWidthPx) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0 && recentPoints.isNotEmpty()) {
                                                val relativeX = offset.x - scrollPaddingLeft
                                                val stepX = if (recentPoints.size > 1) width / (recentPoints.size - 1) else width
                                                val idx = (relativeX / stepX + 0.5f).toInt().coerceIn(0, recentPoints.size - 1)
                                                selectedPointIndex = if (selectedPointIndex == idx) null else idx
                                            }
                                        }
                                    )
                                }
                                .pointerInput(recentPoints.size, scrollableAreaWidthPx) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0 && recentPoints.isNotEmpty()) {
                                                val relativeX = offset.x - scrollPaddingLeft
                                                val stepX = if (recentPoints.size > 1) width / (recentPoints.size - 1) else width
                                                val idx = (relativeX / stepX + 0.5f).toInt().coerceIn(0, recentPoints.size - 1)
                                                selectedPointIndex = idx
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0 && recentPoints.isNotEmpty()) {
                                                val relativeX = change.position.x - scrollPaddingLeft
                                                val stepX = if (recentPoints.size > 1) width / (recentPoints.size - 1) else width
                                                val idx = (relativeX / stepX + 0.5f).toInt().coerceIn(0, recentPoints.size - 1)
                                                selectedPointIndex = idx
                                            }
                                        },
                                        onDragEnd = {}
                                    )
                                }
                        ) {
                            val width = size.width - scrollPaddingLeft - paddingRight
                            val height = size.height - paddingTop - paddingBottom

                            if (recentPoints.isEmpty()) {
                                val noDataText = textMeasurer.measure(
                                    text = "No run rate data yet.",
                                    style = TextStyle(color = colors.textSecondary, fontSize = 11.sp)
                                )
                                drawText(
                                    textLayoutResult = noDataText,
                                    topLeft = Offset(size.width / 2f - noDataText.size.width / 2f, size.height / 2f - noDataText.size.height / 2f)
                                )
                                return@Canvas
                            }

                            val maxRate = recentPoints.maxOfOrNull { it.rate } ?: 0f
                            val maxValY = maxOf(((maxRate + 1.99f) / 2).toInt() * 2f, 6f)

                            // Grid lines
                            val gridLineCount = 4
                            for (i in 0..gridLineCount) {
                                val ratio = i.toFloat() / gridLineCount
                                val y = paddingTop + height * (1f - ratio)

                                drawLine(
                                    color = colors.grid,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                )
                            }

                            // Draw Curve
                            val stepX = if (recentPoints.size > 1) width / (recentPoints.size - 1) else width
                            val pointsList = ArrayList<Offset>()

                            val linePath = Path()
                            val fillPath = Path()

                            recentPoints.forEachIndexed { idx, pt ->
                                val x = scrollPaddingLeft + (idx * stepX)
                                val y = paddingTop + height * (1f - (pt.rate / maxValY))
                                val pointOffset = Offset(x, y)
                                pointsList.add(pointOffset)

                                if (idx == 0) {
                                    linePath.moveTo(x, y)
                                    fillPath.moveTo(x, paddingTop + height)
                                    fillPath.lineTo(x, y)
                                } else {
                                    linePath.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }

                                if (idx == recentPoints.size - 1) {
                                    fillPath.lineTo(x, paddingTop + height)
                                    fillPath.close()
                                }
                            }

                            // Draw Gradient Fill
                            val fillBrush = Brush.verticalGradient(
                                colors = listOf(colors.textAccent.copy(alpha = 0.25f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + height
                            )
                            drawPath(path = fillPath, brush = fillBrush)

                            // Draw Curve Stroke
                            drawPath(
                                path = linePath,
                                color = colors.textAccent,
                                style = Stroke(width = 4f)
                            )

                            // Draw data points & X Labels
                            recentPoints.forEachIndexed { idx, pt ->
                                val offset = pointsList[idx]
                                val isSelected = selectedPointIndex == idx

                                // Point circle
                                drawCircle(
                                    color = colors.surface,
                                    radius = if (isSelected) 8f else 6f,
                                    center = offset
                                )
                                drawCircle(
                                    color = if (isSelected) colors.textAccent else colors.textAccent.copy(alpha = 0.7f),
                                    radius = if (isSelected) 6f else 4f,
                                    center = offset
                                )

                                if (isSelected) {
                                    // Vertical dashed line
                                    drawLine(
                                        color = colors.textAccent.copy(alpha = 0.8f),
                                        start = Offset(offset.x, paddingTop),
                                        end = Offset(offset.x, paddingTop + height),
                                        strokeWidth = 1.5f,
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                    )

                                    // Highlight ring
                                    drawCircle(
                                        color = colors.textAccent.copy(alpha = 0.3f),
                                        radius = 12f,
                                        center = offset,
                                        style = Stroke(width = 2f)
                                    )
                                }

                                // Draw X Label for every point clearly
                                val over = (pt.ball - 1) / 6
                                val ball = (pt.ball - 1) % 6 + 1
                                val labelText = "$over.$ball"

                                val textLayoutResult = textMeasurer.measure(
                                    text = labelText,
                                    style = TextStyle(
                                        color = if (isSelected) colors.textAccent else colors.textSecondary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 7.sp
                                    )
                                )
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(offset.x - textLayoutResult.size.width / 2f, size.height - paddingBottom + 4f)
                                )
                            }

                            // Title
                            val titleLayout = textMeasurer.measure(
                                text = "Over.Ball",
                                style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                              )
                            drawText(
                                textLayoutResult = titleLayout,
                                topLeft = Offset(scrollPaddingLeft + width / 2f - titleLayout.size.width / 2f, size.height - 12f)
                            )
                        }

                        // Floating Badge for Selected Point
                        selectedPointIndex?.let { idx ->
                            val pt = recentPoints.getOrNull(idx)
                            if (pt != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .offset {
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            val stepX = if (recentPoints.size > 1) width / (recentPoints.size - 1) else width
                                            val centerX = scrollPaddingLeft + (idx * stepX)

                                            val tooltipWidthPx = 80.dp.toPx()
                                            val xOffset = (centerX - tooltipWidthPx / 2f).coerceIn(
                                                scrollPaddingLeft,
                                                maxOf(scrollPaddingLeft, canvasWidth - paddingRight - tooltipWidthPx)
                                            )
                                            IntOffset(xOffset.toInt(), 2.dp.toPx().toInt())
                                        }
                                        .width(80.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.surface.copy(alpha = 0.9f))
                                        .border(1.dp, colors.textAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${pt.rate} RPO",
                                        color = colors.textAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (scrollState.maxValue > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "◀",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val targetValue = (scrollState.value - 100).coerceAtLeast(0)
                                scrollState.animateScrollTo(targetValue)
                            }
                        }
                        .padding(4.dp)
                )
                Slider(
                    value = scrollState.value.toFloat().coerceIn(0f, scrollState.maxValue.toFloat()),
                    onValueChange = { newValue ->
                        coroutineScope.launch {
                            scrollState.scrollTo(newValue.toInt())
                        }
                    },
                    valueRange = 0f..scrollState.maxValue.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.textAccent,
                        activeTrackColor = colors.textAccent.copy(alpha = 0.5f),
                        inactiveTrackColor = colors.grid.copy(alpha = 0.2f)
                    )
                )
                Text(
                    text = "▶",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val targetValue = (scrollState.value + 100).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(targetValue)
                            }
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun WormLineChart(
    teamAProgress: List<RunProgressPoint>,
    teamBProgress: List<RunProgressPoint>,
    totalOvers: Int,
    colors: CricketColorScheme,
    modifier: Modifier = Modifier,
    ballHistory: List<BallHistoryEntry> = emptyList(),
    teamAName: String = "Team A",
    teamBName: String = "Team B"
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedBall by remember { mutableStateOf<Int?>(null) }
    var canvasWidth by remember { mutableStateOf(0f) }
    var canvasHeight by remember { mutableStateOf(0f) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    val paddingLeft = with(LocalDensity.current) { 35.dp.toPx() }
    val paddingBottom = with(LocalDensity.current) { 25.dp.toPx() }
    val paddingTop = with(LocalDensity.current) { 15.dp.toPx() }
    val paddingRight = with(LocalDensity.current) { 15.dp.toPx() }

    val maxBallsOverall = maxOf(
        if (teamAProgress.isNotEmpty()) teamAProgress.last().ball else 0,
        if (teamBProgress.isNotEmpty()) teamBProgress.last().ball else 0,
        totalOvers * 6
    )

    val totalOversToShow = maxOf(totalOvers, (maxBallsOverall + 5) / 6)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(12.dp)
            .animateContentSize()
    ) {
        // Legend and hint
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(colors.teamA)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = teamAName,
                    color = colors.textSecondary,
                    fontSize = 10.sp,
                    style = TextStyle(lineHeight = 10.sp)
                )
                if (teamBProgress.size > 1) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(colors.teamB)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = teamBName,
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        style = TextStyle(lineHeight = 10.sp)
                    )
                }
            }

            if (maxBallsOverall > 0) {
                Text(
                    text = if (selectedBall == null) "Slide to compare" else "Tap again to clear",
                    color = colors.textAccent.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 150.dp)
        ) {
            val totalWidth = maxWidth
            val yAxisWidth = 35.dp
            val scrollableAreaWidth = maxOf(totalWidth - yAxisWidth, (totalOversToShow * 55).dp)

            val density = LocalDensity.current
            val scrollPaddingLeft = with(density) { 15.dp.toPx() }
            val scrollableAreaWidthPx = with(density) { scrollableAreaWidth.toPx() }

            Row(modifier = Modifier.fillMaxSize()) {
                // 1. Pinned Y-Axis
                Canvas(
                    modifier = Modifier
                        .width(yAxisWidth)
                        .fillMaxHeight()
                ) {
                    val height = size.height - paddingTop - paddingBottom
                    val maxRunsA = teamAProgress.maxOfOrNull { it.runs } ?: 0
                    val maxRunsB = teamBProgress.maxOfOrNull { it.runs } ?: 0
                    val maxRunsOverall = maxOf(maxRunsA, maxRunsB, 10)
                    val maxValY = ((maxRunsOverall + 9) / 10) * 10f

                    val gridLineCount = 4
                    for (i in 0..gridLineCount) {
                        val ratio = i.toFloat() / gridLineCount
                        val y = paddingTop + height * (1f - ratio)
                        val value = ratio * maxValY

                        val textLayoutResult = textMeasurer.measure(
                            text = value.toInt().toString(),
                            style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                        )
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(size.width - textLayoutResult.size.width - 5f, y - textLayoutResult.size.height / 2f)
                        )
                    }
                }

                // 2. Horizontally scrollable content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .horizontalScroll(scrollState)
                ) {
                    Box(
                        modifier = Modifier
                            .width(scrollableAreaWidth)
                            .fillMaxHeight()
                            .onSizeChanged {
                                canvasWidth = it.width.toFloat()
                                canvasHeight = it.height.toFloat()
                            }
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(maxBallsOverall, scrollableAreaWidthPx) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0 && maxBallsOverall > 0) {
                                                val relativeX = offset.x - scrollPaddingLeft
                                                val stepX = width / (totalOversToShow * 6f)
                                                val ballIdx = (relativeX / stepX + 0.5f).toInt().coerceIn(0, totalOversToShow * 6)
                                                selectedBall = if (selectedBall == ballIdx) null else ballIdx
                                            }
                                        }
                                    )
                                }
                                .pointerInput(maxBallsOverall, scrollableAreaWidthPx) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0 && maxBallsOverall > 0) {
                                                val relativeX = offset.x - scrollPaddingLeft
                                                val stepX = width / (totalOversToShow * 6f)
                                                val ballIdx = (relativeX / stepX + 0.5f).toInt().coerceIn(0, totalOversToShow * 6)
                                                selectedBall = ballIdx
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            val width = canvasWidth - scrollPaddingLeft - paddingRight
                                            if (width > 0 && maxBallsOverall > 0) {
                                                val relativeX = change.position.x - scrollPaddingLeft
                                                val stepX = width / (totalOversToShow * 6f)
                                                val ballIdx = (relativeX / stepX + 0.5f).toInt().coerceIn(0, totalOversToShow * 6)
                                                selectedBall = ballIdx
                                            }
                                        },
                                        onDragEnd = {}
                                    )
                                }
                        ) {
                            val width = size.width - scrollPaddingLeft - paddingRight
                            val height = size.height - paddingTop - paddingBottom

                            if (maxBallsOverall <= 0) {
                                val noDataText = textMeasurer.measure(
                                    text = "No worm data yet.",
                                    style = TextStyle(color = colors.textSecondary, fontSize = 11.sp)
                                )
                                drawText(
                                    textLayoutResult = noDataText,
                                    topLeft = Offset(size.width / 2f - noDataText.size.width / 2f, size.height / 2f - noDataText.size.height / 2f)
                                )
                                return@Canvas
                            }

                            val maxRunsA = teamAProgress.maxOfOrNull { it.runs } ?: 0
                            val maxRunsB = teamBProgress.maxOfOrNull { it.runs } ?: 0
                            val maxRunsOverall = maxOf(maxRunsA, maxRunsB, 10)
                            val maxValY = ((maxRunsOverall + 9) / 10) * 10f

                            // Draw grid lines (horizontal only)
                            val gridLineCount = 4
                            for (i in 0..gridLineCount) {
                                val ratio = i.toFloat() / gridLineCount
                                val y = paddingTop + height * (1f - ratio)

                                drawLine(
                                    color = colors.grid,
                                    start = Offset(0f, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 1f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                                )
                            }

                            // X-Axis labels (Overs)
                            val stepX = width / (totalOversToShow * 6f)

                            for (over in 0..totalOversToShow) {
                                val x = scrollPaddingLeft + (over * 6 * stepX)

                                // Grid tick
                                drawLine(
                                    color = colors.grid.copy(alpha = 0.3f),
                                    start = Offset(x, paddingTop),
                                    end = Offset(x, size.height - paddingBottom),
                                    strokeWidth = 1f
                                )

                                val labelText = over.toString()
                                val textLayoutResult = textMeasurer.measure(
                                    text = labelText,
                                    style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                                )
                                drawText(
                                    textLayoutResult = textLayoutResult,
                                    topLeft = Offset(x - textLayoutResult.size.width / 2f, size.height - paddingBottom + 4f)
                                )
                            }

                            // Helper to draw a single team worm
                            fun drawTeamWorm(
                                progress: List<RunProgressPoint>,
                                lineColor: Color,
                                fillColor: Color
                            ) {
                                if (progress.size <= 1) return

                                val linePath = Path()
                                val fillPath = Path()

                                progress.forEachIndexed { idx, pt ->
                                    val x = scrollPaddingLeft + (pt.ball * stepX)
                                    val y = paddingTop + height * (1f - (pt.runs.toFloat() / maxValY))

                                    if (idx == 0) {
                                        linePath.moveTo(x, y)
                                        fillPath.moveTo(x, paddingTop + height)
                                        fillPath.lineTo(x, y)
                                    } else {
                                        linePath.lineTo(x, y)
                                        fillPath.lineTo(x, y)
                                    }

                                    if (idx == progress.size - 1) {
                                        fillPath.lineTo(x, paddingTop + height)
                                        fillPath.close()
                                    }
                                }

                                // Draw Fill
                                val fillBrush = Brush.verticalGradient(
                                    colors = listOf(fillColor.copy(alpha = 0.15f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = paddingTop + height
                                )
                                drawPath(path = fillPath, brush = fillBrush)

                                // Draw Line
                                drawPath(
                                    path = linePath,
                                    color = lineColor,
                                    style = Stroke(width = 4f)
                                )

                                // Draw Last Point Circle for current marker
                                if (progress.isNotEmpty() && selectedBall == null) {
                                    val lastPt = progress.last()
                                    val lastX = scrollPaddingLeft + (lastPt.ball * stepX)
                                    val lastY = paddingTop + height * (1f - (lastPt.runs.toFloat() / maxValY))
                                    drawCircle(
                                        color = colors.surface,
                                        radius = 6f,
                                        center = Offset(lastX, lastY)
                                    )
                                    drawCircle(
                                        color = lineColor,
                                        radius = 4f,
                                        center = Offset(lastX, lastY)
                                    )
                                }
                            }

                            // Draw Team A Worm
                            drawTeamWorm(teamAProgress, colors.teamA, colors.teamA)

                            // Draw Team B Worm
                            drawTeamWorm(teamBProgress, colors.teamB, colors.teamB)

                            // Draw Guide and highlight elements if a ball is selected
                            selectedBall?.let { ball ->
                                val x = scrollPaddingLeft + (ball * stepX)

                                // Vertical dashed line
                                drawLine(
                                    color = colors.textAccent.copy(alpha = 0.8f),
                                    start = Offset(x, paddingTop),
                                    end = Offset(x, paddingTop + height),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                )

                                // Highlight for Team A point if available
                                val runsA = getScoreAtBall(teamAProgress, ball)
                                if (ball <= (teamAProgress.lastOrNull()?.ball ?: 0)) {
                                    val yA = paddingTop + height * (1f - (runsA.toFloat() / maxValY))
                                    drawCircle(
                                        color = colors.surface,
                                        radius = 8f,
                                        center = Offset(x, yA)
                                    )
                                    drawCircle(
                                        color = colors.teamA,
                                        radius = 6f,
                                        center = Offset(x, yA)
                                    )
                                    drawCircle(
                                        color = colors.teamA.copy(alpha = 0.3f),
                                        radius = 12f,
                                        center = Offset(x, yA),
                                        style = Stroke(width = 2f)
                                    )
                                }

                                // Highlight for Team B point if available
                                if (teamBProgress.isNotEmpty() && ball <= (teamBProgress.lastOrNull()?.ball ?: 0)) {
                                    val runsB = getScoreAtBall(teamBProgress, ball)
                                    val yB = paddingTop + height * (1f - (runsB.toFloat() / maxValY))
                                    drawCircle(
                                        color = colors.surface,
                                        radius = 8f,
                                        center = Offset(x, yB)
                                    )
                                    drawCircle(
                                        color = colors.teamB,
                                        radius = 6f,
                                        center = Offset(x, yB)
                                    )
                                    drawCircle(
                                        color = colors.teamB.copy(alpha = 0.3f),
                                        radius = 12f,
                                        center = Offset(x, yB),
                                        style = Stroke(width = 2f)
                                    )
                                }
                            }

                            val titleLayout = textMeasurer.measure(
                                text = "Overs",
                                style = TextStyle(color = colors.textSecondary, fontSize = 8.sp)
                            )
                            drawText(
                                textLayoutResult = titleLayout,
                                topLeft = Offset(scrollPaddingLeft + width / 2f - titleLayout.size.width / 2f, size.height - 12f)
                            )
                        }

                        // Floating value badge for Worm
                        selectedBall?.let { ball ->
                            val runsA = getScoreAtBall(teamAProgress, ball)
                            val runsB = getScoreAtBall(teamBProgress, ball)
                            val hasB = teamBProgress.isNotEmpty() && ball <= (teamBProgress.lastOrNull()?.ball ?: 0)

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset {
                                        val width = canvasWidth - scrollPaddingLeft - paddingRight
                                        val stepX = width / (totalOversToShow * 6f)
                                        val centerX = scrollPaddingLeft + (ball * stepX)

                                        val tooltipWidthPx = 110.dp.toPx()
                                        val xOffset = (centerX - tooltipWidthPx / 2f).coerceIn(
                                            scrollPaddingLeft,
                                            maxOf(scrollPaddingLeft, canvasWidth - paddingRight - tooltipWidthPx)
                                        )
                                        IntOffset(xOffset.toInt(), 2.dp.toPx().toInt())
                                    }
                                    .width(110.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surface.copy(alpha = 0.9f))
                                    .border(1.dp, colors.textAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (hasB) "${teamAName.take(8)}: $runsA | ${teamBName.take(8)}: $runsB" else "$teamAName: $runsA",
                                    color = colors.textAccent,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
        if (scrollState.maxValue > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "◀",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val targetValue = (scrollState.value - 100).coerceAtLeast(0)
                                scrollState.animateScrollTo(targetValue)
                            }
                        }
                        .padding(4.dp)
                )
                Slider(
                    value = scrollState.value.toFloat().coerceIn(0f, scrollState.maxValue.toFloat()),
                    onValueChange = { newValue ->
                        coroutineScope.launch {
                            scrollState.scrollTo(newValue.toInt())
                        }
                    },
                    valueRange = 0f..scrollState.maxValue.toFloat(),
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = colors.textAccent,
                        activeTrackColor = colors.textAccent.copy(alpha = 0.5f),
                        inactiveTrackColor = colors.grid.copy(alpha = 0.2f)
                    )
                )
                Text(
                    text = "▶",
                    color = colors.textAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val targetValue = (scrollState.value + 100).coerceAtMost(scrollState.maxValue)
                                scrollState.animateScrollTo(targetValue)
                            }
                        }
                        .padding(4.dp)
                )
            }
        }
    }
}

// Helper to get score at specific ball
private fun getScoreAtBall(progress: List<RunProgressPoint>, ball: Int): Int {
    if (progress.isEmpty()) return 0
    val pt = progress.lastOrNull { it.ball <= ball }
    return pt?.runs ?: 0
}
