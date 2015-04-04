package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.ConfigBaseVisitor
import se.blea.flexiconf.parser.gen.ConfigParser._

import scala.collection.JavaConversions._


/**
 * Base trait for argument types
 */
sealed trait ArgumentKind[T] {
  /** Returns true if the value meets the criteria for this type */
  def accepts(value: String): Boolean

  /** Returns the native value for the provided value */
  def valueOf(value: String): T

  /** Returns a new argument using this type **/
  def apply(value: String) = Argument(value, this)
}


/** Container for an argument value: name, value, and kind */
case class Argument(value: String, 
                    kind: ArgumentKind[_] = StringArgument,
                    name: String = "?") {
  override def toString = s"$name:$kind<$value>"
}


/** Boolean values */
case object BoolArgument extends ArgumentKind[Boolean] {
  val boolTruePattern = "on|yes|y|true"
  val boolFalsePattern = "off|no|n|false"
  val boolPattern = boolTruePattern ++ "|" ++ boolFalsePattern

  override def accepts(value: String) = value.toLowerCase matches boolPattern
  override def valueOf(value: String) = value.toLowerCase matches boolTruePattern
  override def toString = "Bool"
}


/** Integer values */
case object IntArgument extends ArgumentKind[Long] {
  val intPattern = "0|[1-9]\\d*"

  override def accepts(value: String) = value matches intPattern
  override def valueOf(value: String) = value.toLong
  override def toString = "Int"
}


/** Decimal values */
case object DecimalArgument extends ArgumentKind[Double] {
  val decimalPattern = "(0|[1-9]\\d*)(\\.\\d+)?"

  override def accepts(value: String) = value matches decimalPattern
  override def valueOf(value: String) = value.toDouble
  override def toString = "Decimal"
}


/** Duration values */
case object DurationArgument extends ArgumentKind[Long] {
  val durationPattern = "((?:0|[1-9]\\d*)(?:\\.\\d+)?)(ms|s|m|h|d|w|M|y)".r
  val multipliers = Map("ms" -> 1l,
                        "s" -> 1000l,
                        "m" -> 60000l,
                        "h" -> 3600000l,
                        "d" -> 86400000l,
                        "w" -> 604800000l,
                        "M" -> 26297460000l,
                        "y" -> 315569520000l)

  override def accepts(value: String) = durationPattern.pattern.matcher(value).matches
  override def valueOf(value: String) = value match {
    case durationPattern(amount, unit) => (amount.toDouble * multipliers.getOrElse(unit, 1l)).toLong
    case _ => throw new IllegalStateException(s"Can't get duration value from $value")
  }
  override def toString = "Duration"
}


/** Percentage values */
case object PercentageArgument extends ArgumentKind[Double] {
  val percentagePattern = "((?:0|[1-9]\\d*)(?:\\.\\d+)?)%".r

  override def accepts(value: String) = percentagePattern.pattern.matcher(value).matches
  override def valueOf(value: String) = value match {
    case percentagePattern(amount) => amount.toDouble / 100
    case _ => throw new IllegalStateException(s"Can't get percentage value from $value")
  }
  override def toString = "Percentage"
}


/** String values */
case object StringArgument extends ArgumentKind[String] {
  override def accepts(value: String) = true
  override def valueOf(value: String) = value
  override def toString = "String"
}

/** Unknown values */
case object UnknownArgument extends ArgumentKind[Unit] {
  override def accepts(value: String) = false
  override def valueOf(value: String) = throw new IllegalStateException("Can't get value of argument with unknown type")
  override def toString = "Unknown"
}


/** Parse argument lists and argument values **/
private[flexiconf] object ArgumentVisitor extends ConfigBaseVisitor[Argument] {
  def apply(ctx: ParserRuleContext): List[Argument] = ctx match {
    case argList: ArgumentListContext => (argList.argument map visitArgument).toList
    case arg: ArgumentContext => List(visitArgument(arg))
    case arg: StringArgumentContext => List(visitStringArgument(arg))
    case _ => List.empty
  }

  override def visitUnquotedStringValue(ctx: UnquotedStringValueContext) = StringArgument(ctx.getText)
  override def visitQuotedStringValue(ctx: QuotedStringValueContext) = StringArgument(ctx.getText.substring(1, ctx.getText.size - 1))
  override def visitIntegerValue(ctx: IntegerValueContext) = IntArgument(ctx.getText)
  override def visitBooleanValue(ctx: BooleanValueContext) = BoolArgument(ctx.getText)
  override def visitDecimalValue(ctx: DecimalValueContext) = DecimalArgument(ctx.getText)
  override def visitDurationValue(ctx: DurationValueContext) = DurationArgument(ctx.getText)
  override def visitPercentageValue(ctx: PercentageValueContext) = PercentageArgument(ctx.getText)
}
