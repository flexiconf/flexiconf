package se.blea.flexiconf.javaapi

import scala.collection.JavaConversions._


/** Java-friendly wrapper for the ConfigNode API */
class Directive(private val _directive: se.blea.flexiconf.Directive) {
  def getName: String = _directive.name

  def getArgs: java.util.List[Argument] = _directive.args.map(new Argument(_))

  def getDirectives: java.util.List[Directive] = _directive.directives.map(new Directive(_))

  @annotation.varargs
  def getDirectives(names: String*): java.util.List[Directive] = _directive.directives(names:_*).map(new Directive(_))

  def getDirective(name: String): Directive = new Directive(_directive.directive(name))

  private def boolArg(name: String): Option[Boolean] = _directive.argValue(name).boolValue
  private def longArg(name: String): Option[Long] = _directive.argValue(name).longValue
  private def doubleArg(name: String): Option[Double] = _directive.argValue(name).doubleValue
  private def stringArg(name: String): Option[String] = _directive.argValue(name).stringValue

  def getBoolArg(name: String): java.lang.Boolean = boolArg(name).get
  def getBoolArg(name: String, default: java.lang.Boolean): java.lang.Boolean = boolArg(name).getOrElse[Boolean](default)

  def getPercentageArg(name: String): java.lang.Double = doubleArg(name).get
  def getPercentageArg(name: String, default: java.lang.Double): java.lang.Double = doubleArg(name).getOrElse[Double](default)

  def getDecimalArg(name: String): java.lang.Double = doubleArg(name).get
  def getDecimalArg(name: String, default: java.lang.Double): java.lang.Double = doubleArg(name).getOrElse[Double](default)

  def getStringArg(name: String): java.lang.String = stringArg(name).get
  def getStringArg(name: String, default: java.lang.String): java.lang.String = stringArg(name).getOrElse[String](default)

  def getIntArg(name: String): java.lang.Long = longArg(name).get
  def getIntArg(name: String, default: java.lang.Long): java.lang.Long = longArg(name).getOrElse[Long](default)

  def getDurationArg(name: String): java.lang.Long = longArg(name).get
  def getDurationArg(name: String, default: java.lang.Long): java.lang.Long = longArg(name).getOrElse[Long](default)
}
