package cryptotool.cli

import java.text.DecimalFormat

package object service {

  private val CryptoDecimals = 5

  implicit class DoubleWithFormat(double: Double) {
    private val cryptoFormat = new DecimalFormat("0." + ("0" * CryptoDecimals))
    private val fiatFormat = new DecimalFormat("###,##0.00")

    def formatCrypto = cryptoFormat.format(double)
    def formatFiat = fiatFormat.format(double)
    def formatUsd = "$" + fiatFormat.format(double)
  }

}
