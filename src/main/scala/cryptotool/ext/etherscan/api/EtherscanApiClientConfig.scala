package cryptotool.ext.etherscan.api

import com.typesafe.config.Config

case class EtherscanApiClientConfig(apiKeyToken: String)

object EtherscanApiClientConfig {
  def apply(typesafeConfig: Config): EtherscanApiClientConfig = {
    val etherscanConfig = typesafeConfig.getConfig("crypto-tool.ext.etherscan")
    EtherscanApiClientConfig(
      apiKeyToken = etherscanConfig.getString("api-key-token"))
  }
}
