package roti.lut

import scala.language.experimental.macros
import scala.reflect.macros.whitebox


/**
 * Macro for @record annotation
 */
class RecordMacroImpl(val c: whitebox.Context) {

  import c.universe._

  private lazy val debugEnabled = true

  def transformClass(annottees: c.Expr[Any]*): c.Expr[Record] = {

    try {
      val result = annottees.map(_.tree) match {
        case List(classDef: ClassDef) => transformClassImpl(classDef, None)
        case List(classDef: ClassDef, companionObjectDef: ModuleDef) => transformClassImpl(classDef, Some(companionObjectDef))
        case x => c.abort(c.enclosingPosition, s"@record can only be applied to classes or traits, not to $x")
      }

      if (debugEnabled) {
        println("transformTrait macro result\n" + result)
      }

      c.Expr(result)
    } catch {
      case e: RecordException => c.abort(c.enclosingPosition, e.getMessage)
    }
  }


  def transformClassImpl(classDef: ClassDef, companionObjectDef: Option[ModuleDef]) = {
    val ClassDef(mods, className, typeParams, impl) = classDef

    val abstractMethods = impl.body.map(m =>
      m match {
        case methodDef: DefDef if methodDef.rhs.isEmpty => Some(methodDef)
        case x => None
      }
    ).flatten

    val recordParent = impl.parents.find {
      case Ident(TypeName("Record")) => true
      case _ => false
    }

    if (!recordParent.isDefined) {
      c.abort(c.enclosingPosition, s"$className needs to extend Record")
    }


    val transformedBody = impl.body.map(m =>
      m match {
        case methodDef: DefDef if methodDef.rhs.isEmpty => transformMethod(methodDef)
        case x => x
      }
    ) :+ buildCopyMethod(className, abstractMethods)


    val transformedClass = ClassDef(mods, className, typeParams, Template(impl.parents, impl.self, transformedBody))


    val transformedCompanionObject = companionObjectDef match {
      case None =>
        q"object ${className.toTermName} { ${buildConstructor(className)}; ${buildConstructor2(className, abstractMethods)}; ${buildUnapplyMethod(className, abstractMethods)} }"
      case Some(ModuleDef(mods, name, Template(parents, self, body))) =>
        ModuleDef(mods, name, Template(parents, self, body :+ buildConstructor(className) :+ buildConstructor2(className, abstractMethods) :+ buildUnapplyMethod(className, abstractMethods)))
    }

    q"$transformedClass; $transformedCompanionObject"
  }


  private def transformMethod(methodDef: DefDef) = {
    val DefDef(modifiers, name, typeParams, argss, returnType, body) = methodDef

    if (!body.isEmpty) {
      //existing implementations are untouched
      methodDef
    } else {

      if (argss.size > 0 && argss(0).size > 0) {
        throw new RecordException("abstract methods must not take parameters")
      }

      if (typeParams.size > 0)
        throw new RecordException("type parameters are not allowed on abstract method")

      if (returnType.toString().startsWith("Option")) {
        q"""@roti.lut.annotation.field(${name.toString}) def $name = get(${name.toString}).asInstanceOf[$returnType]"""
        q"""@roti.lut.annotation.field(${name.toString}) def $name = get(${name.toString}).asInstanceOf[$returnType]"""
      } else {
        q"""@roti.lut.annotation.field(${name.toString}) def $name = apply(${name.toString}).asInstanceOf[$returnType]"""
      }

    }


  }


  private def buildCopyMethod(className: TypeName, abstractMethods: Seq[DefDef]) = {

    //only abstract methods are part of the copy method
    val argsWithValues = abstractMethods.map { m =>
      val DefDef(modifiers, name, typeParams, argss, returnType, body) = m
      //this needs to be a value definition because of the default parameter value
      (q"val $name: $returnType = $name", q"(${name.toString}, $name)")
    }

    val args = argsWithValues.map(_._1)
    val values = argsWithValues.map(_._2)

    q"def copy(..$args): $className = ${className.toTermName}(data ++ Seq(..$values))"
  }


  private def buildConstructor2(className: TypeName, abstractMethods: Seq[DefDef]) = {
    //only abstract methods are part of the copy method
    val argsWithValues = abstractMethods.map { m =>
      val DefDef(modifiers, name, typeParams, argss, returnType, body) = m
      //this needs to be a value definition because of the default parameter value
      (q"$name: $returnType", q"(${name.toString}, $name)")
    }

    val args = argsWithValues.map(_._1)
    val values = argsWithValues.map(_._2)

    q"def apply(..$args): $className = ${className.toTermName}(Map(..$values))"
  }


  private def buildConstructor(className: TypeName) = {
    q"""def apply(m: Map[String, Any]): $className = new $className {
          override val data = m
    }"""
  }


  private def buildUnapplyMethod(className: TypeName, abstractMethods: Seq[DefDef]) = {
    val argsWithValues = abstractMethods.map { m =>
      val DefDef(modifiers, name, typeParams, argss, returnType, body) = m
      //this needs to be a value definition because of the default parameter value
      (q"$returnType", q"x.$name")
    }

    val returnTypes = argsWithValues.map(_._1)
    val methodCalls = argsWithValues.map(_._2)

    q"def unapply(x: $className): Option[(..$returnTypes)] = Some((..$methodCalls))"
  }


}
