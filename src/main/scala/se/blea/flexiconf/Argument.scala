package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.ConfigBaseVisitor
import se.blea.flexiconf.parser.gen.ConfigParser._

import scala.collection.JavaConversions._


/**
 * Base trait for argument types
 */
sealed trait ArgumentType[T] {
  /** Returns true if the value meets the criteria for this type */
  def accepts(value: String): Boolean

  /** Returns the native value for the provided value */
  def valueOf(value: String): T

  /** Returns a new argument using this type **/
  def apply(value: String) = Argument(value, this)
}


/** Container for an argument value: value and type */
case class Argument(value: String, kind: ArgumentType[_] = StringArgument) {
  override def toString = s"$kind<$value>"
}


/** Boolean values */
case object BoolArgument extends ArgumentType[Boolean] {
  val boolTruePattern = "on|yes|y|true"
  val boolFalsePattern = "off|no|n|false"
  val boolPattern = boolTruePattern ++ "|" ++ boolFalsePattern

  override def accepts(value: String) = value.toLowerCase matches boolPattern
  override def valueOf(value: String) = value.toLowerCase matches boolTruePattern
  override def toString = "Bool"
}


/** Integer values */
case object IntArgument extends ArgumentType[Long] {
  val intPattern = "0|[1-9]\\d*"

  override def accepts(value: String) = value matches intPattern
  override def valueOf(value: String) = value.toLong
  override def toString = "Int"
}


/** Decimal values */
case object DecimalArgument extends ArgumentType[Double] {
  val decimalPattern = "(0|[1-9]\\d*)(\\.\\d+)?"

  override def accepts(value: String) = value matches decimalPattern
  override def valueOf(value: String) = value.toDouble
  override def toString = "Decimal"
}


/** String values */
case object StringArgument extends ArgumentType[String] {
  override def accepts(value: String) = true
  override def valueOf(value: String) = value
  override def toString = "String"
}

/** Unknown values */
case object UnknownArgument extends ArgumentType[Unit] {
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
}
