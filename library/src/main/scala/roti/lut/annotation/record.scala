package roti.lut.annotation

import roti.lut.{Record, RecordMacroImpl}

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros

@compileTimeOnly("@record requires annotation macros to be enabled")
class record extends StaticAnnotation {
  def macroTransform(annottees: Any*): Record = macro RecordMacroImpl.transformClass
}


