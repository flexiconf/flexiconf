package se.blea.flexiconf.docgen

import org.pegdown.PegDownProcessor
import se.blea.flexiconf.DirectiveFlags.AllowOnce
import se.blea.flexiconf.SchemaNode

/** Documentation generator that parses doc comments as markdown and creates HTML */
object MarkdownDocGenerator extends DocGenerator {
  lazy val processor = new PegDownProcessor()

  override def process(node: SchemaNode): String = {
    s"""
       |<html>
       |  <head>
       |    <link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/3.3.2/css/bootstrap.min.css'>
       |    <style>
       |      ol { counter-reset: item; padding-left: 0; margin-top: .2em }
       |      ol li { display: block; margin-bottom: .2em }
       |      ol li.block { display: block; margin-top: .8em }
       |      ol li:before { content: counters(item, '.') ' '; counter-increment: item; padding-right: .4em }
       |    </style>
       |  </head>
       |  <body>
       |    <div class="container">
       |      <div class="row">
       |        <div class="col-md-3">
       |          <hr>
       |          <h4>Table of contents</h4>
       |          <ol>${formatToc(node)}</ol>
       |        </div>
       |        <div class="col-md-9">
       |          ${formatContent(node)}
       |        </div>
       |      </div>
       |    </div>
       |  </body>
       |</html>
     """.stripMargin
  }

  def formatToc(node: SchemaNode): String = {
    val children = node.children.map(formatToc).mkString("")

    if (node.name.startsWith("$")) {
      children
    } else {
      val title = node.name
      val ref = node.name.toLowerCase
      val blockClass = if (node.children.nonEmpty) {
        "block"
      } else {
        ""
      }

      s"""
         |<li class='$blockClass'>
         |  <a href='#$ref'>$title</a>
         |  <ol>
         |    $children
         |  </ol>
         |</li>
       """.stripMargin
    }
  }

  def formatContent(node: SchemaNode): String = {
    val children = node.children.map(formatContent).mkString("")

    if (node.name.startsWith("$")) {
      children
    } else {
      val title = node.name
      val ref = node.name.toLowerCase
      val syntax = formatSyntax(node)
      val restrictions = formatFlags(node)
      val notes = processor.markdownToHtml(node.documentation)

      s"""
         |<div>
         |<hr>
         |<h4 id='$ref'>$title</h4>
         |<div class='panel panel-default'>
         |  <div class='panel-heading'>
         |    <code>$syntax</code>
         |  </div>
         |  <div class='panel-body'>
         |    $notes
         |    $restrictions
         |    $children
         |  </div>
         |</div>
         |</div>
       """.stripMargin
    }
  }

  def formatFlags(node: SchemaNode): String = {
    val flags = node.flags.map({
      case f if f == AllowOnce => "May only defined once in this context"
    })

    if (flags.nonEmpty) {
      "<b>Restrictions:</b>" + flags.mkString("<ul><li>", "</li><li>", "</li></ul>")
    } else {
      ""
    }
  }

  def formatSyntax(node: SchemaNode): String = {
    var syntax = node.name

    if (node.parameters.nonEmpty) {
      syntax += " " + node.parameters.mkString(" ")
    }

    if (node.children.nonEmpty) {
      syntax += " { ... }"
    } else {
      syntax += ";"
    }

    syntax
  }
}
