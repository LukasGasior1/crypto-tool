package cryptotool.ext.bittrex.service

case class CurrencySummary(
    currency: String,
    balance: Double,
    priceInBtc: Double,
    estimatedBtcValue: Double,
    estimatedUsdValue: Double)

case class Summary(
    currencies: Seq[CurrencySummary])

case class MarketSummary(
    marketName: String,
    high: Double,
    low: Double,
    volume: Double,
    last: Double,
    baseVolume: Double,
    bid: Double,
    ask: Double,
    openBuyOrders: Long,
    openSellOrders: Long,
    prevDay: Double)
