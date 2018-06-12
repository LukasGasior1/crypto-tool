package cryptotool.cli

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import cryptotool.ext.binance.api.{BinanceApiClient, BinanceApiClientConfig}
import cryptotool.ext.binance.service.BinanceService
import cryptotool.ext.bittrex.api.{BittrexApiClient, BittrexApiClientConfig}
import cryptotool.ext.bittrex.service.BittrexService
import cryptotool.cli.service.{PricesCliService, ProfitCalcCliService, SummaryCliService}
import cryptotool.ext.etcchain.api.{EtcchainApiClient, EtcchainApiClientConfig}
import cryptotool.ext.etcchain.service.EtcchainService
import cryptotool.ext.etherscan.api.{EtherscanApiClient, EtherscanApiClientConfig}
import cryptotool.ext.etherscan.service.EtherscanService

import scala.collection.JavaConverters._

object CliApp extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system = ActorSystem("crypto-tool-cli_system")
  implicit val materializer = ActorMaterializer()

  private val config = ConfigFactory.load()

  private val options = OptionParser(args).getOrElse(sys.exit(1))

  val bittrexApiClient = new BittrexApiClient(BittrexApiClientConfig(config))
  val bittrexService = new BittrexService(bittrexApiClient)

  val binanceApiClient = new BinanceApiClient(BinanceApiClientConfig(config))
  val binanceService = new BinanceService(binanceApiClient)

  val etherscanApiClient = new EtherscanApiClient(EtherscanApiClientConfig(config))
  val etherscanService = new EtherscanService(etherscanApiClient)

  val etcchainApiClient = new EtcchainApiClient(EtcchainApiClientConfig(config))
  val etcchainService = new EtcchainService(etcchainApiClient)

  val summaryCliService = new SummaryCliService(bittrexService, binanceService, etherscanService, etcchainService)
  val profitCalcCliService = new ProfitCalcCliService(binanceService)
  val pricesCliService = new PricesCliService(bittrexService, binanceService)

  options.command match {
    case balanceCmd: BalanceCmd => handleBalanceCommand(balanceCmd)
    case profitCmd: ProfitCmd => handleProfitCommand(profitCmd)
    case pricesCmd: PricesCmd => handlePricesCommand(pricesCmd)
    case NoopCmd => println("Use either balance or profit command")
  }

  private def handleBalanceCommand(balanceCmd: BalanceCmd): Unit = {
    val resF = summaryCliService.summary(
      exchanges = balanceCmd.exchanges,
      ethAddresses = balanceCmd.ethAddresses.getOrElse(config.getStringList("crypto-tool.eth-addresses").asScala),
      etcAddresses = balanceCmd.etcAddresses.getOrElse(config.getStringList("crypto-tool.etc-addresses").asScala))

    resF.foreach(_ => shutdown())
    resF.failed.foreach { ex =>
      println(ex)
      shutdown()
    }
  }

  private def handleProfitCommand(profitCmd: ProfitCmd): Unit = {
    val resF = profitCalcCliService.profitCalc(
      exchange = profitCmd.exchange,
      currencies = profitCmd.currencies)

    resF.foreach(_ => shutdown())
    resF.failed.foreach { ex =>
      println(ex)
      shutdown()
    }
  }

  private def handlePricesCommand(pricesCmd: PricesCmd): Unit = {
    val resF = pricesCliService.prices(
      exchange = pricesCmd.exchange,
      tickers = pricesCmd.tickers)

    resF.foreach(_ => shutdown())
    resF.failed.foreach { ex =>
      println(ex)
      shutdown()
    }
  }

  private def shutdown(): Unit = {
    Http().shutdownAllConnectionPools().andThen { case _ =>
      materializer.shutdown()
      system.terminate()
    }
  }

}
