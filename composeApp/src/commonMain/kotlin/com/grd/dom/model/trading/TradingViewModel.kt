package com.grd.dom.model.trading

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.grd.dom.SERVER_PORT
import com.grd.dom.trading.AggressivenessMode
import com.grd.dom.trading.QuoteMessage
import com.grd.dom.trading.QuoteSimulator
import com.grd.dom.trading.QuoteWebSocketClient
import com.grd.dom.trading.RemoteCommand
import com.grd.dom.trading.TradeSide
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class TradingViewModel {
    var useLocal by mutableStateOf(false)
    var isConnected by mutableStateOf(false)
    var buyOrders by mutableStateOf<Map<Int, Int>>(emptyMap())
    var sellOrders by mutableStateOf<Map<Int, Int>>(emptyMap())
    var bestBuyHistory by mutableStateOf<List<Int>>(emptyList())
    var bestSellHistory by mutableStateOf<List<Int>>(emptyList())
    var midHistory by mutableStateOf<List<Int>>(emptyList())
    var latestMid by mutableStateOf<Int?>(null)
    var historyTimestamps by mutableStateOf<List<Instant>>(emptyList())
    var selectedAggressiveness by mutableStateOf(AggressivenessMode.CALM.level)
    var autoScroll by mutableStateOf(true)
    var smallFont by mutableStateOf(false)
    var userBalance by mutableStateOf(1000)
    var userPendingOrder by mutableStateOf<UserOrder?>(null)
    var openTrade by mutableStateOf<Trade?>(null)
    var tradeHistory by mutableStateOf<List<TradeRecord>>(emptyList())
    var serverEndpoint by mutableStateOf("ws://10.0.2.2:$SERVER_PORT/quotes")

    private val scope = MainScope()
    private val simulator = QuoteSimulator()
    private var localJob: Job? = null
    private var remoteJob: Job? = null
    private var quoteClient: QuoteWebSocketClient? = null

    fun connect() {
        if (useLocal) startLocalConnection() else startRemoteConnection()
    }

    fun disconnect() {
        scope.launch {
            localJob?.cancel()
            localJob = null
            remoteJob?.cancel()
            remoteJob = null
            quoteClient?.close()
            quoteClient = null
            isConnected = false
        }
    }

    fun clear() {
        disconnect()
        scope.cancel()
    }

    private fun startLocalConnection() {
        if (localJob?.isActive == true) return
        simulator.startTrading()
        localJob = scope.launch {
            isConnected = true
            while (isActive) {
                handleMessage(simulator.nextQuote())
                delay(simulator.nextDelayMillis())
            }
        }
    }

    private fun startRemoteConnection() {
        if (remoteJob?.isActive == true) return
        remoteJob = scope.launch {
            val client = QuoteWebSocketClient(endpoint = serverEndpoint)
            quoteClient = client
            try {
                client.connect()
                isConnected = true
                client.updates.collect { handleMessage(it) }
            } catch (_: Throwable) {
                isConnected = false
            } finally {
                client.close()
                if (quoteClient === client) quoteClient = null
                isConnected = false
            }
        }
    }

    fun sendCommand(command: RemoteCommand) {
        if (useLocal) {
            when (command.command) {
                "setAggressiveness" -> command.mode?.let {
                    selectedAggressiveness = it
                    simulator.setAggressiveness(AggressivenessMode.fromLevel(it))
                }

                "startTrading" -> simulator.startTrading()
                "stopTrading" -> simulator.stopTrading()
            }
        } else {
            scope.launch {
                quoteClient?.sendCommand(command)
            }
        }
    }

    fun updateUserBalance(newBalance: Int) {
        userBalance = newBalance
    }

    fun placeOrder(order: UserOrder) {
        userPendingOrder = order
    }

    fun openTrade(trade: Trade) {
        openTrade = trade
    }

    fun closeTrade(trade: Trade, result: Int) {
        val record = TradeRecord(
            type = trade.type,
            openPrice = trade.openPrice,
            closePrice = latestMid ?: trade.openPrice,
            lots = trade.lots,
            openTime = trade.openTime,
            closeTime = Clock.System.now(),
            result = result
        )
        tradeHistory = tradeHistory + record
        openTrade = null
    }

    fun cancelOrderOrCloseTradeEarly() {
        userPendingOrder = null
        openTrade?.let {
            val mid = latestMid ?: return
            val result = computeProfit(it, mid)
            closeTrade(it, result)
        }
    }

    private fun handleMessage(message: QuoteMessage) {
        buyOrders  = message.buy.associate  { it.price to it.qty }
        sellOrders = message.sell.associate { it.price to it.qty }

        message.history.bestBuy.lastOrNull()?.let { bestBuyHistory  = appendWithLimit(bestBuyHistory, it) }
        message.history.bestSell.lastOrNull()?.let { bestSellHistory = appendWithLimit(bestSellHistory, it) }
        message.history.mid.lastOrNull()?.let {
            midHistory = appendWithLimit(midHistory, it)
            latestMid  = it
        }
        historyTimestamps = appendWithLimit(historyTimestamps, Clock.System.now())

        // --- execute pending order when crossed ---
        userPendingOrder?.let { pending ->
            val crossed = when (pending.type) {
                TradeSide.BUY  -> sellOrders.keys.any { it <= pending.price }
                TradeSide.SELL -> buyOrders.keys.any { it >= pending.price }
            }
            if (crossed) {
                openTrade = Trade(
                    type = pending.type,
                    openPrice = pending.price,
                    lots = pending.lots,
                    openTime = Clock.System.now(),
                    autoCloseThreshold = pending.autoCloseThreshold,
                    openIndex = midHistory.size
                )
                userPendingOrder = null
            }

        }

        // Auto-close open trade when threshold reached
        openTrade?.let { t ->
            val mid = latestMid ?: return@let
            val pl = computeProfit(t, mid)
            if (kotlin.math.abs(pl) >= t.autoCloseThreshold) {
                userBalance += pl
                closeTrade(t, pl)
            }
        }
    }

    private fun <T> appendWithLimit(list: List<T>, value: T, limit: Int = HISTORY_LIMIT): List<T> {
        val updated = list + value
        return if (updated.size > limit) updated.takeLast(limit) else updated
    }

    companion object {
        private const val HISTORY_LIMIT = 100
    }

    fun formatTimestamp(instant: Instant): String {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        val second = local.second.toString().padStart(2, '0')
        return "$hour:$minute:$second"
    }

    private fun computeProfit(trade: Trade, mid: Int): Int = when (trade.type) {
        TradeSide.BUY -> (mid - trade.openPrice) * trade.lots
        TradeSide.SELL -> (trade.openPrice - mid) * trade.lots
    }
}
