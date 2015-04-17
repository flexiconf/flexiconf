package se.blea.flexiconf

import se.blea.flexiconf.parser.gen.ConfigParser.DirectiveListContext


/** Not all data can be carried on the stack, so we have an additional context object that carries this state */
private[flexiconf] case class ConfigVisitorContext(var groupsByNode: Map[ConfigNode, Map[String, DirectiveListContext]] = Map.empty,
                                                    var directivesByNode: Map[ConfigNode, Set[DirectiveDefinition]] = Map.empty) {

  def addGroup(frame: ConfigNode, name: String, directives: DirectiveListContext): Unit = {
    val combinedGroups = groupsByNode.get(frame) map { gs =>
      gs + (name -> directives)
    } getOrElse {
      Map(name -> directives)
    }

    groupsByNode = groupsByNode + (frame -> combinedGroups)
  }

  def addDirective(frame: ConfigNode, directive: DirectiveDefinition): Unit = {
    val combinedDirectives = directivesByNode.get(frame) map { ds =>
      ds + directive
    } getOrElse {
      Set(directive)
    }

    directivesByNode = directivesByNode + (frame -> combinedDirectives)
  }
}
