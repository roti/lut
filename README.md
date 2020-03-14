# Lut

Lut is an attempt to make data modelling easier in Scala. The design is inspired by Clojure's `defrecord`.

Scala's case classes are problematic for modelling data since they insist on strictness and compose poorly. They lead to a lot of boilerplate code and conversions between classes. 

### Dependency

```scala
libraryDependencies += "com.github.roti" %% "lut" % "0.5-SNAPSHOT"
```

### Scala Versions

Supported scala versions are 2.11, 2.12 and 2.13.

For scala 2.13 you need to enable macro annotations:
```scala
scalacOptions += "-Ymacro-annotations"
```

For scala 2.12 and earlier you need to add the macro paradise compiler plugin to your project:
```scala
addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.1")
```

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

The annotation transforms the trait with a macro so that it can be used like a case class. It provides:
* implementations for the abstract methods 
* a `copy` method, like the one available in case classes
* an `apply` method in the companion object, like the one available in case classes
* an additional `apply` method, to create instances from a `Map[String, Any]`
* an `unapply` method for deconstructing instances into the individual fields 

Now `Employee` can be used as if it were a fully implemented class:

```scala
val data: Map[String, Any] = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith")
val employee = Employee(data)
println(employee.firstName + " " + employee.lastName + " " + employee.phoneNumber )  //"John Smith None"

val employee2 = employee.copy(phoneNumber = Some("123"))
println(employee.firstName + " " + employee.lastName + " " + employee.phoneNumber )  //"John Smith Some(123)"

val employee3 = Employee(id = 100, firstName = "John", lastName = "Smith", phoneNumber = None)

val Employee(id, fName, lName, _) = employee3
println(id + " " + fName + " " + lName)  //100 John Smith
```

The data is stored as a `Map[String, Any]`, the generated implementations of the abstract methods just access the values from this map. 
The map is available through `.data`. Other values which are not exposed by the traits methods are left untouched when modified versions are created with `copy`: 

```scala
val data: Map[String, Any] = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith", "foo" -> "bar")
val employee = Employee(data)
println(employee.data.get("foo") )  //Some("bar")

val employee2 = employee.copy(phoneNumber = Some("123"))
println(employee2.data.get("foo") )  //Some("bar")
```

No conversions are done when getting values from the map, so the map is expected to have the correct types. 
If that's not the case a runtime exception will occur. This is true also when the field is another record.

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


### Partial information

Since the underlying data is stored as a `Map`, and the `apply` method which creates instances from maps does not do any checks, it is possible to have instances with incomplete data. 
This makes it possible to use the same class in situations where the data is gradually built, in multiple steps, by simply passing the instance around, and creating modified version via `copy` (of course, as long you don't try to retrieve data which does not exist).
For example in a CRUD context, you can use the same trait for insert and update operations, where the only difference is that there is no id when doing insert. 
The insert operation will receive an incomplete instance, where the id is missing (and will not try to retrieve the id), and will return a complete instance with the generated id.

We basically give up some type safety to avoid creating a case class for every partial representation of the data that is needed in our workflow. 


### Equality

Since a record is meant to be just a convenient interface over a `Map`, equality is based on the underlying data. 
Two record instances are equal if and only if the underlying maps are equal. 
This means that even if all fields are equal, two records will not be equal if the underlying maps are not equal.
In other words, data which is not exposed through the record's interface, participates in equality checks.

Furthermore records can be compared to plain maps and the semantic is the same: if the underlying map is equal to the map then the record is equals to the map. 