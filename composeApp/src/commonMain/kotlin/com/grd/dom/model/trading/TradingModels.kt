package com.grd.dom.model.trading

import com.grd.dom.trading.TradeSide
import kotlinx.datetime.Instant

data class UserOrder(
    val type: TradeSide,
    val price: Int,
    val lots: Int,
    val autoCloseThreshold: Int
)

data class Trade(
    val type: TradeSide,
    val openPrice: Int,
    val lots: Int,
    val openTime: Instant,
    val autoCloseThreshold: Int,
    val openIndex: Int
)

data class TradeRecord(
    val type: TradeSide,
    val openPrice: Int,
    val closePrice: Int,
    val lots: Int,
    val openTime: Instant,
    val closeTime: Instant,
    val result: Int
)
