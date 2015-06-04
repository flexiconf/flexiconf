package se.blea.flexiconf


/** Value trait for representing argument values **/
sealed trait ArgumentValue {
  def intValue: Option[Int]
  def longValue: Option[Long]
  def floatValue: Option[Float]
  def doubleValue: Option[Double]
  def stringValue: Option[String]
  def boolValue: Option[Boolean]

  /** Return this value, or the other value **/
  def or(other: ArgumentValue): ArgumentValue = OptionalValue(this, other)
  def |(other: ArgumentValue) = or(other)
}


/** Value class companion for implicit conversions **/
object ArgumentValue {
  implicit def argument2string(a: ArgumentValue): String = a.stringValue.getOrElse("")
  implicit def argument2bool(a: ArgumentValue): Boolean = a.boolValue.getOrElse(false)
  implicit def argument2int(a: ArgumentValue): Int = a.intValue.getOrElse(0)
  implicit def argument2long(a: ArgumentValue): Long = a.longValue.getOrElse(0)
  implicit def argument2float(a: ArgumentValue): Float = a.floatValue.getOrElse(0.0f)
  implicit def argument2double(a: ArgumentValue): Double = a.doubleValue.getOrElse(0.0)

  implicit def string2argument(v: String): ArgumentValue = StringValue(v)
  implicit def bool2argument(v: Boolean): ArgumentValue = BoolValue(v)
  implicit def int2argument(v: Int): ArgumentValue = LongValue(v)
  implicit def long2argument(v: Long): ArgumentValue = LongValue(v)
  implicit def float2argument(v: Float): ArgumentValue = DoubleValue(v)
  implicit def double2argument(v: Double): ArgumentValue = DoubleValue(v)
}


/** Value class for allowing defaults **/
case class OptionalValue(value: ArgumentValue, other: ArgumentValue) extends ArgumentValue {
  override def intValue: Option[Int] = value.intValue.orElse(other.intValue)
  override def doubleValue: Option[Double] = value.doubleValue.orElse(other.doubleValue)
  override def floatValue: Option[Float] = value.floatValue.orElse(other.floatValue)
  override def boolValue: Option[Boolean] = value.boolValue.orElse(other.boolValue)
  override def longValue: Option[Long] = value.longValue.orElse(other.longValue)
  override def stringValue: Option[String] = value.stringValue.orElse(other.stringValue)
  override def or(other2: ArgumentValue) = OptionalValue(other, other2)
}


/** Value class implementation for implicit conversions **/
object NullValue extends ArgumentValue {
  override def intValue: Option[Int] = None
  override def doubleValue: Option[Double] = None
  override def floatValue: Option[Float] = None
  override def boolValue: Option[Boolean] = None
  override def longValue: Option[Long] = None
  override def stringValue: Option[String] = None
  override def or(other: ArgumentValue) = other
}


/** Value class implementation for implicit conversions **/
case class BoolValue(value: Boolean) extends ArgumentValue {
  override def intValue: Option[Int] = None
  override def doubleValue: Option[Double] = None
  override def floatValue: Option[Float] = None
  override def boolValue: Option[Boolean] = Some(value)
  override def longValue: Option[Long] = None
  override def stringValue: Option[String] = Some(value.toString)
}


/** Value class implementation for implicit conversions **/
case class LongValue(value: Long) extends ArgumentValue {
  override def intValue: Option[Int] = Some(value.toInt)
  override def doubleValue: Option[Double] = Some(value.toDouble)
  override def floatValue: Option[Float] = Some(value.toFloat)
  override def boolValue: Option[Boolean] = None
  override def longValue: Option[Long] = Some(value.toLong)
  override def stringValue: Option[String] = Some(value.toString)
}


/** Value class implementation for implicit conversions **/
case class DoubleValue(value: Double) extends ArgumentValue {
  override def intValue: Option[Int] = Some(value.toInt)
  override def doubleValue: Option[Double] = Some(value.toDouble)
  override def floatValue: Option[Float] = Some(value.toFloat)
  override def boolValue: Option[Boolean] = None
  override def longValue: Option[Long] = Some(value.toLong)
  override def stringValue: Option[String] = Some(value.toString)
}


/** Value class implementation for implicit conversions **/
case class StringValue(value: String) extends ArgumentValue {
  override def intValue: Option[Int] = {
    if (DecimalArgument.accepts(value)) {
      Some(value.toInt)
    } else {
      None
    }
  }

  override def doubleValue: Option[Double] = {
    if (DecimalArgument.accepts(value)) {
      Some(value.toDouble)
    } else {
      None
    }
  }

  override def floatValue: Option[Float] = {
    if (DecimalArgument.accepts(value)) {
      Some(value.toFloat)
    } else {
      None
    }
  }

  override def boolValue: Option[Boolean] = {
    if (BoolArgument.accepts(value)) {
      Some(value.toBoolean)
    } else {
      None
    }
  }

  override def longValue: Option[Long] = {
    if (DecimalArgument.accepts(value)) {
      Some(value.toLong)
    } else {
      None
    }
  }

  override def stringValue: Option[String] = Some(value)
}
