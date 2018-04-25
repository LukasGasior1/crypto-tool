package cryptotool.utils

object HexUtils {

  implicit class ByteArrayWithUtils(arr: Array[Byte]) {
    def toHexString: String = bytesToHexString(arr)
  }

  def bytesToHexString(bytes: Array[Byte]): String = {
    bytes.map("%02x".format(_)).mkString
  }

}
