package cryptotool.ext.binance.api

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cryptotool.utils.CryptoUtils
import cryptotool.utils.HexUtils._
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, native}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class BinanceApiClient(config: BinanceApiClientConfig)
                      (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  import scala.collection.immutable.Seq

  private implicit val serialization = native.Serialization
  private implicit val formats = DefaultFormats

  def accountInfo(timestamp: Long = System.currentTimeMillis, recvWindow: Option[Long] = None): Future[AccountResponse] = {
    getSigned[AccountResponse](
      uri = "/account",
      params = stdParams(timestamp, recvWindow),
      version = "v3")
  }

  def orderBook(symbol: String, limit: Option[Int] = None): Future[OrderBookResponse] = {
    getUnsigned[OrderBookResponse](
      uri = "/depth",
      params = Map("symbol" -> symbol) ++ optionalParams("limit" -> limit),
      version = "v1")
  }

  def tickerPrice(symbol: Option[String]): Future[Either[TickerPrice, Seq[TickerPrice]]] = {
    symbol match {
      case Some(s) =>
        getUnsigned[TickerPrice](uri = "/ticker/price", params = Map("symbol" -> s), version = "v3").map(Left.apply)
      case None =>
        getUnsigned[Seq[TickerPrice]](uri = "/ticker/price", params = Map.empty, version = "v3").map(Right.apply)
    }
  }

  def tickerStats24H(symbol: Option[String]): Future[Either[TickerStats24H, Seq[TickerStats24H]]] = {
    symbol match {
      case Some(s) =>
        getUnsigned[TickerStats24H](uri = "/ticker/24hr", params = Map("symbol" -> s), version = "v1").map(Left.apply)
      case None =>
        getUnsigned[Seq[TickerStats24H]](uri = "/ticker/24hr", params = Map.empty, version = "v1").map(Right.apply)
    }
  }

  def recentTrades(symbol: String, limit: Option[Int] = None): Future[Seq[RecentTrade]] = {
    getUnsigned[Seq[RecentTrade]](
      uri = "/trades",
      params = Map("symbol" -> symbol) ++ optionalParams("limit" -> limit),
      version = "v1")
  }

  def accountTradeList(symbol: String, limit: Option[Int] = None, fromId: Option[Long] = None,
                       timestamp: Long = System.currentTimeMillis, recvWindow: Option[Long] = None)
  : Future[Seq[AccountTrade]] = {
    getSigned[Seq[AccountTrade]](
      uri = "/myTrades",
      params = Map("symbol" -> symbol) ++ optionalParams("limit" -> limit, "fromId" -> fromId) ++ stdParams(timestamp, recvWindow),
      version = "v3")
  }

  private def getSigned[Result](uri: String, params: Map[String, String], version: String)
                          (implicit mf: Manifest[Result]): Future[Result] = {
    val queryString = makeQueryString(params)

    val signature = CryptoUtils.generateHMAC(
      data = queryString.getBytes,
      secretKey = config.secretKey.getBytes,
      hashFn = "HmacSHA256").toHexString

    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = s"${baseUri(version)}$uri?$queryString&signature=$signature",
      headers = Seq(RawHeader("X-MBX-APIKEY", config.apiKey)))

    Http().singleRequest(request).flatMap(handleResponse[Result])
  }

  private def getUnsigned[Result](uri: String, params: Map[String, String], version: String)(implicit mf: Manifest[Result]): Future[Result] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = s"${baseUri(version)}$uri?${makeQueryString(params)}")

    Http().singleRequest(request).flatMap(handleResponse[Result])
  }

  private def handleResponse[Result](httpResponse: HttpResponse)(implicit mf: Manifest[Result]): Future[Result] = {
    if (httpResponse.status.isSuccess()) {
      Unmarshal(httpResponse.entity).to[String].flatMap { responseEntityStr =>
        Try(JsonMethods.parse(responseEntityStr).extract[Result]) match {
          case Success(obj) => Future.successful(obj)
          case Failure(ex) => Future.failed(new RuntimeException(s"Cannot parse json from response: $responseEntityStr", ex))
        }
      }
    } else if (httpResponse.status == StatusCodes.TooManyRequests) {
      Future.failed(new RuntimeException("Too many requests!"))
    } else {
      Unmarshal(httpResponse.entity).to[String].flatMap { str =>
        Future.failed(new RuntimeException(s"Server did not return success status code: $str"))
      }
    }
  }

  private def makeQueryString(params: Map[String, String]): String = {
    params.foldLeft("") { case (accum, (k, v)) =>
      val prefix = if (accum.isEmpty) "" else s"$accum&"
      s"$prefix${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
  }

  private def optionalParams(params: (String, Option[Any])*): Map[String, String] = {
    params.foldLeft(Map.empty[String, String]) { case (accum, (k, vOpt)) =>
      accum ++ optionalParam(k, vOpt)
    }
  }

  private def optionalParam(name: String, optParam: Option[Any]): Map[String, String] = {
    optParam.map(v => Map(name -> v.toString)).getOrElse(Map.empty)
  }

  private def stdParams(timestamp: Long, recvWindow: Option[Long]): Map[String, String] = {
    Map("timestamp" -> timestamp.toString) ++ optionalParam("recvWindow", recvWindow)
  }

  private def baseUri(version: String) = s"https://api.binance.com/api/$version"

}
