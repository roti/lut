package roti.lut

import roti.lut.annotation.record

/**
 * Tests for records which extend other records.
 */
@record
trait Record5 extends Record {

  def id: Long
  def name: String
  def description5: String

}


@record
trait Record6 extends Record5 {

  def description6: String

}
@record
trait Record7 extends Record6 {
  //id exists in parent records as well
  def id: Long
  def foo: String
}