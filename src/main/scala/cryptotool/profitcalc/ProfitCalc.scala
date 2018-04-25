package cryptotool.profitcalc

import scala.annotation.tailrec

class ProfitCalc(trades: Seq[Trade], currentPrice: Double)(implicit ord: Ordering[Trade]) {

  import ProfitComponent._

  private val weightedAverageCostByTradeId = calculateWeightedAverageCosts(trades)

  private val (buyTrades, sellTrades) = trades.partition(_.tradeType == TradeType.Buy)

  def calculateProfit(): TradeProfitsResult = {
    val tradeProfits =
      (sellTrades.map(calculateSellTradeProfit) ++ calculateBuyTradeProfits())
        .sortBy(_.trade)

    TradeProfitsResult(tradeProfits)
  }

  private def calculateBuyTradeProfits(): Seq[TradeProfit] = {
    val (_, tradeProfits) = buyTrades.foldLeft((sellTrades, Seq.empty[TradeProfit])) {
      case ((accumSellTrades, accumResult), trade) =>
        val (matchingSellTrades, updatedSellTrades) = findMatchingSellTrades(accumSellTrades, trade)
        val tradeProfit = calculateBuyTradeProfit(trade, matchingSellTrades)
        (updatedSellTrades, accumResult :+ tradeProfit)
    }
    tradeProfits
  }

  private def calculateBuyTradeProfit(trade: Trade, matchingSellTrades: Seq[(Double, Double, String)]): TradeProfit = {
    val realizedProfits = matchingSellTrades.map { case (qty, price, id) =>
      val buyPrice = qty * trade.price
      val sellPrice = qty * price
      val profit = sellPrice - buyPrice
      RealizedProfit(profit, Some(id))
    }

    val realizedQty = matchingSellTrades.map(_._1).sum
    val unrealizedQty = trade.quantity - realizedQty
    val unrealizedProfit = if (unrealizedQty > 0) {
      val unrealizedBuyPrice = unrealizedQty * trade.price
      val unrealizedCurrentValue = unrealizedQty * currentPrice
      unrealizedCurrentValue - unrealizedBuyPrice
    } else 0.0

    TradeProfit(trade, realizedProfits :+ UnrealizedProfit(unrealizedProfit) :+ Commission(trade.commission))
  }

  /** Finds sell trades that match given buy trade. Returns a list of (quantity, price, trade id) triplets. Also returns a list of sell trades with updated remaining quantities. */
  private def findMatchingSellTrades(initialSellTrades: Seq[Trade], trade: Trade)
  : (Seq[(Double, Double, String)], Seq[Trade]) = {

    @tailrec
    def recur(remainingSellTrades: Seq[Trade], remainingQty: Double, accumResult: Seq[(Double, Double, String)] = Nil)
    : (Seq[(Double, Double, String)], Seq[Trade]) = remainingSellTrades match {
      case head :: tail if head.timestamp > trade.timestamp =>
        if (head.quantity >= remainingQty) {
          // this sell trade is sufficient to match whole remaining quantity
          val qtyRemainingInTrade = head.quantity - remainingQty
          val updatedTrades =
            if (qtyRemainingInTrade == 0) tail
            else head.copy(quantity = qtyRemainingInTrade) :: tail
          (accumResult :+ (remainingQty, head.price, head.id), updatedTrades)
        } else {
          // this sell trade is not sufficient, we need to continue
          val updatedRemainingQty = remainingQty - head.quantity
          recur(tail, updatedRemainingQty, accumResult :+ (head.quantity, head.price, head.id))
        }

      case head :: tail if head.timestamp <= trade.timestamp =>
        recur(tail, remainingQty, accumResult)

      case _ =>
        (accumResult, Nil)
    }

    recur(initialSellTrades, trade.quantity)
  }

  // TODO: use similar mechanism to buy profit (find matching buy trade), in current form it won't work if tx list is not a full history
  private def calculateSellTradeProfit(trade: Trade): TradeProfit = {
    val weightedAverageCost = weightedAverageCostByTradeId(trade.id)
    if (weightedAverageCost == 0) {
      TradeProfit(trade, Seq(Commission(trade.commission)))
    } else {
      val buyCost = weightedAverageCost * trade.quantity
      val sellCost = trade.price * trade.quantity
      val profit = sellCost - buyCost
      TradeProfit(trade, Seq(
        RealizedProfit(profit, None),
        Commission(trade.commission)))
    }
  }

  /** Returns weighted average cost after each trade by trade id */
  private def calculateWeightedAverageCosts(trades: Seq[Trade]): Map[String, Double] = {
    val (_, _, weightedAverageCostByTradeId) =
      trades.foldLeft((0.0, 0.0, Map.empty[String, Double])) { case ((accumCost, accumQty, accumResult), trade) =>
        trade.tradeType match {
          case TradeType.Buy =>
            val updatedQty = accumQty + trade.quantity
            val updatedCost = ((accumCost * accumQty) + (trade.price * trade.quantity)) / updatedQty
            (updatedCost, updatedQty, accumResult + (trade.id -> updatedCost))
          case TradeType.Sell =>
            val updatedQty = accumQty - trade.quantity
            (accumCost, updatedQty, accumResult + (trade.id -> accumCost))
        }
      }
    weightedAverageCostByTradeId
  }

}
