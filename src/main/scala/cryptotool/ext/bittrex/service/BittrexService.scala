package cryptotool.ext.bittrex.service

import cryptotool.ext.bittrex.api.BittrexApiClient
import cryptotool.domain.BTC

import scala.concurrent.{ExecutionContext, Future}

class BittrexService(bittrexApiClient: BittrexApiClient)(implicit ec: ExecutionContext) {

  import ApiConversions._

  def getSummary(): Future[Summary] = {
    (for {
      marketSummaries <- getMarketSummaries()
      balances <- bittrexApiClient.getBalances()
    } yield {
      val btcUsdMarket = marketSummaries.find(_.marketName == "USDT-BTC")
      val btcPriceInUsd = btcUsdMarket.map(_.last).getOrElse(0.0)

      balances.filter(_.available > 0).map { balance =>
        val marketSummaryOpt = marketSummaries.find(_.marketName == s"BTC-${balance.currency}")
        val priceInBtc =
          if (balance.currency == BTC) 1.0
          else marketSummaryOpt.map(_.last).getOrElse(0.0)
        val estimatedBtcValue = priceInBtc * balance.available

        CurrencySummary(
          currency = balance.currency,
          balance = balance.available,
          priceInBtc = priceInBtc,
          estimatedBtcValue = estimatedBtcValue,
          estimatedUsdValue = estimatedBtcValue * btcPriceInUsd)
      }
    }).map(Summary.apply)
  }

  def getMarketSummaries(): Future[Seq[MarketSummary]] = {
    bittrexApiClient.getMarketSummaries().map(_.map(parseMarketSummary))
  }

}
