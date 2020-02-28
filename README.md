# Lut

Lut is an attempt to make data modelling easier in Scala. The design is inspired by Clojure's `defrecord`.

Scala's case classes are not a good fit for modelling data since they insist on strictness and are not composable. Once you model your data with case classes you end up with a lot of classes, a lot of conversions.

The idea is to have the data flow through the program as maps.
