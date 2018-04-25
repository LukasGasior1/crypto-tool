package cryptotool.ext.etherscan.api

import java.net.URLEncoder

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import org.json4s.{DefaultFormats, native}
import org.json4s.native.JsonMethods

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object EtherscanApiClient {
  private case class Response[Result](status: String, message: Option[String], result: Option[Result])
}

class EtherscanApiClient(config: EtherscanApiClientConfig)(implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  import EtherscanApiClient._

  implicit val serialization = native.Serialization
  implicit val formats = DefaultFormats

  def getBalances(addresses: Seq[String]): Future[Seq[AccountBalance]] = {
    require(addresses.nonEmpty)
    require(addresses.size <= 20)

    get[Seq[AccountBalance]](
      module = "account",
      action = "balancemulti",
      params = Map("tag" -> "latest", "address" -> addresses.mkString(",")))
  }

  private def get[Result](module: String, action: String, params: Map[String, String])(implicit manifest: Manifest[Response[Result]]): Future[Result] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = s"$baseUri?apikey=${config.apiKeyToken}&module=$module&action=$action&${makeQueryString(params)}")

    Http().singleRequest(request).flatMap(handleResponse[Result])
  }

  private def makeQueryString(params: Map[String, String]): String = {
    params.foldLeft("") { case (accum, (k, v)) =>
      val prefix = if (accum.isEmpty) "" else s"$accum&"
      s"$prefix${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
  }

  private def handleResponse[Result](httpResponse: HttpResponse)(implicit manifest: Manifest[Response[Result]]): Future[Result] = {
    if (httpResponse.status.isSuccess())
      Unmarshal(httpResponse.entity).to[String]
        .flatMap { responseStr =>
          val responseTry = Try(JsonMethods.parse(responseStr).extract[Response[Result]])
          responseTry match {
            case Success(res) if res.status == "1" =>
              res.result.map(Future.successful).getOrElse(Future.failed(new RuntimeException("Request was a success but no result provided")))
            case Success(res) =>
              Future.failed(new RuntimeException("Response status is not 1: " + res.message.getOrElse("[no message provided]")))
            case Failure(ex) =>
              Future.failed(new RuntimeException("Cannot read response json: " + responseStr, ex))
          }
        }
    else Future.failed(new RuntimeException("Server did not return success status code"))
  }

  private val baseUri = "https://api.etherscan.io/api"
}
