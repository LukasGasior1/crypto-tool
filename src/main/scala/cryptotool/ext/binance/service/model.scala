package cryptotool.ext.binance.service

import cryptotool.domain

case class Balance(
    asset: String,
    free: Double,
    locked: Double)

case class OrderBook(
    lastUpdateId: Long,
    bids: Seq[OrderBookBid],
    asks: Seq[OrderBookAsk])

case class OrderBookAsk(price: Double, quantity: Double)
case class OrderBookBid(price: Double, quantity: Double)

case class Summary(
    currencies: Seq[CurrencySummary])

case class CurrencySummary(
    balance: Balance,
    priceInBtc: Double,
    estimatedBtcValue: Double,
    estimatedUsdValue: Double,
    percentageChange24HBtc: Double,
    percentageChange24HUsd: Double)

case class TradeList(trades: Seq[Trade])

object Trade {
  implicit val ordering: Ordering[Trade] = Ordering.by[Trade, Long](_.id)
}

case class Trade(
    id: Long,
    orderId: Long,
    price: Double,
    qty: Double,
    commission: Double,
    commissionAsset: String,
    time: Long,
    isBuyer: Boolean,
    isMaker: Boolean,
    isBestMatch: Boolean) {

  lazy val netQty: Double =
    if (commissionAsset == domain.BTC) qty
    else qty - commission
}

case class RecentTradeList(recentTrades: Seq[RecentTrade])
case class RecentTrade(
    id: Long,
    price: Double,
    qty: Double,
    time: Long,
    isBuyerMaker: Boolean,
    isBestMatch: Boolean)

case class TickerPrice(symbol: String, price: Double)

case class TickerStats24H(
    symbol: String,
    priceChange: Double,
    priceChangePercent: Double,
    weightedAvgPrice: Double,
    prevClosePrice: Double,
    lastPrice: Double,
    lastQty: Double,
    bidPrice: Double,
    askPrice: Double,
    openPrice: Double,
    highPrice: Double,
    lowPrice: Double,
    volume: Double,
    quoteVolume: Double,
    openTime: Long,
    closeTime: Long,
    firstId: Long,
    lastId: Long,
    count: Long)