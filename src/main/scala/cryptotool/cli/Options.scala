package cryptotool.cli

import cryptotool.domain.Exchange

sealed trait Command

case object NoopCmd extends Command

case class BalanceCmd(exchanges: Set[Exchange], ethAddresses: Option[Seq[String]], etcAddresses: Option[Seq[String]]) extends Command

case class ProfitCmd(exchange: Exchange, currencies: Seq[String]) extends Command

case class PricesCmd(exchange: Exchange, tickers: Seq[String]) extends Command

case class Options(command: Command = NoopCmd)
