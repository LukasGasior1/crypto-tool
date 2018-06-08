package cryptotool.cli.service

import cryptotool.domain.Exchange
import cryptotool.ext.binance
import cryptotool.ext.binance.service.BinanceService
import cryptotool.ext.bittrex
import cryptotool.ext.bittrex.service.BittrexService
import cryptotool.ext.etcchain.service.EtcchainService
import cryptotool.ext.etherscan.service.EtherscanService
import de.vandermeer.asciitable.{AT_Row, AsciiTable}
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment

import scala.concurrent.{ExecutionContext, Future}

object SummaryCliService {
  val TableWidth = 100
}

class SummaryCliService(
    bittrexService: BittrexService,
    binanceService: BinanceService,
    etherscanService: EtherscanService,
    etcchainService: EtcchainService)(implicit ec: ExecutionContext) {

  import SummaryCliService._

  def summary(exchanges: Set[Exchange], ethAddresses: Seq[String], etcAddresses: Seq[String]): Future[Unit] = {
    val bittrexSummaryOptF =
      if (exchanges.contains(Exchange.Bittrex)) bittrexService.getSummary().map(Some.apply)
      else Future.successful(None)

    val binanceSummaryOptF =
      if (exchanges.contains(Exchange.Binance)) binanceService.getSummary().map(Some.apply)
      else Future.successful(None)

    val ethAccountsBalancesF = etherscanService.getBalances(ethAddresses).recover {
      case ex =>
        println("Unable to get balances of ETH accounts from etherscan.io")
        println(ex.getMessage)
        Map.empty[String, Double]
    }

    val etcAccountsBalancesF = etcchainService.getBalances(etcAddresses).recover {
      case ex =>
        println("Unable to get balances of ETC accounts from etcchain.com")
        println(ex.getMessage)
        Map.empty[String, Double]
    }

    val marketSummariesF = bittrexService.getMarketSummaries()

    for {
      marketSummaries <- marketSummariesF
      bittrexSummaryOpt <- bittrexSummaryOptF
      binanceSummaryOpt <- binanceSummaryOptF
      ethAccountsBalances <- ethAccountsBalancesF
      etcAccountsBalances <- etcAccountsBalancesF
    } yield {
      val btcPriceInUsd = marketSummaries.find(_.marketName == "USDT-BTC").map(_.last).getOrElse(0.0)
      val ethPriceInBtc = marketSummaries.find(_.marketName == "BTC-ETH").map(_.last).getOrElse(0.0)
      val etcPriceInBtc = marketSummaries.find(_.marketName == "BTC-ETC").map(_.last).getOrElse(0.0)
      val ethPriceInUsd = marketSummaries.find(_.marketName == "USDT-ETH").map(_.last).getOrElse(0.0)
      val etcPriceInUsd = marketSummaries.find(_.marketName == "USDT-ETC").map(_.last).getOrElse(0.0)

      val table = new AsciiTable
      table.addRule()

      alignRow(table.addRow("Currency", "Balance", "Price in BTC", "BTC value", "USD value"))
      table.addRule()

      bittrexSummaryOpt.foreach { summary =>
        addBittrexRows(table, summary)
        table.addRule()
      }

      binanceSummaryOpt.foreach { summary =>
        addBinanceRows(table, summary)
        table.addRule()
      }

      if (ethAccountsBalances.nonEmpty) {
        addCurrencyRows(table, "ETH", ethAccountsBalances, ethPriceInBtc, ethPriceInUsd)
        table.addRule()
      }

      if (etcAccountsBalances.nonEmpty) {
        addCurrencyRows(table, "ETC", etcAccountsBalances, etcPriceInBtc, etcPriceInUsd)
        table.addRule()
      }

      addTotalRow(
        table = table,
        bittrexCurrencies = bittrexSummaryOpt.map(_.currencies).getOrElse(Nil),
        binanceCurrencies = binanceSummaryOpt.map(_.currencies).getOrElse(Nil),
        ethAccountsBalances = ethAccountsBalances,
        ethPriceInBtc = ethPriceInBtc,
        ethPriceInUsd = ethPriceInUsd,
        etcAccountsBalances = etcAccountsBalances,
        etcPriceInBtc = etcPriceInBtc,
        etcPriceInUsd = etcPriceInUsd)

      table.addRule()

      println(table.render(TableWidth))
    }
  }

  private def addBittrexRows(table: AsciiTable, summary: bittrex.service.Summary): Unit = {
    val totalBtc = summary.currencies.map(_.estimatedBtcValue).sum
    val totalUsd = summary.currencies.map(_.estimatedUsdValue).sum

    alignRow(table.addRow("Bittrex", "", "", totalBtc.formatCrypto, totalUsd.formatUsd))

    summary.currencies.sortBy(-_.estimatedBtcValue).foreach { s =>
      val row = table.addRow(
        s"- ${s.currency}",
        s.balance.formatCrypto,
        s.priceInBtc.formatCrypto,
        s.estimatedBtcValue.formatCrypto,
        s.estimatedUsdValue.formatUsd)
      alignRow(row)
      row.getCells.get(0).getContext.setPaddingLeft(2)
    }
  }

  private def addBinanceRows(table: AsciiTable, summary: binance.service.Summary): Unit = {
    val totalBtc = summary.currencies.map(_.estimatedBtcValue).sum
    val totalUsd = summary.currencies.map(_.estimatedUsdValue).sum

    alignRow(table.addRow("Binance", "", "", totalBtc.formatCrypto, totalUsd.formatUsd))

    summary.currencies.sortBy(-_.estimatedBtcValue).foreach { s =>
      val row = table.addRow(
        s"- ${s.balance.asset}",
        s.balance.free.formatCrypto,
        s.priceInBtc.formatCrypto,
        s.estimatedBtcValue.formatCrypto,
        s.estimatedUsdValue.formatUsd)
      alignRow(row)
      row.getCells.get(0).getContext.setPaddingLeft(2)
    }
  }

  private def addCurrencyRows(table: AsciiTable, currency: String, balances: Map[String, Double], priceInBtc: Double, priceInUsd: Double): Unit = {
    val total = balances.values.sum
    val totalBtc = total * priceInBtc
    val totalUsd = total * priceInUsd

    alignRow(table.addRow(currency, total.formatCrypto, priceInBtc.formatCrypto, totalBtc.formatCrypto, totalUsd.formatUsd))

    balances.toSeq.sortBy(-_._2).foreach { case (account, balance) =>
      val row = table.addRow(
        s"- ${account.take(6)}..${account.takeRight(4)}",
        balance.formatCrypto,
        priceInBtc.formatCrypto,
        (balance * priceInBtc).formatCrypto,
        (balance * priceInUsd).formatUsd)
      alignRow(row)
      row.getCells.get(0).getContext.setPaddingLeft(2)
    }
  }

  private def addTotalRow(
    table: AsciiTable,
    bittrexCurrencies: Seq[bittrex.service.CurrencySummary],
    binanceCurrencies: Seq[binance.service.CurrencySummary],
    ethAccountsBalances: Map[String, Double],
    ethPriceInBtc: Double,
    ethPriceInUsd: Double,
    etcAccountsBalances: Map[String, Double],
    etcPriceInBtc: Double,
    etcPriceInUsd: Double): Unit = {

    val totalBtc =
      bittrexCurrencies.map(_.estimatedBtcValue).sum +
      binanceCurrencies.map(_.estimatedBtcValue).sum +
      ethAccountsBalances.values.sum * ethPriceInBtc +
      etcAccountsBalances.values.sum * etcPriceInBtc

    val totalUsd =
      bittrexCurrencies.map(_.estimatedUsdValue).sum +
      binanceCurrencies.map(_.estimatedUsdValue).sum +
      ethAccountsBalances.values.sum * ethPriceInUsd +
      etcAccountsBalances.values.sum * etcPriceInUsd

    alignRow(table.addRow("Total", "", "", totalBtc.formatCrypto, totalUsd.formatUsd))
  }

  private def alignRow(row: AT_Row): Unit = {
    row.getCells.get(0).getContext.setTextAlignment(TextAlignment.LEFT)
    (1 until row.getCells.size).foreach { i => row.getCells.get(i).getContext.setTextAlignment(TextAlignment.RIGHT) }
  }

}
