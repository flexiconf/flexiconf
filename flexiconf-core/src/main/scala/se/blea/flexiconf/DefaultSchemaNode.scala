package se.blea.flexiconf


/** Tree node that represents part of a schema */
private[flexiconf] case class DefaultSchemaNode(name: String,
                                                parameters: List[Parameter],
                                                source: Source,
                                                flags: Set[DirectiveFlag] = Set.empty,
                                                documentation: String = "",
                                                children: List[DefaultSchemaNode] = List.empty) extends SchemaNode {

  // Parameter validation

  // Names must be unique
  val paramNames = parameters.map(_.name)
  if (paramNames.size != paramNames.toSet.size) {
    throw new IllegalStateException(s"Directive '$name' must use a unique name for each parameter at $source")
  }

  // Parameter types must be known
  parameters.foreach { p =>
    if (p.kind == UnknownArgument) {
      throw new IllegalStateException(s"Unknown type for parameter '${p.name}' at $source")
    }
  }

  override def toDirectives: Set[DirectiveDefinition] = {
    children.map(_.toDirective).toSet
  }

  /** Collapse the tree and remove all internal nodes */
  def collapse: DefaultSchemaNode = {
    copy(children = children.flatMap { node =>
      if (node.name.startsWith("$")) {
        node.collapse.children
      } else {
        List(node.collapse)
      }
    })
  }

  /** Convert this SchemaNode to a tree of directives */
  def toDirective: DirectiveDefinition = {
    DirectiveDefinition.withUnsafeName(name)
      .withParameters(parameters)
      .withFlags(flags)
      .withDocumentation(documentation)
      .withDirectives(collapse.children.map(_.toDirective):_*)
      .build
  }

  override def toString: String = {
    var res = name

    if (parameters.nonEmpty) {
      res ++= (parameters map (_.toString)).mkString(" ", " ", "")
    }

    if (flags != DirectiveFlags()) {
      res ++= s" $flags"
    }

    res ++ s" ($source)"
  }

  /** Return a string representing a configuration tree */
  def renderTree(depth: Int = 0): String = {
    val sb = StringBuilder.newBuilder

    sb ++= ("  " * depth)
    sb ++= s"> $this\n"
    sb ++= children flatMap (_.renderTree(depth + 1))

    sb.mkString
  }
}
