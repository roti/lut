package roti.lut.annotation

import scala.annotation.{StaticAnnotation}

/**
 * Metadata for records. Keeps the list of field names.
 */
class fields(fields: String*) extends StaticAnnotation


