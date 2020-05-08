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

  it should "not touch methods which already have implementations" in {
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


  "A Record" should "have a update methods for each field" in {
    val data = Map("id" -> 100, "name" -> "testusr")
    val expectedData = Map("id" -> 301, "name" -> "testusr")
    val expectedData2 = Map("id" -> 301, "name" -> "foo")
    val record2 = Record2(data)

    record2.id(301.toLong).data should be (expectedData)
    record2.id(301.toLong).name("foo") should be (expectedData2)
  }

  it should "expose the underlying map through .data" in {

    val data = Map("byteF" -> 100.toByte, "shortF" -> 200.toShort, "intF" -> 300, "longF" -> 400L, "doubleF" -> 4.9d, "floatF" -> 7.5f, "bigDecimalF" -> BigDecimal(9.8), "stringF" -> "teststr")
    val record1 = Record1(data)

    record1.data should be (data)

  }


  it should "have a case class-like constructor" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val record = Record2(id = 100L, name = "teststr")

    record.data should be (data)

  }


  it should "be have an extractor object" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val record = Record2(data)

    val Record2(id, name) = record
    id should be (100L)
    name should be ("teststr")
  }


  "The update methods" should "preserve all data from the map" in {
    val data = Map("id" -> 100, "name" -> "testusr", "anotherHiddenValue" -> 998)
    val expectedData = Map("id" -> 301, "name" -> "testusr", "anotherHiddenValue" -> 998)
    val record2 = Record2(data)

    record2.id(301).data should be (expectedData)
  }



  "The generated apply method" should "be added to the companion object when it exists" in {
    val record = Record3(Map.empty[String, Any])

    Record3("the other apply method") should be ("the other apply method")
  }

  "The second generated apply method" should "not add None values to the underlying map" in {
    val record = Record1(byteF = 1, shortF = 2, intF = 3, longF = 4, doubleF = 5.6d, floatF = 7.8f, bigDecimalF = BigDecimal(9.10), stringF = "11.12", maybeIntF = None, maybeStringF = None)
    val data = Map("byteF" -> 1, "shortF" -> 2, "intF" -> 3, "longF" -> 4, "doubleF" -> 5.6d, "floatF" -> 7.8f, "bigDecimalF" -> BigDecimal(9.10), "stringF" -> "11.12")

    record.data.size should be (8)
    record.data should be (data)
  }

  it should "unbox Some values when adding to the underlying map" in {
    val record = Record1(byteF = 1, shortF = 2, intF = 3, longF = 4, doubleF = 5.6d, floatF = 7.8f, bigDecimalF = BigDecimal(9.10), stringF = "11.12", maybeIntF = Some(13), maybeStringF = Some("14.15"))
    val data = Map("byteF" -> 1, "shortF" -> 2, "intF" -> 3, "longF" -> 4, "doubleF" -> 5.6d, "floatF" -> 7.8f, "bigDecimalF" -> BigDecimal(9.10), "stringF" -> "11.12", "maybeIntF" -> 13, "maybeStringF" -> "14.15")

    record.data.size should be (10)
    record.data should be (data)
  }

  "A Record" should "be comparable for equality to other records and maps" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val data2 = Map("id" -> 100L, "name" -> "teststr")
    val record = Record2(data)
    val record2 = Record2(data2)

    record.canEqual(record2) should be (true)
    record.canEqual(data2) should be (true)
    record.canEqual(Seq.empty) should be (false)

  }

  "Record equality" should "be based on the underlying data" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val data2 = Map("id" -> 100L, "name" -> "teststr")
    val data3 = Map("id" -> 100L, "name" -> "teststr", "foo" -> "bar")
    val record = Record2(data)
    val record2 = Record2(data2)
    val record3 = Record2(data3)

    (record == record2) should be (true)
    (record.equals(record2)) should be (true)

    (record == record3) should be (false)
    (record.equals(record3)) should be (false)

  }

  it should "work with maps as well" in {
    val data = Map("id" -> 100L, "name" -> "teststr")
    val data2 = Map("id" -> 100L, "name" -> "teststr")
    val data3 = Map("id" -> 100L, "name" -> "teststr", "foo" -> "bar")
    val record = Record2(data)


    (record == data2) should be (true)
    (record.equals(data2)) should be (true)

    (record == data3) should be (false)
    (record.equals(data3)) should be (false)

  }


  "Records extending other records" should "have inherited fields in the field constructor as well" in {
    val record6 = Record6(id = 100, name = "foo", description5 = "bar", description6 = "baz")
    record6.data  should be (Map("id" -> 100, "name" -> "foo", "description5" -> "bar", "description6" -> "baz"))
  }

  it should "reuse implementations from inherited methods" in {

    //compilation fails if this test fails
    val record7 = Record7(id = 100, name = "foo", description5 = "bar", description6 = "baz", foo = "bla")
    record7.data  should be (Map("id" -> 100, "name" -> "foo", "description5" -> "bar", "description6" -> "baz", "foo" -> "bla"))
  }




}
