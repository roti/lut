package roti.lut

import roti.lut.annotation.field

import scala.language.experimental.macros
import scala.reflect.ClassTag



/**
 * Base trait for all Records
 */
trait Record extends Map[String, Any] {

  val data: Map[String, Any]

  override def removed(key: String): Map[String, Any] = data.removed(key)

  override def updated[V1 >: Any](key: String, value: V1): Map[String, V1] = data.updated(key, value)

  override def get(key: String): Option[Any] = data.get(key)

  override def iterator: Iterator[(String, Any)] = data.iterator

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

    val fieldAnnTpe = ru.typeOf[field]
    val recordTpe = ru.typeOf[Record]
    val optionTpe = ru.typeOf[Option[_]]

    val fields = tpe.decls.filter(m => m.isMethod && m.annotations.find(a => a.tree.tpe =:= fieldAnnTpe).isDefined).map(_.asMethod)
    val subRecordFields = fields.filter(m => m.returnType <:< recordTpe)
    val optSubRecordFields = fields.filter(m => m.returnType <:< optionTpe && m.returnType.typeArgs.head <:< recordTpe)

    val convertedData = subRecordFields.foldLeft(data) { (result, m) =>
      val fieldName = m.name.toString
      val data = result.getOrElse(fieldName, throw new RecordException(s"missing data for $fieldName")).asInstanceOf[Map[String, Any]]
      result + (fieldName -> to(data, m.returnType))
    }

    val convertedData2 = optSubRecordFields.foldLeft(convertedData) { (result, m) =>
      val fieldName = m.name.toString
      val tpe = m.returnType.typeArgs.head
      result.get(fieldName).map(ddata =>
        result + (fieldName -> to(ddata.asInstanceOf[Map[String, Any]], tpe))
      ).getOrElse(result)
    }

    ctor(convertedData2)

  }


}
