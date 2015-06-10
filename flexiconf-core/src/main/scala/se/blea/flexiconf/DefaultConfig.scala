package se.blea.flexiconf


/** Container for a configuration tree */
private[flexiconf] case class DefaultConfig(private val config: ConfigNode) extends Config {
  import DefaultDirective._

  private lazy val collapsedConfig = config.collapse
  private lazy val collapsedDirectives = collapsedConfig.children.map(new DefaultDirective(_))
  private lazy val allowedDirectives = collapsedConfig.allowedDirectives.map(_.name)

  override def renderTree = collapsedConfig.children.map(_.renderTree()).mkString("")
  override private[flexiconf] def renderDebugTree = config.children.map(_.renderTree()).mkString("")

  override def warnings = config.warnings

  override def directive(name: String): Directive = {
    if (allowedDirectives.contains(name)) {
      getDirective(collapsedDirectives, name) getOrElse {
        NullDirective(config.directive.children.find(_.name == name).get)
      }
    } else {
      throw directiveNotAllowed("top-level of config", allowedDirectives, Set(name))
    }
  }

  override def directives: List[Directive] = collapsedDirectives

  override def directives(names: String*): List[Directive] = {
    val missing = names.toSet &~ allowedDirectives
    if (missing.size == 0) {
      names.flatMap(getDirectives(collapsedDirectives, _)).toList
    } else {
      throw directiveNotAllowed("top-level of config", allowedDirectives, missing)
    }
  }

  override def contains(name: String) = directives.exists(_.name == name)
  override def allows(name: String) = allowedDirectives.contains(name)
}
