package cryptotool.cli.service

import cryptotool.domain.Exchange
import cryptotool.ext.binance.service.{BinanceService, Trade}
import cryptotool.{domain, profitcalc}
import cryptotool.profitcalc.{ProfitCalc, TradeProfitsResult, TradeType}
import de.vandermeer.asciitable.{AT_Row, AsciiTable}
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

import scala.concurrent.{ExecutionContext, Future}

object ProfitCalcCliService {
  val TableWidth = 100
}

class ProfitCalcCliService(binanceService: BinanceService)(implicit ec: ExecutionContext) {

  import ProfitCalcCliService._

  def profitCalc(exchange: Exchange, currencies: Seq[String]): Future[Unit] = {
    require(exchange == Exchange.Binance, "only binance supported")

    val resultsF = Future.traverse(currencies) { currency =>
      val resF = binanceService.getFullTradeList(currency) zip binanceService.getTickerPrice(s"${currency}BTC")
      resF.map { case (tr, price) => (currency, tr, price) }
    }.map { tradeListsWithPrices =>
      tradeListsWithPrices.map { case (currency, tradeList, price) =>
        val profitCalc = new ProfitCalc(tradeList.trades.map(toCalcTrade), price)
        (currency, profitCalc.calculateProfit())
      }
    }
    for {
      results <- resultsF
      btcPriceInUsd <- binanceService.getTickerPrice("BTCUSDT")
    } yield printResults(results, btcPriceInUsd)
  }

  private def printResults(results: Seq[(String, TradeProfitsResult)], btcPriceInUsd: Double): Unit = {
    val table = new AsciiTable
    table.addRule()

    alignRow(table.addRow("Currency", "Gross profit", "Commissions", "Net profit", "Unrealized profit", "Unrealized profit (USD)"))
    table.addRule()

    results.toArray.foreach { case (currency, r) =>
      alignRow(table.addRow(
        currency,
        r.realizedProfitGross.formatCrypto,
        r.totalCommissions.formatCrypto,
        r.realizedProfitNet.formatCrypto,
        r.unrealizedProfit.formatCrypto,
        (r.unrealizedProfit * btcPriceInUsd).formatUsd))

      table.addRule()
    }
    println(table.render(TableWidth))
  }

  private def toCalcTrade(trade: Trade): profitcalc.Trade = {
    if (trade.isBuyer) {
      require(trade.commissionAsset != domain.BTC)
      profitcalc.Trade(
        id = trade.id.toString,
        price = trade.price,
        quantity = trade.netQty,
        commission = trade.commission * trade.price,
        timestamp = trade.time,
        tradeType = TradeType.Buy)
    } else {
      require(trade.commissionAsset == domain.BTC)
      profitcalc.Trade(
        id = trade.id.toString,
        price = trade.price,
        quantity = trade.netQty,
        commission = trade.commission,
        timestamp = trade.time,
        tradeType = TradeType.Sell)
    }
  }

  private def alignRow(row: AT_Row): Unit = {
    row.getCells.get(0).getContext.setTextAlignment(TextAlignment.LEFT)
    (1 until row.getCells.size).foreach { i => row.getCells.get(i).getContext.setTextAlignment(TextAlignment.RIGHT) }
  }

}
