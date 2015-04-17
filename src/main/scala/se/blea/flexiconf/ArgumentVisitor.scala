package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.ConfigBaseVisitor
import se.blea.flexiconf.parser.gen.ConfigParser._

import scala.collection.JavaConversions._


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
