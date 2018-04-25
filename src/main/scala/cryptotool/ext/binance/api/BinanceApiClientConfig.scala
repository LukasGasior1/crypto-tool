package cryptotool.ext.binance.api

import com.typesafe.config.Config

case class BinanceApiClientConfig(apiKey: String, secretKey: String)

object BinanceApiClientConfig {
  def apply(typesafeConfig: Config): BinanceApiClientConfig = {
    val binanceConfig = typesafeConfig.getConfig("crypto-tool.ext.binance")
    BinanceApiClientConfig(
      apiKey = binanceConfig.getString("api-key"),
      secretKey = binanceConfig.getString("secret-key"))
  }
}
