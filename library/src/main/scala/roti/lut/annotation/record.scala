package roti.lut.annotation

import roti.lut.{Record, RecordMacroImpl}

import scala.annotation.{StaticAnnotation, compileTimeOnly}
import scala.language.experimental.macros

@compileTimeOnly("enable macro paradise to expand macro annotations")
class record extends StaticAnnotation {
  def macroTransform(annottees: Any*): Record = macro RecordMacroImpl.transformClass
}


