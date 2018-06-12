package cryptotool.cli.service

import cryptotool.domain.Exchange
import cryptotool.ext.binance.service.BinanceService
import cryptotool.ext.bittrex.service.BittrexService

import scala.concurrent.{ExecutionContext, Future}

class PricesCliService(
    bittrexService: BittrexService,
    binanceService: BinanceService)(implicit ec: ExecutionContext) {

  def prices(exchange: Exchange, tickers: Seq[String]): Future[Unit] = exchange match {
    case Exchange.Bittrex =>
      bittrexService.getMarketSummaries().map { marketSummaries =>
        tickers.foreach { ticker =>
          marketSummaries.find(_.marketName == ticker) match {
            case Some(summary) => printValue(ticker, summary.last)
            case None => printNotFound(ticker)
          }
        }
      }

    case Exchange.Binance =>
      binanceService.getAllTickersPrices().map { tickersPrices =>
        tickers.foreach { ticker =>
          tickersPrices.get(ticker) match {
            case Some(value) => printValue(ticker, value)
            case None => printNotFound(ticker)
          }
        }
      }
  }

  private def printValue(ticker: String, value: Double): Unit = {
    println(s"$ticker ${value.formatCrypto}")
  }

  private def printNotFound(ticker: String): Unit = {
    println(s"ticker $ticker not found")
  }

}
