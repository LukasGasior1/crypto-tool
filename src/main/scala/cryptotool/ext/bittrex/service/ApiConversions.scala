package cryptotool.ext.bittrex.service

import cryptotool.ext.bittrex.api

object ApiConversions {

  def parseMarketSummary(apiMarketSummary: api.MarketSummary): MarketSummary =
    MarketSummary(
      marketName = apiMarketSummary.marketName,
      high = apiMarketSummary.high,
      low = apiMarketSummary.low,
      volume = apiMarketSummary.volume,
      last = apiMarketSummary.last,
      baseVolume = apiMarketSummary.baseVolume,
      bid = apiMarketSummary.bid,
      ask = apiMarketSummary.ask,
      openBuyOrders = apiMarketSummary.openBuyOrders,
      openSellOrders = apiMarketSummary.openSellOrders,
      prevDay = apiMarketSummary.prevDay)

}
