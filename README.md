# Lut

Lut is an attempt to make data modelling easier in Scala. The design is inspired by Clojure's `defrecord`.

Scala's case classes are problematic for modelling data since they insist on strictness and compose poorly. They lead to a lot of boilerplate code and conversions between classes. 

### Dependency

```scala
libraryDependencies += "com.github.roti" %% "lut" % "0.5-SNAPSHOT"
```

Currently only scala 2.13 is supported.

### Usage

Build the data class as a trait extending `Record` and annotate it with `@record`.

```scala
import roti.lut.annotation.record
import roti.lut.Record

@record
trait Employee extends Record {
  def id: Long
  def firstName: String
  def lastName: String
  def phoneNumber: Option[String]
}
```

The annotation transforms the trait with a macro. It provides implementations for the methods, adds a `copy` method and an `apply` method in the companion object. Now `Employee` can be used as if it were a fully implemented class:

```scala
val data: Map[String, Any] = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith")
val employee = Employee(data)
println(employee.firstName + " " + employee.lastName + " " + employee.phoneNumber )  //"John Smith None"

val employee2 = employee.copy(phoneNumber = Some("123"))
println(employee.firstName + " " + employee.lastName + " " + employee.phoneNumber )  //"John Smith Some(123)"
```

The data is stored as a `Map[String, Any]`, the generated implementations of the abstract methods just access the values from this map. The map is available through `.data`. Other values which are not exposed by the traits methods are left untouched when modified versions are created with `copy`: 

```scala
val data: Map[String, Any] = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith", "foo" -> "bar")
val employee = Employee(data)
println(employee.data.get("foo") )  //Some("bar")

val employee2 = employee.copy(phoneNumber = Some("123"))
println(employee2.data.get("foo") )  //Some("bar")
```

No conversions are done when getting values from the map, so the map is expected to have the correct types. If that's not the case a runtime exception will occur. This is true also when the field is another record.

```scala
import roti.lut.annotation.record
import roti.lut.Record

@record
trait Employee extends Record {
  def id: Long
  def firstName: String
  def lastName: String
  def phoneNumber: Option[String]
  def department: Department
}

@record
trait Department extends Record {
  def name: String
}



val data = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith", "department" -> Map("name" -> "sales"))
println(Employee(data).department)  //will throw an exception, because a Department is expected, but a Map is found
```

For this case you can use the helper `Record.to` which will recursively convert maps to `Record` instances where needed:

```scala
val employee = Record.to[Employee](data)
println(Employee(data).department)  //now it works, Map("name" -> "sales") was converted to an instance of Department
```


