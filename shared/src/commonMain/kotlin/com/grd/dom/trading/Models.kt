package com.grd.dom.trading

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OrderData(val price: Int, val qty: Int)

@Serializable
data class HistoryData(
    val bestBuy: List<Int> = emptyList(),
    val bestSell: List<Int> = emptyList(),
    val mid: List<Int> = emptyList()
)

@Serializable
data class QuoteMessage(
    val buy: List<OrderData> = emptyList(),
    val sell: List<OrderData> = emptyList(),
    val history: HistoryData = HistoryData()
)

@Serializable
enum class TradeSide {
    @SerialName("buy") BUY,
    @SerialName("sell") SELL;

    override fun toString(): String = when (this) {
        BUY -> "buy"
        SELL -> "sell"
    }
}

@Serializable
data class RemoteCommand(
    val command: String,
    val mode: Int? = null
)

enum class AggressivenessMode(val level: Int) {
    CALM(1),
    MODERATELY_SHARP(2),
    SHARP(3),
    AGGRESSIVE(4),
    MIXED(5),
    PANIC(6);

    companion object {
        fun fromLevel(level: Int): AggressivenessMode = values().firstOrNull { it.level == level } ?: CALM
    }
}
