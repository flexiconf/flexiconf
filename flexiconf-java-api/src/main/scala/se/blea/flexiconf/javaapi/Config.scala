package se.blea.flexiconf.javaapi

import scala.collection.JavaConversions._


/** Java-friendly wrapper for the Config API */
class Config(private val _config: se.blea.flexiconf.Config) {
  def getDirectives: java.util.List[Directive] = _config.directives.map(new Directive(_))


  def hasDirective(name: String): java.lang.Boolean = _config.directives.exists(_.name == name)
  
  @annotation.varargs
  def getDirectives(names: String*): java.util.List[Directive] = _config.directives
    .filter( d => names.contains(d.name) )
    .map(new Directive(_))

  def getDirective(name: String): Directive = new Directive(_config.directive(name))

  def getWarnings: java.util.List[String] = _config.warnings

  def renderTree = _config.renderTree
}
