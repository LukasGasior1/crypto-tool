package cryptotool.ext.bittrex.api

import java.net.URLEncoder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import cryptotool.utils.HexUtils._
import org.json4s.native.JsonMethods
import org.json4s.{DefaultFormats, native}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object BittrexApiClient {
  case class Response[Result](success: Boolean, message: Option[String], result: Option[Result])

  val ApiVersion = "v1.1"
}

class BittrexApiClient(config: BittrexApiClientConfig)(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  import BittrexApiClient._

  implicit val serialization = native.Serialization
  implicit val formats = DefaultFormats

  def getBalance(currency: String): Future[BalanceItem] = {
    sendRequest[BalanceItem]("account/getbalance", Map("currency" -> currency))
  }

  def getOrderHistory(): Future[Seq[OrderHistoryItem]] = {
    sendRequest[Seq[OrderHistoryItem]]("account/getorderhistory", Map("market" -> "BTC-ADA"))
  }

  def getBalances(): Future[Seq[BalanceItem]] = {
    sendRequest[Seq[BalanceItem]]("account/getbalances", Map.empty)
  }

  def getMarketSummaries(): Future[Seq[MarketSummary]] = {
    sendPublicRequest[Seq[MarketSummary]]("public/getmarketsummaries", Map.empty)
  }

  private def sendRequest[Result](method: String, params: Map[String, String])(implicit manifest: Manifest[Response[Result]]): Future[Result] = {
    val paramsEncoded = params.foldLeft("") { case (accum, (k, v)) =>
      s"$accum&${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }

    val nonce = System.currentTimeMillis()
    val uri = s"$baseUri/$method?apikey=${config.apiKey}&nonce=$nonce$paramsEncoded"
    val apisign = sign(uri).toHexString
    val request = HttpRequest(method = HttpMethods.POST, uri = uri, headers = scala.collection.immutable.Seq(RawHeader("apisign", apisign)))

    Http().singleRequest(request).flatMap(handleResponse[Result])
  }

  private def sendPublicRequest[Result](method: String, params: Map[String, String])(implicit manifest: Manifest[Response[Result]]): Future[Result] = {
    val paramsEncoded = params.foldLeft("") { case (accum, (k, v)) =>
      s"$accum&${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }

    val uri = s"$baseUri/$method?$paramsEncoded"
    val request = HttpRequest(method = HttpMethods.GET, uri = uri)

    Http().singleRequest(request).flatMap(handleResponse[Result])
  }

  private def handleResponse[Result](httpResponse: HttpResponse)(implicit manifest: Manifest[Response[Result]]): Future[Result] = {
    if (httpResponse.status.isSuccess())
      Unmarshal(httpResponse.entity).to[String]
        .flatMap { responseStr =>
          val responseTry = Try(JsonMethods.parse(responseStr).camelizeKeys.extract[Response[Result]])
          responseTry match {
            case Success(res) if res.success =>
              res.result.map(Future.successful).getOrElse(Future.failed(new RuntimeException("Request was a success but no result provided")))
            case Success(res) =>
              Future.failed(new RuntimeException("Response failed: " + res.message.getOrElse("[no message provided]")))
            case Failure(ex) =>
              Future.failed(new RuntimeException("Cannot read response json: " + responseStr, ex))
          }
        }
    else Future.failed(new RuntimeException("Server did not return success status code"))
  }

  private def sign(uri: String): Array[Byte] = {
    val mac = Mac.getInstance("HmacSHA512")
    mac.init(new SecretKeySpec(config.apiSecret.getBytes, "HmacSHA512"))
    mac.doFinal(uri.getBytes)
  }

  private def baseUri: String = s"https://bittrex.com/api/$ApiVersion"
}
