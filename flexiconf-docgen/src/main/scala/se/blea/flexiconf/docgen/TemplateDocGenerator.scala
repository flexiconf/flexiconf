package se.blea.flexiconf.docgen

import java.io.StringWriter
import java.util

import org.pegdown.PegDownProcessor

import scala.collection.JavaConversions._

import com.github.mustachejava.DefaultMustacheFactory
import se.blea.flexiconf.{Parameter, DirectiveFlags, DirectiveFlag, SchemaNode}


class TemplateDocGenerator(templatePath: String) extends DocGenerator {
  lazy val processor = new PegDownProcessor()
  lazy val mf = new DefaultMustacheFactory()

  private def presentNode(node: SchemaNode): java.util.Map[String, Any] = {
    val name = node.name
    val arity = node.parameters.size
    val params = node.parameters.map(presentNodeParams).mkString(" ")
    val blockFlag = if (node.children.nonEmpty) { "*" } else { "" }
    val id = s"$name/$arity$blockFlag"

    Map(
      "name" -> name,
      "id" -> id,
      "arity" -> arity,
      "syntax" -> s"$name $params",
      "notes" -> processor.markdownToHtml(node.documentation),
      "flags" -> node.flags.map(_.documentation),
      "directives" -> new util.ArrayList(node.children.map(presentNode))
    )
  }

  private def presentNodeParams(param: Parameter) = s"${param.name}:${param.kind}"

  override def process(node: SchemaNode): String = {
    val w = new StringWriter()
    val ctx = new util.HashMap[String, Object](Map(
      "directives" -> new util.ArrayList(node.children.map(presentNode))
    ))

    mf.compile(templatePath).execute(w, ctx)

    w.toString
  }
}
