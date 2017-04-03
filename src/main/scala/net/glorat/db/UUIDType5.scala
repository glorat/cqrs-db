package net.glorat.db

import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.{Objects, UUID}

object UUIDType5 {

  val NAMESPACE_DNS: UUID =
    UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8")
  val NAMESPACE_URL: UUID =
    UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8")
  val NAMESPACE_OID: UUID =
    UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8")
  val NAMESPACE_X500: UUID =
    UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8")
  private val UTF8: Charset = Charset.forName("UTF-8")

  def nameUUIDFromNamespaceAndString(namespace: UUID, name: String): UUID =
    nameUUIDFromNamespaceAndBytes(
      namespace,
      Objects.requireNonNull(name, "name == null").getBytes(UTF8))

  def nameUUIDFromNamespaceAndBytes(namespace: UUID, name: Array[Byte]): UUID = {
    var md: MessageDigest = null
    md = MessageDigest.getInstance("SHA-1")
    md.update(toBytes(Objects.requireNonNull(namespace, "namespace is null")))
    md.update(Objects.requireNonNull(name, "name is null"))
    val sha1Bytes: Array[Byte] = md.digest()

    sha1Bytes(6) = (sha1Bytes(6) & 0x0f).toByte
    /* clear version        */

    sha1Bytes(6) =  (sha1Bytes(6) | 0x50).toByte
    /* set to version 5     */

    sha1Bytes(8) =  (sha1Bytes(8) & 0x3f).toByte
    /* clear variant        */

    sha1Bytes(8) = (sha1Bytes(8) | 0x80).toByte
    /* set to IETF variant  */

    fromBytes(sha1Bytes)
  }

  private def fromBytes(data: Array[Byte]): UUID = {
    // Based on the private UUID(bytes[]) constructor
    var msb: Long = 0
    var lsb: Long = 0
    assert(data.length >= 16)
    for (i <- 0.until(8)) msb = (msb << 8) | (data(i) & 0xff)
    for (i <- 8.until(16)) lsb = (lsb << 8) | (data(i) & 0xff)
    new UUID(msb, lsb)
  }

  private def toBytes(uuid: UUID): Array[Byte] = {
    // inverted logic of fromBytes()
    val out: Array[Byte] = Array.ofDim[Byte](16)
    val msb: Long = uuid.getMostSignificantBits
    val lsb: Long = uuid.getLeastSignificantBits
    for (i <- 0.until(8)) out(i) = ((msb >> ((7 - i) * 8)) & 0xff).toByte
    for (i <- 8.until(16)) out(i) = ((lsb >> ((15 - i) * 8)) & 0xff).toByte
    out
  }

}
