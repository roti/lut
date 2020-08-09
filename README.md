# Lut

Lut is an attempt to make data modelling easier in Scala. The design is inspired by Clojure's `defrecord`.

Scala's case classes are problematic for modelling data. They don't compose and include optionality, leading to a proliferation of classes and a lot of conversions between them.

Lut strives to offer an alternative, which:
* works with immutable values
* allows composition by inheritance
* allows partial data

The basic idea is to keep data in maps and access it in a (relatively) typesafe manner through interfaces. 
This way it looks and behaves like a normal class, with statically declared data members, while the implementation is actually a dynamic immutable map.

### Dependency

```scala
libraryDependencies += "com.github.roti" %% "lut" % "0.5"
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

Build the data class as a trait extending `Record` and annotate it with `@record`. We'll call it a record from now on.

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

The `@record` annotation transforms the trait with a macro providing implementations for the abstract methods, two `apply` methods in the companion object and update methods.

Now `Employee` can be used as if it were a fully implemented class:

```scala
val data: Map[String, Any] = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith")
val employee = Employee(data)
println(employee.firstName + " " + employee.lastName + " " + employee.phoneNumber )  //"John Smith None"

//Each abstract method has an update method, with the same name, but accepting a parameter.
//The update method returns a new modified instance of the record.
val employee2 = employee.phoneNumber(Some("123"))
println(employee.firstName + " " + employee.lastName + " " + employee.phoneNumber )  //"John Smith Some(123)"

//instances can be created either from a Map[String, Any] or from individual field values  
val employee3 = Employee(id = 100, firstName = "John", lastName = "Smith", phoneNumber = None)

//destructuring works
val Employee(id, fName, lName, _) = employee3
println(id + " " + fName + " " + lName)  //100 John Smith
```

The data is stored as a `Map[String, Any]`, the generated implementations of the abstract methods just access the values from this map. 
The map itself is available through `.data`. Other values which are not exposed by the traits methods are left untouched when modified versions are created: 

```scala
val data: Map[String, Any] = Map("id" -> 100, "firstName" -> "John", "lastName" -> "Smith", "foo" -> "bar")
val employee = Employee(data)
println(employee.data.get("foo") )  //Some("bar")

val employee2 = employee.phoneNumber(Some("123"))
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
println(employee.department)  //now it works, Map("name" -> "sales") was converted to an instance of Department
```


### Partial information

Since the underlying data is stored as a `Map`, and the `apply` method which creates instances from maps does not do any checks, it is possible to have instances with incomplete data. 
This makes it possible to use the same class in situations where the data is gradually built, in multiple steps, by simply passing the instance around and creating modified versions (of course, as long you don't try to retrieve data which does not exist).
For example in a CRUD context, you can use the same trait for insert and update operations, where the only difference is that there is no id when doing insert. 
The insert operation will receive an incomplete instance, where the id is missing (and will not try to retrieve the id), and will return a complete instance with the generated id.


### Equality

Since a record is meant to be just a convenient interface over a `Map`, equality is based on the underlying data. 
Two record instances are equal if and only if the underlying maps are equal. 
In other words, data which is not exposed through the record's interface, participates in the equality check.

Furthermore records can be compared to plain maps and the semantic is the same: if the underlying map is equal to the map then the record is equal to the map.


### Optionality

The usual approach for optionality in maps is not to include values in the map when they are missing (as opposed to including a representation of the missing value, like `null` or `None`).
Lut follows the same rule: when the type of a field is `Option` and the value is `None`, the underlying map does not contain any value for that field. 
If the value is `Some(x)`, then the underlying map contains the value `x` for that field.

```scala
@record
trait Foo extends Record {
  def bar: Option[Int]
  def baz: Option[String]
}

val foo1 = Foo(bar = Some(10), baz = None)
println(foo1.data)  //Map(bar -> 10)
```

In other words, the underlying map should never have values which are instances of `Option`, but rather the value should be present in the map or not.
**You need to be aware of this rule when building the map yourself.**


### Inheritance

Since we work with traits, we can make use of inheritance:

```scala
@record
trait Audit extends Record {
  def lastUpdatedAt: Long
  def lastUpdatedBy: String
}

//no need to extend Record as well
@record
trait Employee extends Audit {
  def id: Long
  def name: String
}

//inherited fields need to have a value as well
val employee = Employee(id = 100, name = "John Smith", lastUpdatedAt = 1587812929, lastUpdatedBy = "admin")
```


### Other members

`@record` can be used on both traits and abstract classes, and there are no restrictions on how the class or trait should look like. 
It can have vals and normal methods, but they are not taken into consideration (which means they are not considered to be fields of the record).
Of course, they can't have the name of one of the generated methods.



