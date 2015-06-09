package se.blea.flexiconf


/** Tree node containing a directive and arguments that satisfy it */
private[flexiconf] case class ConfigNode(directive: DirectiveDefinition,
                                         arguments: List[Argument],
                                         source: Source,
                                         children: List[ConfigNode] = List.empty) {

  if (arguments.size != directive.parameters.size) {
    throw new IllegalStateException(s"Argument count must match parameter count: " +
      s"expected ${directive.parameters.size} but got ${arguments.size}")
  }

  /** The node is named after the directive */
  val name = directive.name

  /** Return allowed directives within this scope */
  val allowedDirectives = directive.children

  /** Return allowed arguments for this directive */
  val allowedArguments = directive.parameters

  /** True if the node is for an built-in directive */
  val isInternalNode = name.startsWith("$")

  /** True if the node is a root node */
  val isRootNode = name == "$root"

  /** True if the node is not internal, or is a root node - the root node is considered an internal and user node
    * so directives and groups at the top-most level of the configuration can be associated with a node */
  val isUserNode = !isInternalNode || isRootNode

  /** Collect all warnings for this tree */
  def warnings: List[String] = {
    children.flatMap { node =>
      if (node.directive == DirectiveDefinition.warning) {
        val msg = node.arguments(0).value
        List(s"$msg at $source")
      } else {
        node.warnings
      }
    }
  }

  /** Collapse the tree and remove all internal nodes */
  def collapse: ConfigNode = {
    copy(children = children.flatMap { node =>
      if (node.isInternalNode) {
        node.collapse.children
      } else {
        List(node.collapse)
      }
    })
  }

  /** Return a string representing a configuration tree */
  def renderTree(depth: Int = 0): String = {
    val sb = StringBuilder.newBuilder

    sb ++= ("  " * depth)
    sb ++= s"> $this\n"
    sb ++= children flatMap (_.renderTree(depth + 1))

    sb.mkString
  }

  override def toString = {
    var res = name

    if (arguments.nonEmpty) {
      res ++= (arguments map (_.value)).mkString(" ", " ", "")
    }

    res ++ s" ($source)"
  }
}
