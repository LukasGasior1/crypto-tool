package cryptotool.ext.bittrex.api

import java.util.Date

case class BalanceItem(
    currency: String,
    balance: Double,
    available: Double,
    pending: Double,
    cryptoAddress: Option[String],
    requested: Option[Boolean],
    uuid: Option[String])

case class OrderHistoryItem(
    orderUuid : String,
    exchange : String,
    timeStamp : Date,
    orderType : String,
    limit : Double,
    quantity : Double,
    quantityRemaining : Double,
    commission : Double,
    price : Double,
    pricePerUnit : Option[Double],
    osConditional : Boolean,
    condition : Option[String],
    conditionTarget : Option[String],
    ommediateOrCancel : Boolean)

case class MarketSummary(
    marketName: String,
    high: Double,
    low: Double,
    volume: Double,
    last: Double,
    baseVolume: Double,
    timeStamp: String,
    bid: Double,
    ask: Double,
    openBuyOrders: Long,
    openSellOrders: Long,
    prevDay: Double,
    created: String,
    displayMarketName: Option[String])
