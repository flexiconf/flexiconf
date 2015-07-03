package se.blea.flexiconf

/** Values needed to determine whether a possible directive matches an actual one */
case class MaybeDirective(private[flexiconf] val name: String,
                          private[flexiconf] val arguments: Seq[Argument] = Seq.empty,
                          private[flexiconf] val hasBlock: Boolean = false) {

  /** Returns true if the provided provided Directive matches this MaybeDirective */
  private[flexiconf] def matches(directive: DirectiveDefinition): Boolean = {
    val argumentKinds = arguments map (_.kind)
    val parameterKinds = directive.parameters map (_.kind)

    val matchesName = name == directive.name
    val matchesArgs = argumentKinds == parameterKinds
    val matchesBlock = directive.requiresBlock && hasBlock || !directive.requiresBlock && !hasBlock

    matchesName && matchesArgs && matchesBlock
  }

  /** Returns true if the provided Directive doesn't match this MaybeDirective */

  private[flexiconf] def doesNotMatch(directive: DirectiveDefinition): Boolean = {
    !matches(directive)
  }

  override def toString: String = {
    var res = name

    if (arguments.nonEmpty) {
      res ++= arguments.map({ a => s"<${a.value}>:${a.kind}" }).mkString(" ", " ", "")
    }

    if (hasBlock) {
      res ++= " {}"
    }

    res
  }
}
