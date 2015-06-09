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

  override def accepts(value: String) = value.toLowerCase matches boolPattern
  override def valueOf(value: String) = BoolValue(value.toLowerCase matches boolTruePattern)
  override def toString = "Bool"
}


/** Integer values */
case object IntArgument extends ArgumentKind[Long] {
  val intPattern = "0|[1-9]\\d*"

  override def accepts(value: String) = value matches intPattern
  override def valueOf(value: String) = LongValue(value.toLong)
  override def toString = "Int"
}


/** Decimal values */
case object DecimalArgument extends ArgumentKind[Double] {
  val decimalPattern = "(0|[1-9]\\d*)(\\.\\d+)?"

  override def accepts(value: String) = value matches decimalPattern
  override def valueOf(value: String) = DoubleValue(value.toDouble)
  override def toString = "Decimal"
}


/** Duration values */
case object DurationArgument extends ArgumentKind[Long] {
  val durationPattern = "((?:0|[1-9]\\d*)(?:\\.\\d+)?)(ms|s|m|h|d|w|M|y)".r
  val multipliers = Map(
    "ms" -> 1l,
    "s"  -> 1000l,
    "m"  -> 60000l,
    "h"  -> 3600000l,
    "d"  -> 86400000l,
    "w"  -> 604800000l,
    "M"  -> 26297460000l,
    "y"  -> 315569520000l)

  override def accepts(value: String) = durationPattern.pattern.matcher(value).matches
  override def valueOf(value: String) = value match {
    case durationPattern(amount, unit) => LongValue((amount.toDouble * multipliers.getOrElse(unit, 1l)).toLong)
    case _ => throw new IllegalStateException(s"Can't get duration value from $value")
  }
  override def toString = "Duration"
}


/** Percentage values */
case object PercentageArgument extends ArgumentKind[Double] {
  val percentagePattern = "((?:0|[1-9]\\d*)(?:\\.\\d+)?)%".r

  override def accepts(value: String) = percentagePattern.pattern.matcher(value).matches
  override def valueOf(value: String) = value match {
    case percentagePattern(amount) => DoubleValue(amount.toDouble / 100)
    case _ => throw new IllegalStateException(s"Can't get percentage value from $value")
  }
  override def toString = "Percentage"
}


/** String values */
case object StringArgument extends ArgumentKind[String] {
  override def accepts(value: String) = true
  override def valueOf(value: String) = StringValue(value)
  override def toString = "String"
}


/** Unknown values */
case object UnknownArgument extends ArgumentKind[Unit] {
  override def accepts(value: String) = true
  override def valueOf(value: String) = NullValue
  override def toString = "Unknown"
}
