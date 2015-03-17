package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.SchemaParser.{DocumentationContentContext, DocumentationLineContext, DocumentationBlockContext}
import se.blea.flexiconf.parser.gen.SchemaParserBaseVisitor

import scala.collection.JavaConversions._

/** Returns a Seq of strings for a given context */
private[flexiconf] object DocumentationVisitor extends SchemaParserBaseVisitor[String] {
  def apply(ctx: ParserRuleContext): Seq[String] = ctx match {
    case block: DocumentationBlockContext => block.documentationLine() flatMap apply
    case line: DocumentationLineContext => Seq(visitDocumentationContent(line.documentationContent()))
    case _ => List.empty
  }

  override def visitDocumentationContent(ctx: DocumentationContentContext): String = ctx match {
    case ctx:DocumentationContentContext => ctx.getText
    case _ => ""
  }
}
