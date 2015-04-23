package se.blea.flexiconf


/** Container for a configuration tree */
case class DefaultConfig(private val config: ConfigNode) extends Config {
  private lazy val collapsedConfig = config.collapse

  override def directives = collapsedConfig.children.map(new DefaultDirective(_))
  override def warnings = config.warnings
  override def renderTree = collapsedConfig.children.map(_.renderTree()).mkString("")
}
