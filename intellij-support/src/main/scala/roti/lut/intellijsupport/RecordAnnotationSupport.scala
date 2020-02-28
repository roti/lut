package roti.lut.intellijsupport

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunctionDeclaration
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.SyntheticMembersInjector




class RecordAnnotationSupport extends SyntheticMembersInjector {
  private val log = Logger.getInstance(classOf[RecordAnnotationSupport])
  log.info(s"RecordAnnotationSupport Injector loaded")
  override def injectFunctions(source: ScTypeDefinition): Seq[String] = {
    if (source.findAnnotationNoAliases("roti.lut.annotation.record") != null) {
      val copyArgs = source.members.map {
        case m: ScFunctionDeclaration => Some(s"${m.name}: ${m.returnType.getOrAny}")
        case _ => None
      }.flatten
      val copyMethod = s"def copy(${copyArgs.mkString(", ")}): ${source.name} = ???"
      //log.info("*** copy method: " + copyMethod)
      Seq(copyMethod, s"object ${source.name} { def apply(m: Map[String, Any]): ${source.name} = ??? }")
    } else {
      Seq.empty
    }
  }

  override def needsCompanionObject(source: ScTypeDefinition): Boolean = {
    if (source.findAnnotationNoAliases("roti.lut.annotation.record") != null) {
      true
    } else {
      false
    }
  }
}
