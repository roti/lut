package roti.lut

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class RecordSpec extends AnyFlatSpec with Matchers {


  "@record" should "provide implementations for abstract methods" in {

    val data = Map("byteF" -> 100.toByte, "shortF" -> 200.toShort, "intF" -> 300, "longF" -> 400L, "doubleF" -> 4.9d, "floatF" -> 7.5f, "bigDecimalF" -> BigDecimal(9.8), "stringF" -> "teststr")
    val record1 = Record1(data)

    record1.byteF should be (100.toByte)
    record1.shortF should be (200.toShort)
    record1.intF should be (300)
    record1.longF should be (400L)
    record1.doubleF should be (4.9d)
    record1.floatF should be (7.5f)
    record1.bigDecimalF should be (BigDecimal(9.8))
    record1.stringF should be ("teststr")
  }

  "@record" should "not touch methods which already have implementations" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val record = Record2(data)

    record.id should be (100L)
    record.name should be ("teststr")
    record.methodWithExistingImplementation should be ("I already have an implementation")

  }


  "missing data" should "throw NoSuchElementException" in {
    val data = Map.empty[String, Any]

    val record1 = Record1(data)

    a [NoSuchElementException] should be thrownBy {record1.byteF}
    a [NoSuchElementException] should be thrownBy {record1.shortF}
    a [NoSuchElementException] should be thrownBy {record1.intF}
    a [NoSuchElementException] should be thrownBy {record1.longF}
    a [NoSuchElementException] should be thrownBy {record1.doubleF}
    a [NoSuchElementException] should be thrownBy {record1.floatF}
    a [NoSuchElementException] should be thrownBy {record1.bigDecimalF}
    a [NoSuchElementException] should be thrownBy {record1.stringF}
  }


  "Option types" should "work as expected" in {
    val data = Map("maybeIntF" -> 100)

    val record1 = Record1(data)

    record1.maybeIntF should be (Some(100))
    record1.maybeStringF should be (None)

  }


  "Record.to" should "do recursive conversion" in {

    val record2Data1 = Map("id" -> 10, "name" -> "foo")
    val record2Data2 = Map("id" -> 11, "name" -> "bar")

    val record4data = Map("name" -> "baz", "record2" -> record2Data1, "optRecord2" -> record2Data2)

    val record4 = Record.to[Record4](record4data)

    record4.record2.data should be (record2Data1)
    record4.optRecord2.get.data should be (record2Data2)

  }


  "A Record" should "have a copy method" in {
    val data = Map("id" -> 100, "name" -> "testusr")
    val expectedData = Map("id" -> 301, "name" -> "testusr")
    val record2 = Record2(data)

    record2.copy(id = 301).data should be (expectedData)
  }

  "The copy method" should "preserve all data from the map" in {
    val data = Map("id" -> 100, "name" -> "testusr", "anotherHiddenValue" -> 998)
    val expectedData = Map("id" -> 301, "name" -> "testusr", "anotherHiddenValue" -> 998)
    val record2 = Record2(data)

    record2.copy(id = 301).data should be (expectedData)
  }

  "A Record" should "have a Map interface" in {

    val data = Map("byteF" -> 100.toByte, "shortF" -> 200.toShort, "intF" -> 300, "longF" -> 400L, "doubleF" -> 4.9d, "floatF" -> 7.5f, "bigDecimalF" -> BigDecimal(9.8), "stringF" -> "teststr")
    val record1 = Record1(data)

    record1.size should be (8)

    (record1 + ("foo" -> "bar")).size should be (9)

  }


  "The generated apply method" should "be added to the companion object when it exists" in {
    val record = Record3(Map.empty[String, Any])

    Record3("the other apply method") should be ("the other apply method")
  }


  "A Record" should "have a case class-like constructor" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val record = Record2(id = 100L, name = "teststr")

    record.data should be (data)

  }

}
