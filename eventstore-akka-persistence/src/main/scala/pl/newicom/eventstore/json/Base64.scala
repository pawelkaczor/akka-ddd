package pl.newicom.eventstore.json

/**
 * Base64 encoder
 * @author Mark Lister
 * (c) Mark Lister 2014
 *
 * I, the copyright holder of this work, release this work into the public domain.
 * This applies worldwide. In some countries this may not be legally possible;
 * if so: I grant anyone the right to use this work for any purpose, without any
 * conditions, unless such conditions are required by law.
 *
 * The repo for this Base64 encoder lives at https://github.com/marklister/base64
 * Please send your issues, suggestions and pull requests there.
 *
 */
object Base64 {
  class B64Scheme(val encodeTable: IndexedSeq[Char]) {
    lazy val decodeTable = collection.immutable.TreeMap(encodeTable.zipWithIndex: _*)
  }
  lazy val base64 = new B64Scheme(('A' to 'Z') ++ ('a' to 'z') ++ ('0' to '9') ++ Seq('+', '/'))
  lazy val base64Url = new B64Scheme(base64.encodeTable.dropRight(2) ++ Seq('-', '_'))
  implicit class Encoder(b: Array[Byte]) {
    private[this] val zero = Array(0, 0).map(_.toByte)
    lazy val pad = (3 - b.length % 3) % 3
    def toBase64(implicit scheme: B64Scheme = base64): String = {
      def sixBits(x: Array[Byte]): Seq[Int] = {
        val a = (x(0) & 0xfc) >> 2
        val b = ((x(0) & 0x3) << 4) + ((x(1) & 0xf0) >> 4)
        val c = ((x(1) & 0xf) << 2) + ((x(2) & 0xc0) >> 6)
        val d = (x(2)) & 0x3f
        Seq(a, b, c, d)
      }
      ((b ++ zero.take(pad)).grouped(3)
        .flatMap(sixBits(_))
        .map(x => scheme.encodeTable(x))
        .toSeq
        .dropRight(pad) :+ "=" * pad)
        .mkString
    }
  }
  implicit class Decoder(s: String) {
    lazy val cleanS = s.reverse.dropWhile(_ == '=').reverse
    lazy val pad = s.length - cleanS.length
    def toByteArray(implicit scheme: B64Scheme = base64): Array[Byte] = {
      def threeBytes(s: Seq[Char]): Array[Byte] = {
        val r = s.map(scheme.decodeTable(_)).foldLeft(0)((a,b)=>(a << 6) +b)
        java.nio.ByteBuffer.allocate(8).putLong(r).array().takeRight(3)
      }
      if (pad > 2 || s.length % 4 != 0) throw new java.lang.IllegalArgumentException("Invalid Base64 String:" + s)
      if (!cleanS.forall(scheme.encodeTable.contains(_))) throw new java.lang.IllegalArgumentException("Invalid Base64 String:" + s)
      (cleanS + "A" * pad)
        .grouped(4)
        .map(threeBytes(_))
        .flatten
        .toArray
        .dropRight(pad)
    }
  }
}