package com.grd.dom.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.grd.dom.model.trading.Trade
import com.grd.dom.model.trading.TradeRecord
import com.grd.dom.model.trading.TradingViewModel
import com.grd.dom.model.trading.UserOrder
import com.grd.dom.trading.AggressivenessMode
import com.grd.dom.trading.QuoteMessage
import com.grd.dom.trading.RemoteCommand
import com.grd.dom.trading.TradeSide
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.max
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.min

@Composable
fun TradingScreen(
    modifier: Modifier = Modifier,
    viewModel: TradingViewModel = remember { TradingViewModel() },
    onRemoteToggleRequest: (Boolean) -> Boolean = { true },
    forceLocalMode: Boolean = false,
    loggedInLabel: String? = null,
    tokenLabel: String? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    val scaffoldState = rememberScaffoldState()
    val orderBookListState = rememberLazyListState()
    var showTradeDialog by remember { mutableStateOf(false) }
    var tradePriceInput by remember { mutableStateOf("") }
    var tradeLotsInput by remember { mutableStateOf("1") }
    var tradeAutoCloseInput by remember { mutableStateOf("15") }
    var tradeType by remember { mutableStateOf(TradeSide.BUY) }

    DisposableEffect(Unit) { onDispose { viewModel.clear() } }

    LaunchedEffect(Unit) {
        snapshotFlow { viewModel.latestMid }
            .debounce(800L)
            .collectLatest { mid ->
                if (viewModel.autoScroll && mid != null) {
                    orderBookListState.animateScrollToItem(99 - mid)
                }
            }
    }

    LaunchedEffect(forceLocalMode) {
        if (forceLocalMode && !viewModel.useLocal) {
            viewModel.useLocal = true
            viewModel.disconnect()
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        floatingActionButton = {
            Column {
                FloatingActionButton(
                    onClick = { viewModel.cancelOrderOrCloseTradeEarly() },
                    backgroundColor = Color.Black.copy(alpha = 0.6f)
                ) { Icon(Icons.Default.Clear, contentDescription = "Cancel/Close") }
                Spacer(modifier = Modifier.height(10.dp))
                FloatingActionButton(
                    onClick = { showTradeDialog = true },
                    backgroundColor = Color.Black.copy(alpha = 0.6f)
                ) { Icon(Icons.Default.Add, contentDescription = "New Trade") }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ControlsSection(
                    viewModel = viewModel,
                    onRemoteToggleRequest = onRemoteToggleRequest,
                    loggedInLabel = loggedInLabel,
                    tokenLabel = tokenLabel,
                    onLogoutClick = onLogoutClick
                )
            }
            item {
                Row(modifier = Modifier.height(600.dp)) {
                    Box(
                        modifier = Modifier
                            .weight(4f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
                            .padding(6.dp)
                    ) { ChartSection(viewModel) }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(1.dp, Color.Gray)
                    ) {
                        DomPanel(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize(),
                            listState = orderBookListState
                        )
                    }
                }
            }
            item { HistorySection(viewModel) }
            item { TradeInfoSection(viewModel) }
        }
    }

    if (showTradeDialog) {
        TradeDialog(
            tradePriceInput = tradePriceInput,
            onPriceChange = { tradePriceInput = it },
            tradeLotsInput = tradeLotsInput,
            onLotsChange = { tradeLotsInput = it },
            tradeAutoCloseInput = tradeAutoCloseInput,
            onAutoCloseChange = { tradeAutoCloseInput = it },
            tradeType = tradeType,
            onTypeChange = { tradeType = it },
            onDismiss = { showTradeDialog = false }
        ) {
            val price = tradePriceInput.toIntOrNull()
            val lots = tradeLotsInput.toIntOrNull()
            val autoClose = tradeAutoCloseInput.toIntOrNull()
            if (price != null && lots != null && lots > 0 && autoClose != null) {
                viewModel.userPendingOrder = UserOrder(tradeType, price, lots, autoClose)
                showTradeDialog = false
            }
        }
    }
}



@Composable
fun ControlsSection(
    viewModel: TradingViewModel,
    onRemoteToggleRequest: (Boolean) -> Boolean = { true },
    loggedInLabel: String? = null,
    tokenLabel: String? = null,
    onLogoutClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text("Order Book and Chart", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Remote")
            Switch(
                checked = viewModel.useLocal,
                onCheckedChange = { desiredUseLocal ->
                    val allowed = onRemoteToggleRequest(desiredUseLocal)
                    if (!allowed) return@Switch
                    viewModel.useLocal = desiredUseLocal
                    viewModel.disconnect()
                    viewModel.connect()
                }
            )
            Text("Local")

            Button(onClick = { viewModel.connect() }, enabled = !viewModel.isConnected) {
                Text("Connect")
            }
            Button(onClick = { viewModel.disconnect() }, enabled = viewModel.isConnected) {
                Text("Disconnect")
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (viewModel.isConnected) Color(0xFF2E7D32)
                        else Color(0xFFC62828)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    if (viewModel.isConnected) "Connected" else "Disconnected",
                    color = Color.White
                )
            }

            OutlinedTextField(
                value = viewModel.serverEndpoint,
                onValueChange = { viewModel.serverEndpoint = it },
                label = { Text("Server WS URL") },
                modifier = Modifier.width(260.dp),
                singleLine = true
            )

            Text("Aggressiveness:")
            DropdownMenuAggressiveness(selected = viewModel.selectedAggressiveness) { newVal ->
                viewModel.selectedAggressiveness = newVal
                viewModel.sendCommand(RemoteCommand(command = "setAggressiveness", mode = newVal))
            }

            Button(onClick = { viewModel.sendCommand(RemoteCommand("startTrading")) }) {
                Text("Start")
            }
            Button(onClick = { viewModel.sendCommand(RemoteCommand("stopTrading")) }) {
                Text("Stop")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = viewModel.autoScroll,
                    onCheckedChange = { viewModel.autoScroll = it }
                )
                Text("Auto-scroll")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Small Font")
                Switch(
                    checked = viewModel.smallFont,
                    onCheckedChange = { viewModel.smallFont = it }
                )
            }

            if (loggedInLabel != null) Text(loggedInLabel, fontSize = 12.sp)
            if (tokenLabel != null) Text("Token: $tokenLabel", fontSize = 10.sp)

            if (onLogoutClick != null) {
                Button(onClick = onLogoutClick) { Text("Logout") }
            }
        }
    }
}



@Composable
private fun TradeDialog(
    tradePriceInput: String,
    onPriceChange: (String) -> Unit,
    tradeLotsInput: String,
    onLotsChange: (String) -> Unit,
    tradeAutoCloseInput: String,
    onAutoCloseChange: (String) -> Unit,
    tradeType: TradeSide,
    onTypeChange: (TradeSide) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colors.surface,
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Open Trade Order", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tradePriceInput,
                    onValueChange = onPriceChange,
                    label = { Text("Price") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Order Type:")
                    Spacer(Modifier.width(6.dp))
                    DropdownMenuTradeType(selected = tradeType, onSelectedChange = onTypeChange)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tradeLotsInput,
                    onValueChange = onLotsChange,
                    label = { Text("Number of Lots") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tradeAutoCloseInput,
                    onValueChange = onAutoCloseChange,
                    label = { Text("Auto-Close Threshold (points)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onConfirm) { Text("OK") }
                }
            }
        }
    }
}

@Composable
fun DropdownMenuTradeType(selected: TradeSide, onSelectedChange: (TradeSide) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = TradeSide.values().toList()
    Box {
        Text(
            text = selected.toString(),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { label ->
                DropdownMenuItem(
                    onClick = {
                        onSelectedChange(label)
                        expanded = false
                    }
                ) {
                    Text(label.toString())
                }
            }
        }
    }
}



@Composable
fun DropdownMenuAggressiveness(selected: Int, onSelectedChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val items = AggressivenessMode.values()
    Box {
        Text(
            text = items.firstOrNull { it.level == selected }?.nameReadable() ?: "Calm",
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF5F5F5))
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { mode ->
                DropdownMenuItem(
                    onClick = {
                        onSelectedChange(mode.level)
                        expanded = false
                    }
                ) {
                    Text(mode.nameReadable())
                }
            }
        }
    }
}

private fun AggressivenessMode.nameReadable(): String = when (this) {
    AggressivenessMode.CALM -> "Calm"
    AggressivenessMode.MODERATELY_SHARP -> "Moderately Sharp"
    AggressivenessMode.SHARP -> "Sharp"
    AggressivenessMode.AGGRESSIVE -> "Aggressive"
    AggressivenessMode.MIXED -> "Mixed"
    AggressivenessMode.PANIC -> "Panic"
}

/**
 * Chart + last price label.
 * NOTE: PricePill is drawn OUTSIDE Canvas – no composables inside draw lambda.
 */
@Composable
fun ChartSection(viewModel: TradingViewModel) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val history = viewModel.midHistory

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Background
            drawRect(color = Color.White, size = size)

            // Grid
            val vStep = 40.dp.toPx()
            val hStep = 30.dp.toPx()
            val gridColor = Color.Gray.copy(alpha = 0.15f)

            var gx = 0f
            while (gx <= w) {
                drawLine(
                    gridColor,
                    start = Offset(gx, 0f),
                    end = Offset(gx, h),
                    strokeWidth = 0.5.dp.toPx()
                )
                gx += vStep
            }
            var gy = 0f
            while (gy <= h) {
                drawLine(
                    gridColor,
                    start = Offset(0f, gy),
                    end = Offset(w, gy),
                    strokeWidth = 0.5.dp.toPx()
                )
                gy += hStep
            }

            // Soft horizontal levels
            val axisColor = Color.Gray.copy(alpha = 0.08f)
            for (lvl in 0..100 step 20) {
                val y = h - (lvl / 100f) * h
                drawLine(
                    axisColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 0.5.dp.toPx()
                )
            }

            // Price line + markers
            if (history.size >= 2) {
                val xStep = if (history.size > 1) w / (history.size - 1) else 0f

                val path = Path().apply {
                    history.forEachIndexed { i, mid ->
                        val x = i * xStep
                        val y = h - (mid / 100f) * h
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = Color(0xFF1E88E5),
                    style = Stroke(width = 2.dp.toPx())
                )

                history.forEachIndexed { i, mid ->
                    val x = i * xStep
                    val y = h - (mid / 100f) * h
                    drawCircle(
                        color = Color(0xFFD32F2F),
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }

                val last = history.last()
                val lastY = h - (last / 100f) * h

                drawLine(
                    color = Color(0xFF1E88E5).copy(alpha = 0.6f),
                    start = Offset(0f, lastY),
                    end = Offset(w, lastY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                )
            }
        }

        // Last price pill, positioned using the same normalized fraction
        val last = history.lastOrNull()
        if (last != null) {
            val yFraction = 1f - last / 100f
            val rawY = maxHeight * yFraction
            PricePill(value = last, y = rawY.clamp(0.dp, maxHeight), containerHeight = maxHeight)
        }

        // Y-axis labels
        val labels = listOf(0, 20, 40, 60, 80, 100)
        labels.forEach { lvl ->
            val yFraction = 1f - lvl / 100f
            val y = (maxHeight * yFraction).clamp(0.dp, maxHeight)
            Text(
                text = lvl.toString(),
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-6).dp, y = y - 8.dp)
            )
        }
    }
}

@Composable
private fun PricePill(value: Int, y: Dp, containerHeight: Dp) {
    val pillHeight = 22.dp
    val clampedY = when {
        y < 0.dp -> 0.dp
        y > containerHeight - pillHeight -> containerHeight - pillHeight
        else -> y
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Little triangle pointer
        Canvas(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-18).dp, y = clampedY + pillHeight / 2 - 4.dp)
                .size(8.dp)
        ) {
            val path = Path().apply {
                moveTo(size.width, size.height / 2)
                lineTo(0f, 0f)
                lineTo(0f, size.height)
                close()
            }
            drawPath(path, color = Color(0xFF1E88E5))
        }
        // Price pill
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = clampedY),
            color = Color(0xFF1E88E5),
            shape = RoundedCornerShape(10.dp),
            elevation = 2.dp
        ) {
            Text(
                text = value.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

private fun Dp.clamp(min: Dp, max: Dp): Dp = when {
    this < min -> min
    this > max -> max
    else -> this
}

@Composable
fun DomPanel(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState()
) {
    val density = LocalDensity.current
    val rowHeightPx = with(density) { (if (viewModel.smallFont) 15.dp else 30.dp).toPx() }

    // Pause auto-centering while the user scrolls manually
    var pauseAuto by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            pauseAuto = true
        } else {
            delay(1200)
            pauseAuto = false
        }
    }

    // Initial snap to mid
    LaunchedEffect(Unit) {
        delay(300)
        viewModel.latestMid?.let { mid ->
            val vp = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            val centerOffset = (vp / 2f - rowHeightPx / 2f).toInt()
            listState.scrollToItem(index = 99 - mid, scrollOffset = -centerOffset)
        }
    }

    // Keep mid centered while autoScroll is ON
    LaunchedEffect(viewModel.latestMid, viewModel.autoScroll, pauseAuto, rowHeightPx) {
        val mid = viewModel.latestMid ?: return@LaunchedEffect
        if (!viewModel.autoScroll || pauseAuto) return@LaunchedEffect
        val vp = listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        val centerOffset = (vp / 2f - rowHeightPx / 2f).toInt()
        listState.animateScrollToItem(index = 99 - mid, scrollOffset = -centerOffset)
    }

    Column(modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F2F2))
                .padding(vertical = 4.dp)
        ) {
            Text(
                "DOM",
                modifier = Modifier.align(Alignment.Center),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Clip children to this box
        Box(
            Modifier
                .fillMaxSize()
                .clipToBounds()
        ) {
            OrderBookSection(viewModel = viewModel, listState = listState)
        }
    }
}


@Composable
fun OrderBookSection(viewModel: TradingViewModel, listState: LazyListState) {
    val rowHeightDp = if (viewModel.smallFont) 15.dp else 30.dp
    val density = LocalDensity.current

    Box(modifier = Modifier.fillMaxSize()) {

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(100) { index ->
                val level = 99 - index
                val pending = viewModel.userPendingOrder

                val isBuyLevel =
                    viewModel.buyOrders.containsKey(level) ||
                            (pending?.type == TradeSide.BUY && pending.price == level)

                val isSellLevel =
                    viewModel.sellOrders.containsKey(level) ||
                            (pending?.type == TradeSide.SELL && pending.price == level)

                val rowColor = when {
                    isBuyLevel -> Color(0xFF8BC34A)
                    isSellLevel -> Color(0xFFF44336)
                    else -> Color.White
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeightDp)
                        .background(rowColor)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isBuyLevel) {
                        val serverQty = viewModel.buyOrders[level] ?: 0
                        val userQty =
                            if (pending?.type == TradeSide.BUY && pending.price == level) pending.lots else 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Buy",
                                tint = Color.Black,
                                modifier = Modifier.size(if (viewModel.smallFont) 10.dp else 14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "$serverQty",
                                fontSize = if (viewModel.smallFont) 10.sp else 12.sp,
                                color = Color.Black
                            )
                            if (userQty > 0) {
                                Text(
                                    " +$userQty",
                                    fontSize = if (viewModel.smallFont) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    } else if (isSellLevel) {
                        val serverQty = viewModel.sellOrders[level] ?: 0
                        val userQty =
                            if (pending?.type == TradeSide.SELL && pending.price == level) pending.lots else 0
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Sell",
                                tint = Color.Black,
                                modifier = Modifier.size(if (viewModel.smallFont) 10.dp else 14.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "$serverQty",
                                fontSize = if (viewModel.smallFont) 10.sp else 12.sp,
                                color = Color.Black
                            )
                            if (userQty > 0) {
                                Text(
                                    " +$userQty",
                                    fontSize = if (viewModel.smallFont) 10.sp else 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        "$level",
                        fontSize = if (viewModel.smallFont) 10.sp else 12.sp,
                        color = Color.Black,
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // P/L stripe clamped to the visible viewport
        if (viewModel.openTrade != null && viewModel.latestMid != null) {
            val trade = viewModel.openTrade!!
            val latestMid = viewModel.latestMid!!

            val rowHeightPx = with(density) { rowHeightDp.toPx() }

            val openYAbs = (99 - trade.openPrice) * rowHeightPx
            val currentYAbs = (99 - latestMid) * rowHeightPx

            val viewportStart = listState.layoutInfo.viewportStartOffset
            val viewportEnd = listState.layoutInfo.viewportEndOffset
            val viewportH = (viewportEnd - viewportStart).toFloat()

            val scrollY =
                listState.firstVisibleItemIndex * rowHeightPx +
                        listState.firstVisibleItemScrollOffset

            val topRaw = min(openYAbs, currentYAbs) - scrollY
            val bottomRaw = max(openYAbs, currentYAbs) - scrollY + rowHeightPx

            val topClamped = topRaw.coerceIn(0f, viewportH)
            val bottomClamped = bottomRaw.coerceIn(0f, viewportH)
            val heightPx = (bottomClamped - topClamped).coerceAtLeast(1f)

            val profit =
                if (trade.type == TradeSide.BUY)
                    (latestMid - trade.openPrice) * trade.lots
                else
                    (trade.openPrice - latestMid) * trade.lots

            val tradeColor = if (profit >= 0) Color(0xFF006400) else Color(0xFF8B0000)

            Box(
                modifier = Modifier
                    .offset { IntOffset(0, topClamped.toInt()) }
                    .width(52.dp)
                    .height(with(density) { heightPx.toDp() })
                    .clip(RoundedCornerShape(topEnd = 6.dp, bottomEnd = 6.dp))
                    .background(tradeColor.copy(alpha = 0.45f))
            )
        }
    }
}


@Composable
fun HistorySection(viewModel: TradingViewModel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp)),
        color = Color.White
    ) {
        LazyRow(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HistoryList(
                    title = "Best Buy",
                    prices = viewModel.bestBuyHistory,
                    timestamps = viewModel.historyTimestamps
                )
            }
            item {
                HistoryList(
                    title = "Best Sell",
                    prices = viewModel.bestSellHistory,
                    timestamps = viewModel.historyTimestamps
                )
            }
            item {
                HistoryList(
                    title = "Mid Price",
                    prices = viewModel.midHistory,
                    timestamps = viewModel.historyTimestamps
                )
            }
        }
    }
}


@Composable
fun HistoryList(title: String, prices: List<Int>, timestamps: List<Instant>) {
    val count = min(prices.size, timestamps.size)
    val priceSegment = prices.takeLast(count)
    val timeSegment = timestamps.takeLast(count)

    // Average price for display in the header
    val avgPrice = if (priceSegment.isNotEmpty()) priceSegment.average() else null

    Column(
        modifier = Modifier
            .width(160.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFFCCCCCC), RoundedCornerShape(8.dp))
            .background(Color(0xFFF7F7F7))
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            if (avgPrice != null) {
                Text(
                    text = "Avg: ${"%.2f".format(avgPrice)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Divider()

        // The list itself is scrollable within the fixed-height card
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(priceSegment.zip(timeSegment)) { (price, time) ->
                Text(
                    text = "Price: $price, Time: ${time.formatTimestamp()}",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}



@Composable
fun TradeInfoSection(viewModel: TradingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFE9F1FF))
            .border(1.dp, Color(0xFFBBD0FF), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            "Balance: ${viewModel.userBalance} credits",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        viewModel.openTrade?.let { trade ->
            val mid = viewModel.latestMid ?: 0
            val pl =
                if (trade.type == TradeSide.BUY)
                    (mid - trade.openPrice) * trade.lots
                else
                    (trade.openPrice - mid) * trade.lots

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF4F6F8))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${trade.type.name} @ ${trade.openPrice} × ${trade.lots}",
                    fontSize = 14.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "P/L: ${if (pl >= 0) "+" else ""}$pl",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (pl >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Trade History", fontSize = 16.sp, fontWeight = FontWeight.Bold)

        if (viewModel.tradeHistory.isEmpty()) {
            Text(
                "No closed trades yet.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(modifier = Modifier.height(160.dp)) {
                itemsIndexed(viewModel.tradeHistory) { _, record ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF9FAFB))
                            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            "${record.type.name} - ${record.lots} lot(s)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (record.type == TradeSide.BUY)
                                Color(0xFF2E7D32)
                            else
                                Color(0xFFC62828)
                        )
                        Text(
                            "Open: ${record.openTime.formatTimestamp()}  •  Close: ${record.closeTime.formatTimestamp()}",
                            fontSize = 11.sp
                        )
                        Text(
                            "Result: ${if (record.result >= 0) "+" else ""}${record.result} credits",
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun Instant.formatTimestamp(): String {
    val local = toLocalDateTime(TimeZone.currentSystemDefault())
    val millis = (local.nanosecond / 1_000_000).toString().padStart(3, '0')
    return listOf(local.hour, local.minute, local.second)
        .joinToString(":") { it.toString().padStart(2, '0') } + ".$millis"
}

