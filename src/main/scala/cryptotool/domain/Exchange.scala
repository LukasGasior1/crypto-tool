package cryptotool.domain

sealed trait Exchange
object Exchange {
  case object Bittrex extends Exchange
  case object Binance extends Exchange
}
