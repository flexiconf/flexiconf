package se.blea.flexiconf

import se.blea.flexiconf.parser.gen.SchemaParser._
import se.blea.flexiconf.parser.gen.SchemaParserBaseVisitor


/** Returns an ArgumentType for supported parameter types */
private[flexiconf] object ParameterTypeVisitor extends SchemaParserBaseVisitor[ArgumentKind[_]] {
  override def visitStringType(ctx: StringTypeContext): ArgumentKind[_] = StringArgument
  override def visitIntegerType(ctx: IntegerTypeContext): ArgumentKind[_] = IntArgument
  override def visitBooleanType(ctx: BooleanTypeContext): ArgumentKind[_] = BoolArgument
  override def visitDecimalType(ctx: DecimalTypeContext): ArgumentKind[_] = DecimalArgument
  override def visitDurationType(ctx: DurationTypeContext): ArgumentKind[_] = DurationArgument
  override def visitPercentageType(ctx: PercentageTypeContext): ArgumentKind[_] = PercentageArgument
}
