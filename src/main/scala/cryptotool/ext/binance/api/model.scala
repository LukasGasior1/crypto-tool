package cryptotool.ext.binance.api

import org.json4s.JsonAST.JValue

case class AccountResponse(
    makerCommission: Double,
    takerCommission: Double,
    buyerCommission: Double,
    sellerCommission: Double,
    canTrade: Boolean,
    canWithdraw: Boolean,
    canDeposit: Boolean,
    updateTime: Long,
    balances: Seq[AccountBalanceItem])

case class AccountBalanceItem(
    asset: String,
    free: String,
    locked: String)

case class AccountTrade(
    id: Long,
    orderId: Long,
    price: String,
    qty: String,
    commission: String,
    commissionAsset: String,
    time: Long,
    isBuyer: Boolean,
    isMaker: Boolean,
    isBestMatch: Boolean)

case class OrderBookResponse(
    lastUpdateId: Long,
    bids: Seq[Seq[JValue]],
    asks: Seq[Seq[JValue]])

case class RecentTrade(
    id: Long,
    price: String,
    qty: String,
    time: Long,
    isBuyerMaker: Boolean,
    isBestMatch: Boolean)

case class TickerPrice(symbol: String, price: String)

case class TickerStats24H(
    symbol: String,
    priceChange: String,
    priceChangePercent: String,
    weightedAvgPrice: String,
    prevClosePrice: String,
    lastPrice: String,
    lastQty: String,
    bidPrice: String,
    askPrice: String,
    openPrice: String,
    highPrice: String,
    lowPrice: String,
    volume: String,
    quoteVolume: String,
    openTime: Long,
    closeTime: Long,
    firstId: Long,
    lastId: Long,
    count: Long)
