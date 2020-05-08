package roti.lut

import roti.lut.annotation.fields

import scala.collection.mutable
import scala.language.experimental.macros
import scala.reflect.macros.whitebox

/**
 * Macro for @record annotation
 */
class RecordMacroImpl(val c: whitebox.Context) {

  import c.universe._

  private lazy val debugEnabled = false

  def transformClass(annottees: c.Expr[Any]*): c.Expr[Record] = {

    try {
      val result = annottees.map(_.tree) match {
        case List(classDef: ClassDef) => transformClassImpl(classDef, None)
        case List(classDef: ClassDef, companionObjectDef: ModuleDef) => transformClassImpl(classDef, Some(companionObjectDef))
        case x => c.abort(c.enclosingPosition, s"@record can only be applied to classes or traits, not to $x")
      }

      if (debugEnabled) {
        println("@record macro result\n" + result)
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

    val recordTpe = c.typeOf[Record]

    //need to type check the parents, otherwise we don't have type information on the symbols
    val parentTypes = impl.parents.map(tree => c.typecheck(tree, c.TYPEmode))

    if (!parentTypes.find(parent => parent.tpe <:< recordTpe).isDefined) {
      c.abort(c.enclosingPosition, s"$className needs to extend Record")
    }

    //contains all fields from parent records
    val parentFields = parentTypes.map(p => getFieldInfo(p.tpe)).flatten.toMap

    var fields = mutable.Map.empty[String, Type]
    val transformedBody = impl.body.map(m =>
      m match {
        case methodDef: DefDef if methodDef.rhs.isEmpty =>
          val retType = c.typecheck(methodDef.tpt, c.TYPEmode)
          fields += (methodDef.name.toString -> retType.tpe)
          transformMethod(methodDef, parentFields)
        case x => x
      }
    )

    val tansformedBody2 = transformedBody ++ buildUpdateMethods(className, abstractMethods.filterNot(m => parentFields.contains(m.name.toString))) //if a field exists in a parent as well, we don't implement it

    //from now on fields contains inherited fields as well
    fields ++= parentFields

    val fieldsTpe = c.typeOf[fields]
    val newMods = Modifiers(mods.flags, mods.privateWithin, mods.annotations :+ q"new $fieldsTpe(..${fields.keys})")

    val transformedClass = ClassDef(newMods, className, typeParams, Template(impl.parents, impl.self, tansformedBody2))

    val transformedCompanionObject = companionObjectDef match {
      case None =>
        q"object ${className.toTermName} { ${buildConstructor(className)}; ${buildConstructor2(className, fields.toMap)}; ${buildUnapplyMethod(className, abstractMethods)} }"
      case Some(ModuleDef(mods, name, Template(parents, self, body))) =>
        ModuleDef(mods, name, Template(parents, self, body :+ buildConstructor(className) :+ buildConstructor2(className, fields.toMap) :+ buildUnapplyMethod(className, abstractMethods)))
    }

    q"$transformedClass; $transformedCompanionObject"
  }


  /**
   * Transform abstract methods, giving them an implementation.
   */
  private def transformMethod(methodDef: DefDef, parentFields: Map[String, Type]) = {
    val DefDef(modifiers, name, typeParams, argss, returnType, body) = methodDef
    val optionTpe = c.typeOf[Option[_]]

    if (!body.isEmpty) {
      //existing implementations are untouched
      methodDef
    } else if (parentFields.contains(name.toString)) {
      //in case a method is implemented in the parent class, we leave it untouched
      methodDef
    } else {

      if (argss.size > 0 && argss(0).size > 0) {
        throw new RecordException("abstract methods must not take parameters")
      }

      if (typeParams.size > 0)
        throw new RecordException("type parameters are not allowed on abstract method")

      val returnTpe = c.typecheck(returnType, c.TYPEmode)

      if (returnTpe.tpe <:< optionTpe) {
        q"def $name = data.get(${name.toString}).asInstanceOf[$returnType]"
      } else {
        q"def $name = data.apply(${name.toString}).asInstanceOf[$returnType]"
      }

    }


  }


  /**
   * Generate the update methods.
   */
  private def buildUpdateMethods(className: TypeName, abstractMethods: Seq[DefDef]) = {

    //only abstract methods are part of the copy method
    abstractMethods.map { m =>
      val DefDef(modifiers, name, typeParams, argss, returnType, body) = m
      //this needs to be a value definition because of the default parameter value
      //(q"val $name: $returnType = $name", q"(${name.toString}, $name)")

      q"def $name($name: $returnType): $className = ${className.toTermName}(data + ((${name.toString}, $name)))"
    }

    //val args = argsWithValues.map(_._1)
    //val values = argsWithValues.map(_._2)

    //q"def copy(..$args): $className = ${className.toTermName}(data ++ Seq(..$values))"
  }




  /**
   * Generate the map constructor, which creates a record from Map[String, Any].
   */
  private def buildConstructor(className: TypeName) = {
    q"""def apply(m: Map[String, Any]): $className = new $className {
          override val data = m
    }"""
  }


  /**
   * Generate the field constructor (like case classes have), which has a parameter for each field.
   */
  private def buildConstructor2(className: TypeName, fields: Map[String, Type]) = {

    val resultVarName = TermName(c.freshName("result"))

    val optionTpe = c.typeOf[Option[_]]

    val implBody = fields.map { entry =>
      val (name, tpe) = entry
      val termName = TermName(name)
      if (tpe <:< optionTpe) {
        val x = TermName(c.freshName("x"))
        q"""$resultVarName = $termName match {
              case None => $resultVarName
              case Some($x) => $resultVarName + (${name.toString} -> $x)
              }"""
      } else {
        q"$resultVarName = $resultVarName + (${name.toString} -> $termName)"
      }
    }

    val args = fields.map { entry =>
      val (name, tpe) = entry
      val termName = TermName(name)
      if (tpe <:< optionTpe) {
        q"val $termName: $tpe = None"
      } else {
        q"$termName: $tpe"
      }
    }


    q"""def apply(..$args): $className = {
          var $resultVarName = Map.empty[String, Any]
          ..$implBody
          ${className.toTermName}($resultVarName)
       }"""
  }

  /**
   * Generate the unapply method (on the companion object).
   */
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

  /**
   * Returns record information from the fields annotation.
   */
  private def getFieldInfo(tpe: Type): Map[String, Type] = {

    val fieldsTpe = c.typeOf[fields]

    val fieldsAnnot = tpe.typeSymbol.annotations.find(_.tree.tpe =:= fieldsTpe)

    val fieldNames = fieldsAnnot match {
      case None => Set.empty
      case Some(annot) =>
        annot.tree.children.foldLeft(Set.empty[String])((result, a) =>
          a match {
            case Literal(name) =>
              result + name.value.asInstanceOf[String]
            case _ => result
          }
        )
    }

    fieldNames.map { name =>
      //since there are at least two methods with the same name, we need to find the one without params
      val method = tpe.member(TermName(name)).alternatives.find(m => m.asMethod.paramLists.isEmpty)
      (name, method.head.asMethod.returnType)
    }.toMap

  }

}
