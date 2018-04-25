package cryptotool.ext.binance.service

import cryptotool.ext.binance.api
import org.json4s.DefaultFormats
import org.json4s.JsonAST.JValue

object ApiConversions {

  private implicit val formats = DefaultFormats

  def parseBalance(apiBalance: api.AccountBalanceItem): Balance =
    Balance(
      asset = apiBalance.asset,
      free = apiBalance.free.toDouble,
      locked = apiBalance.locked.toDouble)

  def parseOrderBook(apiOrderBook: api.OrderBookResponse): OrderBook =
    OrderBook(
      lastUpdateId = apiOrderBook.lastUpdateId,
      bids = apiOrderBook.bids.map(parseOrderBookBid),
      asks = apiOrderBook.asks.map(parseOrderBookAsk))

  def parseOrderBookBid(apiBid: Seq[JValue]): OrderBookBid =
    OrderBookBid(apiBid(0).extract[String].toDouble, apiBid(1).extract[String].toDouble)

  def parseOrderBookAsk(apiAsk: Seq[JValue]): OrderBookAsk =
    OrderBookAsk(apiAsk(0).extract[String].toDouble, apiAsk(1).extract[String].toDouble)

  def parseTrade(apiTrade: api.AccountTrade): Trade =
    Trade(
      id = apiTrade.id,
      orderId = apiTrade.orderId,
      price = apiTrade.price.toDouble,
      qty = apiTrade.qty.toDouble,
      commission = apiTrade.commission.toDouble,
      commissionAsset = apiTrade.commissionAsset,
      time = apiTrade.time,
      isBuyer = apiTrade.isBuyer,
      isMaker = apiTrade.isMaker,
      isBestMatch = apiTrade.isBestMatch)

  def parseRecentTradeList(apiRecentTrades: Seq[api.RecentTrade]): RecentTradeList = {
    RecentTradeList(apiRecentTrades.map(parseRecentTrade))
  }

  def parseRecentTrade(apiRecentTrade: api.RecentTrade): RecentTrade =
    RecentTrade(
      id = apiRecentTrade.id,
      price = apiRecentTrade.price.toDouble,
      qty = apiRecentTrade.qty.toDouble,
      time = apiRecentTrade.time,
      isBestMatch = apiRecentTrade.isBestMatch,
      isBuyerMaker = apiRecentTrade.isBuyerMaker)

  def parseTickerStats24H(apiTickerStats: api.TickerStats24H): TickerStats24H =
    TickerStats24H(
      symbol = apiTickerStats.symbol,
      priceChange = apiTickerStats.priceChange.toDouble,
      priceChangePercent = apiTickerStats.priceChangePercent.toDouble,
      weightedAvgPrice = apiTickerStats.weightedAvgPrice.toDouble,
      prevClosePrice = apiTickerStats.prevClosePrice.toDouble,
      lastPrice = apiTickerStats.lastPrice.toDouble,
      lastQty = apiTickerStats.lastQty.toDouble,
      bidPrice = apiTickerStats.bidPrice.toDouble,
      askPrice = apiTickerStats.askPrice.toDouble,
      openPrice = apiTickerStats.openPrice.toDouble,
      highPrice = apiTickerStats.highPrice.toDouble,
      lowPrice = apiTickerStats.lowPrice.toDouble,
      volume = apiTickerStats.volume.toDouble,
      quoteVolume = apiTickerStats.quoteVolume.toDouble,
      openTime = apiTickerStats.openTime,
      closeTime = apiTickerStats.closeTime,
      firstId = apiTickerStats.firstId,
      lastId = apiTickerStats.lastId,
      count = apiTickerStats.count)


}
