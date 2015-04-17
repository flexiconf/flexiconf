package se.blea.flexiconf.javaapi

import scala.collection.JavaConversions._


/** Java-friendly wrapper for the ConfigNode API */
class Directive(private val _directive: se.blea.flexiconf.Directive) {
  def getName: String = _directive.name

  def getArgs: java.util.List[Argument] = _directive.args.map(new Argument(_))

  def getDirectives: java.util.List[Directive] = _directive.directives.map(new Directive(_))

  @annotation.varargs
  def getDirectives(names: String*): java.util.List[Directive] = _directive.directives
    .filter( d => names.contains(d.name) )
    .map(new Directive(_))

  def getDirective(name: String): Directive = _directive.directives
    .find(_.name == name)
    .map(new Directive(_))
    .orNull

  def getBoolArg(name: String): java.lang.Boolean = _directive.boolArg(name)
  def getPercentageArg(name: String): java.lang.Double = _directive.percentageArg(name)
  def getDecimalArg(name: String): java.lang.Double = _directive.decimalArg(name)
  def getStringArg(name: String): java.lang.String = _directive.stringArg(name)
  def getIntArg(name: String): java.lang.Long = _directive.intArg(name)
  def getDurationArg(name: String): java.lang.Long = _directive.durationArg(name)
}
