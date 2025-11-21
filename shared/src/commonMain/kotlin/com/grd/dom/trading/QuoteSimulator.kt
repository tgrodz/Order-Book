package com.grd.dom.trading

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class QuoteSimulator(
    private val totalCells: Int = 100,
    private val maxHistory: Int = 100,
    private val random: Random = Random.Default
) {
    private var currentBuyStart = 50
    private var currentSpread = 0
    private var aggressivenessMode: AggressivenessMode = AggressivenessMode.CALM
    private var tradingActive = true

    private val buyOrders = IntArray(totalCells)
    private val sellOrders = IntArray(totalCells)

    private val bestBuyHistory = ArrayDeque<Int>()
    private val bestSellHistory = ArrayDeque<Int>()
    private val midHistory = ArrayDeque<Int>()

    fun setAggressiveness(mode: AggressivenessMode) {
        aggressivenessMode = mode
    }

    fun startTrading() {
        tradingActive = true
        clearHistories()
    }

    fun stopTrading() {
        tradingActive = false
        clearBooks()
        clearHistories()
    }

    fun isTrading(): Boolean = tradingActive

    fun nextDelayMillis(): Long = when (aggressivenessMode) {
        AggressivenessMode.CALM -> 1000L
        AggressivenessMode.MODERATELY_SHARP -> 500L
        AggressivenessMode.SHARP -> 300L
        AggressivenessMode.AGGRESSIVE -> 150L
        AggressivenessMode.MIXED -> listOf(1000L, 500L, 300L, 150L).random(random)
        AggressivenessMode.PANIC -> 5L
    }

    private fun clearBooks() {
        buyOrders.fill(0)
        sellOrders.fill(0)
    }

    private fun clearHistories() {
        bestBuyHistory.clear()
        bestSellHistory.clear()
        midHistory.clear()
    }

    private fun clamp(value: Int, minValue: Int, maxValue: Int): Int = max(minValue, min(value, maxValue))

    private fun buyDelta(): Int = when (aggressivenessMode) {
        AggressivenessMode.CALM -> random.nextInt(3) - 1
        AggressivenessMode.MODERATELY_SHARP -> random.nextInt(5) - 2
        AggressivenessMode.SHARP -> random.nextInt(7) - 3
        AggressivenessMode.AGGRESSIVE -> {
            val delta = random.nextInt(21) - 10
            val candidate = currentBuyStart + delta
            clamp(candidate, 10, 80 - currentSpread) - currentBuyStart
        }
        AggressivenessMode.MIXED -> when (random.nextInt(4)) {
            0 -> random.nextInt(3) - 1
            1 -> random.nextInt(5) - 2
            2 -> random.nextInt(7) - 3
            else -> {
                val delta = random.nextInt(21) - 10
                val candidate = currentBuyStart + delta
                clamp(candidate, 10, 80 - currentSpread) - currentBuyStart
            }
        }
        AggressivenessMode.PANIC -> random.nextInt(3) - 1
    }

    private fun spreadDelta(): Int = random.nextInt(3) - 1

    private fun updateBooks() {
        if (!tradingActive) {
            clearBooks()
            return
        }
        clearBooks()
        currentBuyStart += buyDelta()
        currentSpread += spreadDelta()
        currentSpread = clamp(currentSpread, 0, 5)
        if (currentBuyStart < 10) currentBuyStart = 10
        if (currentBuyStart > 80 - currentSpread) currentBuyStart = 80 - currentSpread

        for (price in currentBuyStart until (currentBuyStart + 10)) {
            val qty = random.nextInt(11)
            if (qty > 0) buyOrders[price] = qty
        }
        val sellStart = currentBuyStart + 10 + currentSpread
        for (price in sellStart until (sellStart + 10)) {
            val qty = random.nextInt(11)
            if (qty > 0) sellOrders[price] = qty
        }

        val bestBuy = (10 until totalCells).filter { buyOrders[it] > 0 }.maxOrNull() ?: -1
        val bestSell = (10 until totalCells).filter { sellOrders[it] > 0 }.minOrNull() ?: 91
        val mid = if (bestBuy >= 10 && bestSell <= 90) (bestBuy + bestSell) / 2 else -1

        if (mid != -1) {
            if (midHistory.size == maxHistory) {
                bestBuyHistory.removeFirst()
                bestSellHistory.removeFirst()
                midHistory.removeFirst()
            }
            bestBuyHistory.addLast(bestBuy)
            bestSellHistory.addLast(bestSell)
            midHistory.addLast(mid)
        }
    }

    fun snapshot(): QuoteMessage {
        val buyList = (10 until totalCells).mapNotNull { price ->
            val qty = buyOrders[price]
            if (qty > 0) OrderData(price, qty) else null
        }
        val sellList = (10 until totalCells).mapNotNull { price ->
            val qty = sellOrders[price]
            if (qty > 0) OrderData(price, qty) else null
        }
        val history = HistoryData(
            bestBuy = bestBuyHistory.toList(),
            bestSell = bestSellHistory.toList(),
            mid = midHistory.toList()
        )
        return QuoteMessage(buy = buyList, sell = sellList, history = history)
    }

    fun nextQuote(): QuoteMessage {
        updateBooks()
        return snapshot()
    }
}
