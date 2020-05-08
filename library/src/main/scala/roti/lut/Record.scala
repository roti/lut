package roti.lut

import roti.lut.annotation.fields

import scala.language.experimental.macros
import scala.reflect.ClassTag



/** Base trait for a Record.
 *
 */
trait Record extends Equals {

  val data: Map[String, Any]


  override def canEqual(that: Any): Boolean = that.isInstanceOf[Record] || that.isInstanceOf[Map[_,_]]

  override def equals(that: Any): Boolean = that match {
    case r: Record => data.equals(r.data)
    case m: Map[_, _] => data.equals(m)
    case _ => false
  }
}


object Record {

  import scala.reflect.runtime.{universe => ru}

  /**
   * Recursively converts a map to the record.
   * TODO Currently implemented via reflection. Search for better alternatives.
   */
  def to[T <: Record](data: Map[String, Any])(implicit tt: ru.TypeTag[T], ct: ClassTag[T]): T = {
    to(data, tt.tpe).asInstanceOf[T]
  }


  private def to(data: Map[String, Any], tpe: ru.Type): Any = {
  //TODO use T's classloader, not ours
    val m = ru.runtimeMirror(getClass.getClassLoader)
    val cm = m.reflectModule(tpe.typeSymbol.companion.asModule)
    val ctorSymbol = cm.symbol.typeSignature.member(ru.TermName("apply")).alternatives.find(m =>
      m.asMethod.paramLists.map(_.map(_.typeSignature)) match {
        case (typeSig :: Nil) :: Nil if typeSig =:= ru.typeOf[Map[String, Any]] => true
        case _ => false
      }
    ).get.asMethod

    val ctor = m.reflect(cm.instance).reflectMethod(ctorSymbol)

    val recordTpe = ru.typeOf[Record]
    val optionTpe = ru.typeOf[Option[_]]

    val fields = getFieldInfo(tpe)
    //val fields = tpe.decls.filter(m => m.isMethod && m.annotations.find(a => a.tree.tpe =:= fieldAnnTpe).isDefined).map(_.asMethod)
    val subRecordFields = fields.filter(m => m._2 <:< recordTpe)
    val optSubRecordFields = fields.filter(m => m._2 <:< optionTpe && m._2.typeArgs.head <:< recordTpe)

    val convertedData = subRecordFields.foldLeft(data) { (result, m) =>
      val fieldName = m._1
      val data = result.getOrElse(fieldName, throw new RecordException(s"missing data for $fieldName")).asInstanceOf[Map[String, Any]]
      result + (fieldName -> to(data, m._2))
    }

    val convertedData2 = optSubRecordFields.foldLeft(convertedData) { (result, m) =>
      val fieldName = m._1
      val tpe = m._2.typeArgs.head
      result.get(fieldName).map(ddata =>
        result + (fieldName -> to(ddata.asInstanceOf[Map[String, Any]], tpe))
      ).getOrElse(result)
    }

    ctor(convertedData2)

  }


  /**
   * Returns record information from the fields annotation.
   * Basically the Same as RecordMacroImpl.getFieldInfo
   */
  private def getFieldInfo(tpe: ru.Type): Map[String, ru.Type] = {

    val fieldsTpe = ru.typeOf[fields]

    val fieldsAnnot = tpe.typeSymbol.annotations.find(_.tree.tpe =:= fieldsTpe)

    val fieldNames = fieldsAnnot match {
      case None => Set.empty
      case Some(annot) =>
        annot.tree.children.foldLeft(Set.empty[String])((result, a) =>
          a match {
            case ru.Literal(name) =>
              result + name.value.asInstanceOf[String]
            case _ => result
          }
        )
    }

    fieldNames.map { name =>
      //since there are at least two methods with the same name, we need to find the one without params
      val method = tpe.member(ru.TermName(name)).alternatives.find(m => m.asMethod.paramLists.isEmpty)
      (name, method.head.asMethod.returnType)
    }.toMap

  }

}
