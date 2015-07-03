package se.blea.flexiconf


/** Base trait for argument types */
sealed trait ArgumentKind[T] {
  /** Returns true if the value meets the criteria for this type */
  def accepts(value: String): Boolean

  /** Returns the native value for the provided value */
  def valueOf(value: String): ArgumentValue

  /** Returns a new argument using this type **/
  def apply(value: String): Argument = Argument(value, this)
}


/** Boolean values */
case object BoolArgument extends ArgumentKind[Boolean] {
  val boolTruePattern = "on|yes|y|true"
  val boolFalsePattern = "off|no|n|false"
  val boolPattern = boolTruePattern ++ "|" ++ boolFalsePattern

  override def accepts(value: String): Boolean = value.toLowerCase matches boolPattern
  override def valueOf(value: String): ArgumentValue = BoolValue(value.toLowerCase matches boolTruePattern)
  override def toString: String = "Bool"
}


/** Integer values */
case object IntArgument extends ArgumentKind[Long] {
  val intPattern = "(-?(?:0|[1-9]\\d*))"

  override def accepts(value: String): Boolean = value matches intPattern
  override def valueOf(value: String): ArgumentValue = LongValue(value.toLong)
  override def toString: String = "Int"
}


/** Decimal values */
case object DecimalArgument extends ArgumentKind[Double] {
  val decimalPattern = "(-?(?:0|[1-9]\\d*))(\\.\\d+)?"

  override def accepts(value: String): Boolean = value matches decimalPattern
  override def valueOf(value: String): ArgumentValue = DoubleValue(value.toDouble)
  override def toString: String = "Decimal"
}


/** Duration values */
case object DurationArgument extends ArgumentKind[Long] {
  val durationPattern = "(-?(?:0|[1-9]\\d*)(?:\\.\\d+)?)(ms|s|m|h|d|w|M|y)".r
  val multipliers = Map(
    "ms" -> 1L,
    "s"  -> 1000L,
    "m"  -> 60000L,
    "h"  -> 3600000L,
    "d"  -> 86400000L,
    "w"  -> 604800000L,
    "M"  -> 26297460000L,
    "y"  -> 315569520000L)

  override def accepts(value: String): Boolean = durationPattern.pattern.matcher(value).matches
  override def valueOf(value: String): ArgumentValue = value match {
    case durationPattern(amount, unit) => LongValue((amount.toDouble * multipliers.getOrElse(unit, 1L)).toLong)
    case _ => throw new IllegalStateException(s"Can't get duration value from $value")
  }
  override def toString: String = "Duration"
}


/** Percentage values */
case object PercentageArgument extends ArgumentKind[Double] {
  val percentagePattern = "(-?(?:0|[1-9]\\d*)(?:\\.\\d+)?)%".r

  override def accepts(value: String): Boolean = percentagePattern.pattern.matcher(value).matches
  override def valueOf(value: String): ArgumentValue = value match {
    case percentagePattern(amount) => DoubleValue(amount.toDouble / 100)
    case _ => throw new IllegalStateException(s"Can't get percentage value from $value")
  }
  override def toString: String = "Percentage"
}


/** String values */
case object StringArgument extends ArgumentKind[String] {
  override def accepts(value: String): Boolean = true
  override def valueOf(value: String): ArgumentValue = StringValue(value)
  override def toString: String = "String"
}


/** Unknown values */
case object UnknownArgument extends ArgumentKind[Unit] {
  override def accepts(value: String): Boolean = true
  override def valueOf(value: String): ArgumentValue = NullValue
  override def toString: String = "Unknown"
}
