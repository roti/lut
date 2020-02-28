package roti.lut

import roti.lut.annotation.record


/**
 * Test for sub entities.
 */
@record
trait Record4 extends Record {

  def name: String
  def record2: Record2
  def optRecord2: Option[Record2]

}


