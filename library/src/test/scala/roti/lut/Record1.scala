package roti.lut

import roti.lut.annotation.record

@record
trait Record1 extends Record {

  def byteF: Byte
  def shortF: Short
  def intF: Int
  def longF: Long
  def doubleF: Double
  def floatF: Float
  def bigDecimalF: BigDecimal

  def stringF: String

  def maybeIntF: Option[Int]
  def maybeStringF: Option[String]


}
