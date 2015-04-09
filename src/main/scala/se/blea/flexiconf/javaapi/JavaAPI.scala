package se.blea.flexiconf.javaapi

import scala.collection.JavaConversions._

import se.blea.flexiconf._

/** Java-friendly wrapper for the Parser API */
object Parser {
  def parseConfig(opts: ConfigOptions): Config = {
    se.blea.flexiconf.Parser.parseConfig(opts)
      .map(new Config(_))
      .orNull
  }

  def parseSchema(opts: SchemaOptions): Schema = {
    se.blea.flexiconf.Parser.parseSchema(opts).orNull
  }
}

/** Java-friendly wrapper for the Config API */
class Config(private val _config: se.blea.flexiconf.Config) {
  def getDirectives: java.util.List[Directive] = _config.directives.map(new Directive(_))

  @annotation.varargs
  def getDirectives(names: String*): java.util.List[Directive] = _config.directives
    .filter( d => names.contains(d.name) )
    .map(new Directive(_))

  def getDirective(name: String): Directive = _config.directives
    .find(_.name == name)
    .map(new Directive(_))
    .orNull

  def getWarnings: java.util.List[String] = _config.warnings

  def renderTree = _config.renderTree
}

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

/** Java-friendly wrapper for the Argument API */
class Argument(private val _arg: se.blea.flexiconf.Argument) {
  def getName = _arg.name
  def getKind = _arg.kind match {
    case StringArgument => ArgumentKind.String
    case IntArgument => ArgumentKind.Int
    case BoolArgument => ArgumentKind.Bool
    case DecimalArgument => ArgumentKind.Decimal
    case DurationArgument => ArgumentKind.Duration
    case PercentageArgument => ArgumentKind.Percentage
    case _ => throw new IllegalArgumentException("Unknown argument kind")
  }
}
