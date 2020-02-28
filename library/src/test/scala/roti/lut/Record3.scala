package roti.lut

import roti.lut.annotation.record

/**
 * Test for when the companion object already exists.
 */
@record
trait Record3 extends Record {

  def id: Long
  def name: String

}


object Record3 {
  def apply(s: String) = s
}