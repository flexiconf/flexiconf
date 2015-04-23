package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.SchemaParser.{FlagAllowOnceContext, FlagContext, FlagListContext}
import se.blea.flexiconf.parser.gen.SchemaParserBaseVisitor

import scala.collection.JavaConversions._


/** Returns DirectiveFlags based on supported flags for a directive */
private[flexiconf] object DirectiveFlagListVisitor extends SchemaParserBaseVisitor[Set[DirectiveFlag]] {
  def apply(ctx: ParserRuleContext): Set[DirectiveFlag] = ctx match {
    case flagList: FlagListContext => visitFlagList(flagList)
    case _ => Set.empty
  }

  override def visitFlagList(ctx: FlagListContext): Set[DirectiveFlag] = {
    ctx.flag.foldLeft(Set[DirectiveFlag]()) { (flags, current) =>
      DirectiveFlagVisitor(current) match {
        case Some(f) => flags + f
        case _ => flags
      }
    }
  }

  object DirectiveFlagVisitor extends SchemaParserBaseVisitor[Option[DirectiveFlag]] {
    def apply(ctx: ParserRuleContext): Option[DirectiveFlag] = ctx match {
      case flag: FlagContext => visitFlag(flag)
      case _ => None
    }

    override def visitFlagAllowOnce(ctx: FlagAllowOnceContext): Option[DirectiveFlag] = Some(DirectiveFlags.AllowOnce)
  }
}
