package se.blea.flexiconf

import se.blea.flexiconf.parser.gen.SchemaParser.DirectiveListContext


/** Not all data can be carried on the stack, so we have an additional context object that carries this state */
private[flexiconf] case class SchemaVisitorContext(var groupsByNode: Map[DefaultSchemaNode, Map[String, DirectiveListContext]] = Map.empty) {

  /** Associate a named group of directives with the closest, non-internal node or the root node */
  def addGroup(node: DefaultSchemaNode, name: String, directives: DirectiveListContext): Unit = {
    val combinedGroups = groupsByNode.get(node) map { gs =>
      gs + (name -> directives)
    } getOrElse {
      Map(name -> directives)
    }

    groupsByNode = groupsByNode + (node -> combinedGroups)
  }
}
