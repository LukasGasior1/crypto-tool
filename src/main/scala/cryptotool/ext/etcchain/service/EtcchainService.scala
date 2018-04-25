package cryptotool.ext.etcchain.service

import cryptotool.ext.etcchain.api.EtcchainApiClient

import scala.concurrent.{ExecutionContext, Future}

class EtcchainService(etcchainApiClient: EtcchainApiClient)(implicit ec: ExecutionContext) {

  def getBalance(address: String): Future[Double] = {
    apiRequest(_.getBalance(address).map(r => r.balance))
  }

  def getBalances(addresses: Seq[String]): Future[Map[String, Double]] = {
    val resultsF = Future.traverse(addresses)(a => apiRequest(_.getBalance(a)))
    resultsF.map { results =>
      results.map { res =>
        res.address -> res.balance
      }.toMap
    }
  }

  private def apiRequest[T](f: EtcchainApiClient => T): T = {
    // TODO: throttle
    f(etcchainApiClient)
  }

}
