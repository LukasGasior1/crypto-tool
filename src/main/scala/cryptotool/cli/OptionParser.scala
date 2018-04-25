package cryptotool.cli

import cryptotool.domain.Exchange

object OptionParser {

  def apply(args: Array[String]): Option[Options] = {
    val parser = new scopt.OptionParser[Options]("crypto-tool") {
      head("crypto-tool", "0.1")

      cmd("balance").
        action( (_, c) => c.copy(command = BalanceCmd(Set.empty, None, None)) )
        .text("show balances")
        .children(
          opt[Seq[String]]("exchanges")
            .action {
              case (exchanges, Options(balanceCmd: BalanceCmd)) =>
                Options(balanceCmd.copy(exchanges = exchanges.map {
                  case "bittrex" => Exchange.Bittrex
                  case "binance" => Exchange.Binance
                  case other => throw new RuntimeException(s"Unsupported exchange: $other")
                }.toSet))
              case _ => throw new RuntimeException("Balance command expected")
            }
            .validate { rawExchanges =>
              val validExchanges = Seq("bittrex", "binance")
              if (rawExchanges.forall(validExchanges.contains)) success
              else failure(s"exchanges: ${rawExchanges.filterNot(validExchanges.contains)} not supported")
            }
            .text(s"supported exchanges: bittrex, binance"),

          opt[Seq[String]]("eth-addresses")
            .action {
              case (ethAddresses, Options(balanceCmd: BalanceCmd)) => Options(balanceCmd.copy(ethAddresses = Some(ethAddresses)))
              case _ => throw new RuntimeException("Balance command expected")
            }
            .text("Ethereum addresses"),

          opt[Seq[String]]("etc-addresses")
            .action {
              case (etcAddresses, Options(balanceCmd: BalanceCmd)) => Options(balanceCmd.copy(etcAddresses = Some(etcAddresses)))
              case _ => throw new RuntimeException("Balance command expected")
            }
            .text("Ethereum Classic addresses")
        )

      cmd("profit").
        action( (_, o) => o.copy(command = ProfitCmd(Exchange.Binance, Nil)) )
        .text("show trade profits")
        .children(
          opt[String]("exchange").required()
            .action {
              case (exchange, Options(profitCmd: ProfitCmd)) =>
                Options(profitCmd.copy(exchange = exchange match {
                  case "bittrex" => Exchange.Bittrex
                  case "binance" => Exchange.Binance
                  case other => throw new RuntimeException(s"Unsupported exchange: $other")
                }))
              case _ => throw new RuntimeException("Profit command expected")
            }
            .validate { rawExchange =>
              val validExchanges = Seq("bittrex", "binance")
              if (validExchanges.contains(rawExchange)) success
              else failure(s"exchanges: $rawExchange not supported")
            }
            .text(s"supported exchanges: bittrex, binance"),

          opt[Seq[String]]("currencies").required()
            .action {
              case (currencies, Options(profitCmd: ProfitCmd)) => Options(profitCmd.copy(currencies = currencies))
              case _ => throw new RuntimeException("Balance command expected")
            }
            .text(s"currencies to calculate profit for")
        )
      help("help")
        .text("prints this usage text")
    }

    parser.parse(args, Options(command = NoopCmd))
  }

}
