package cryptotool.profitcalc

sealed trait TradeType
object TradeType {
  case object Buy extends TradeType
  case object Sell extends TradeType
}

object Trade {
  implicit val ordering: Ordering[Trade] = Ordering.by[Trade, Long](_.timestamp)
}

case class Trade(
    id: String,
    price: Double,
    quantity: Double,
    commission: Double, // must be in base currency (BTC). does not affect quantity
    timestamp: Long,
    tradeType: TradeType)

sealed trait ProfitComponent {
  def value: Double
}
object ProfitComponent {
  case class RealizedProfit(override val value: Double, referenceTradeId: Option[String]) extends ProfitComponent
  case class UnrealizedProfit(override val value: Double) extends ProfitComponent
  case class Commission(commission: Double) extends ProfitComponent {
    override val value: Double = -commission
  }
}

case class TradeProfit(trade: Trade, profitComponents: Seq[ProfitComponent]) {
  import ProfitComponent._

  lazy val realizedProfitGross: Double = profitComponents.collect { case RealizedProfit(value, _) => value }.sum
  lazy val totalCommission: Double = profitComponents.collect { case Commission(commission) => commission }.sum
  lazy val unrealizedProfit: Double = profitComponents.collect { case UnrealizedProfit(value) => value }.sum
}

case class TradeProfitsResult(tradeProfits: Seq[TradeProfit]) {
  lazy val realizedProfitGross: Double = tradeProfits.filter(_.trade.tradeType == TradeType.Buy).map(_.realizedProfitGross).sum
  lazy val unrealizedProfit: Double = tradeProfits.filter(_.trade.tradeType == TradeType.Buy).map(_.unrealizedProfit).sum

  lazy val totalCommissions: Double = tradeProfits.map(_.totalCommission).sum
  lazy val realizedProfitNet: Double = realizedProfitGross - totalCommissions
  lazy val totalProfit: Double = realizedProfitNet + unrealizedProfit
}
