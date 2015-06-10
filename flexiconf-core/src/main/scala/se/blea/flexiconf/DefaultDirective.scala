package se.blea.flexiconf

/** Object that represents missing directives in the config **/
private[flexiconf] object NullDirective {
  def apply(definition: DirectiveDefinition) = {
    val nullArgs = definition.parameters.map(_ => Argument("", UnknownArgument))
    val nullNode = new ConfigNode(definition.copy(name = "unknown"), nullArgs, Source("-", 0, 0))
    DefaultDirective(nullNode)
  }
}


/** Helpers for working with directives */
private[flexiconf] object DefaultDirective {
   val unknown = new DefaultDirective(ConfigNode(DirectiveDefinition.unknown, List.empty, Source("unknown", 0, 0)))

  /** Return a DefaultDirective given the provided name or path */
  def getDirective(directives: List[DefaultDirective], name: String): Option[DefaultDirective] = {
    directives.find(_.name == name)
  }

  /** Return all DefaultDirectives given a provided name */
  def getDirectives(directives: List[DefaultDirective], name: String): List[DefaultDirective] = {
    directives.filter(_.name == name)
  }

  /** Return an option containing the given argument value */
  def getArg[T](d: DefaultDirective, argPath: String): Option[ArgumentValue] = {
    d.args.find(_.name == argPath).map(_.value)
  }

  type IllegalStateExceptionGenerator = (String, Set[String], Set[String]) => Throwable

  def entityNotAllowed(singular: String, plural: String, warning: String, complement: String): IllegalStateExceptionGenerator = {
    (name: String, allowedDirectives: Set[String], illegalDirectives: Set[String]) => {
      val illegal = illegalDirectives.mkString("', '")
      val subject = if (illegalDirectives.size > 1) plural else singular

      val allowedMessage = if (allowedDirectives.size > 0) {
        val allowed = allowedDirectives.mkString("', '")
        val verb = if (allowedDirectives.size > 1) "are" else "is"
        s": only '$allowed' $verb $complement"
      } else {
        ""
      }

      new IllegalStateException(s"$subject '$illegal' $warning $name" ++ allowedMessage)
    }
  }

  def directiveNotAllowed = entityNotAllowed("Directive", "Directives", "not allowed in", "allowed")

  def argumentNotAllowed = entityNotAllowed("Argument", "Arguments", "not defined for", "defined")
}

/** Default implementation of a directive */
private[flexiconf] case class DefaultDirective(private val node: ConfigNode) extends Directive {
  import DefaultDirective._

  private lazy val _directives = node.children.map(new DefaultDirective(_))
  private lazy val allowedDirectives = node.allowedDirectives.map(_.name)
  private lazy val allowedArguments = node.allowedArguments.map(_.name).toSet

  override def name: String = node.name

  override def args: List[Argument] = node.arguments

  override def contains(name: String) = directives.exists(_.name == name)
  override def containsArg(name: String) = args.exists(_.name == name)

  override def allows(name: String) = allowedDirectives.contains(name)
  override def allowsArg(name: String) = allowedArguments.contains(name)

  override def directive(name: String): Directive = {
    if (allowedDirectives.contains(name)) {
      getDirective(_directives, name) getOrElse {
        NullDirective(node.directive.children.find(_.name == name).get)
      }
    } else {
      throw directiveNotAllowed(this.name, allowedDirectives, Set(name))
    }
  }

  override def directives: List[Directive] = _directives

  override def directives(names: String*): List[Directive] = {
    val missing = names.toSet &~ allowedDirectives
    if (missing.size == 0) {
      names.flatMap(getDirectives(_directives, _)).toList
    } else {
      throw directiveNotAllowed(this.name, allowedDirectives, missing)
    }
  }

  override def warnings: List[String] = node.warnings

  override def apply: ArgumentValue = argValue(0)

  override def argValue(idx: Int): ArgumentValue = {
    args.lift(idx).map(_.value).getOrElse(NullValue)
  }

  override def argValue(name: String): ArgumentValue = {
    if (allowedArguments.contains(name)) {
      getArg(this, name).getOrElse(NullValue)
    } else {
      throw argumentNotAllowed(this.name, allowedArguments, Set(name))
    }
  }
}
