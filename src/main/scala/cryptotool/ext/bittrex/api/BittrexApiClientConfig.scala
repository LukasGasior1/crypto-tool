package cryptotool.ext.bittrex.api

import com.typesafe.config.Config

case class BittrexApiClientConfig(apiKey: String, apiSecret: String)

object BittrexApiClientConfig {
  def apply(typesafeConfig: Config): BittrexApiClientConfig = {
    val bittrexConfig = typesafeConfig.getConfig("crypto-tool.ext.bittrex")
    BittrexApiClientConfig(
      apiKey = bittrexConfig.getString("api-key"),
      apiSecret = bittrexConfig.getString("api-secret"))
  }
}
