package roti.lut

import roti.lut.annotation.record

/**
 * Tests for records which references itself.
 * There are no test cases for this record, it's enough if it compiles.
 */
@record
trait RecursiveRecord extends Record {

  def otherRecord: RecursiveRecord
  def otherOptRecord: Option[RecursiveRecord]
}
