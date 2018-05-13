package cryptotool.ext.etcchain.api

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

class EtcchainApiClient(config: EtcchainApiClientConfig)
                       (implicit system: ActorSystem, materializer: Materializer, ec: ExecutionContext) {

  implicit val serialization = native.Serialization
  implicit val formats = DefaultFormats

  def getBalance(address: String): Future[BalanceResponse] = {
    get[BalanceResponse]("getAddressBalance", Map("address" -> address))
  }

  private def get[Result](path: String, params: Map[String, String])(implicit manifest: Manifest[Result]): Future[Result] = {
    val request = HttpRequest(
      method = HttpMethods.GET,
      uri = s"$baseUri/$path?${makeQueryString(params)}")

    Http().singleRequest(request).flatMap(handleResponse[Result])
  }

  private def makeQueryString(params: Map[String, String]): String = {
    params.foldLeft("") { case (accum, (k, v)) =>
      val prefix = if (accum.isEmpty) "" else s"$accum&"
      s"$prefix${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
    }
  }

  private def handleResponse[Result](httpResponse: HttpResponse)(implicit manifest: Manifest[Result]): Future[Result] = {
    if (httpResponse.status.isSuccess())
      Unmarshal(httpResponse.entity).to[String]
        .flatMap { responseStr =>
          val responseTry = Try(JsonMethods.parse(responseStr).camelizeKeys.extract[Result])
          responseTry match {
            case Success(res) => Future.successful(res)
            case Failure(ex) => Future.failed(new RuntimeException("Cannot read response json: " + responseStr, ex))
          }
        }
    else Future.failed(new RuntimeException("Server did not return success status code"))
  }

  private val baseUri =
    if (config.useHttps) "https://etcchain.com/api/v1"
    else "http://etcchain.com/api/v1"

}
