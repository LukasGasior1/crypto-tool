package cryptotool.ext.etcchain.api

import com.typesafe.config.Config

case class EtcchainApiClientConfig(useHttps: Boolean)

object EtcchainApiClientConfig {
  def apply(typesafeConfig: Config): EtcchainApiClientConfig = {
    val etcchainConfig = typesafeConfig.getConfig("crypto-tool.ext.etcchain")
    EtcchainApiClientConfig(
      useHttps = etcchainConfig.getBoolean("use-https"))
  }
}
