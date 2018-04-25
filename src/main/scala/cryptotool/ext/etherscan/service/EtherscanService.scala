package cryptotool.ext.etherscan.service

import cryptotool.ext.etherscan.api.EtherscanApiClient

import scala.concurrent.{ExecutionContext, Future}

class EtherscanService(etherscanApiClient: EtherscanApiClient)(implicit ec: ExecutionContext) {

  def getBalance(address: String): Future[Double] = {
    apiRequest(_.getBalances(Seq(address)).map(r => r.head.balance.toDouble / Math.pow(10, 18)))
  }

  def getBalances(addresses: Seq[String]): Future[Map[String, Double]] = {
    val maxAddressesPerRequest = 20
    val packets = addresses.sliding(maxAddressesPerRequest, maxAddressesPerRequest)
    val responsesF = Future.traverse(packets) { packet =>
      apiRequest(_.getBalances(packet))
    }.map(_.flatten)

    responsesF.map { balances =>
      balances.map { balance => balance.account -> balance.balance.toDouble / Math.pow(10, 18) }.toMap
    }
  }

  private def apiRequest[T](f: EtherscanApiClient => T): T = {
    // TODO: throttle (also in other services)
    f(etherscanApiClient)
  }

}
