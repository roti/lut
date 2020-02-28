package roti.lut

import roti.lut.annotation.record

/**
 * Test for when the trait also contains implemented method or values
 */
@record
trait Record2 extends Record {

  def id: Long
  def name: String

  def methodWithExistingImplementation = "I already have an implementation"

  val someValue = 100

}
