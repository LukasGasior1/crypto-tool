package cryptotool.ext.binance.service

import cryptotool.ext.binance.api.BinanceApiClient
import cryptotool.domain

import scala.concurrent.{ExecutionContext, Future}

class BinanceService(binanceApiClient: BinanceApiClient)(implicit ec: ExecutionContext) {

  import ApiConversions._

  def getSummary(): Future[Summary] = {
    for {
      btcPriceInUsd <- getTickerPrice("BTCUSDT")
      balances <- getBalances()
      nonZeroBalances = balances.filter(_.free > 0)
      btcUsdStats <- getTickerStats24H("BTCUSDT")
      tickersStats <- Future.traverse(nonZeroBalances) { balance =>
        if (balance.asset == domain.BTC) Future.successful(btcUsdStats)
        else getTickerStats24H(s"${balance.asset}BTC") }
    } yield {
      val currencies = (nonZeroBalances zip tickersStats).map { case (balance, stats) =>
        val priceInBtc =
          if (balance.asset == domain.BTC) 1.0
          else stats.lastPrice

        val estimatedBtcValue = balance.free * priceInBtc
        val estimatedUsdValue = estimatedBtcValue * btcPriceInUsd

        val percentagePriceChange24HBtc =
          if (balance.asset == domain.BTC) 0.0
          else stats.priceChangePercent

        val percentagePriceChange24HUsd =
          if (balance.asset == domain.BTC) stats.priceChangePercent
          else stats.priceChangePercent + btcUsdStats.priceChangePercent

        CurrencySummary(balance = balance,
          priceInBtc = priceInBtc,
          estimatedBtcValue = estimatedBtcValue,
          estimatedUsdValue = estimatedUsdValue,
          percentageChange24HBtc = percentagePriceChange24HBtc,
          percentageChange24HUsd = percentagePriceChange24HUsd)
      }
      Summary(currencies)
    }
  }

  def getBalances(): Future[Seq[Balance]] = {
    binanceApiClient.accountInfo().map(_.balances.map(parseBalance))
  }

  def getOrderBook(symbol: String, limit: Option[Int]): Future[OrderBook] = {
    binanceApiClient.orderBook(symbol, limit).map(parseOrderBook)
  }

  def getRecentTrades(symbol: String, limit: Option[Int]): Future[RecentTradeList] = {
    binanceApiClient.recentTrades(symbol, limit).map(parseRecentTradeList)
  }

  def getTickerStats24H(symbol: String): Future[TickerStats24H] = {
    binanceApiClient.tickerStats24H(Some(symbol)).map {
      case Left(tickerStats24H) => parseTickerStats24H(tickerStats24H)
      case Right(_) => throw new RuntimeException("Expected single ticker price response from api")
    }
  }

  def getTickerPrice(symbol: String): Future[Double] = {
    binanceApiClient.tickerPrice(Some(symbol)).map {
      case Left(tickerPrice) => tickerPrice.price.toDouble
      case Right(_) => throw new RuntimeException("Expected single ticker price response from api")
    }
  }

  def getAllTickersPrices(): Future[Map[String, Double]] = {
    binanceApiClient.tickerPrice(None).map {
      case Right(tickerPrices) => tickerPrices.map(tp => tp.symbol -> tp.price.toDouble).toMap
      case Left(_) => throw new RuntimeException("Expected an array of ticker prices response from api")
    }
  }

  def getFullTradeList(assetSymbol: String): Future[TradeList] = {
    val limit = 500

    def recur(accumTrades: Seq[Trade] = Nil, fromId: Long = 1): Future[Seq[Trade]] = {
      binanceApiClient.accountTradeList(s"${assetSymbol}BTC", fromId = Some(fromId), recvWindow = Some(10000), limit = Some(limit)).flatMap { apiTrades =>
        val trades = apiTrades.map(parseTrade)
        val newAccumTrades = accumTrades ++ trades
        if (trades.length < limit) Future.successful(newAccumTrades)
        else recur(accumTrades = newAccumTrades, fromId = trades.map(_.id).max + 1)
      }
    }

    recur().map(TradeList.apply)
  }

}
