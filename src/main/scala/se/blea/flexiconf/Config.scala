package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.ConfigBaseVisitor
import se.blea.flexiconf.parser.gen.ConfigParser._

import scala.collection.JavaConversions._

/**
 *
 * def directives(name: String): Map[String, List[Node]]
 * def get(name: String): Argument
 *
 * Map[String, AnyRef] directives
 * Map[String, AnyRef] defaults = directives.get("defaults").directives.get("compression")
 * List[Map[String, AnyRef]] defaults = directives.getAll("defaults")
 *
 * Boolean compression = defaults.get("compression").get("enabled")
 * List[Boolean] compression = defaults.getAll("compression") map (_.get("enabled"))
 *
 */

/** Container for a configuration tree */
case class Config(private val rawConfig: DefaultConfigNode) extends ConfigNode {
  private lazy val warnings = rawConfig.getWarnings
  private lazy val config = rawConfig.collapse

  override def exists(name: String): Boolean = config.exists(name)
  override def get(name: String): ConfigNode = config.get(name)
  override def getAll(name: String): List[ConfigNode] = config.getAll(name)
  override def getIntArg(name: String): Long = config.getIntArg(name)
  override def getStringArg(name: String): String = config.getStringArg(name)
  override def getBoolArg(name: String): Boolean = config.getBoolArg(name)
  override def getDecimalArg(name: String): Double = config.getDecimalArg(name)
  override def getDurationArg(name: String): Long = config.getDurationArg(name)
  override def getPercentageArg(name: String): Double = config.getPercentageArg(name)
  override def getWarnings: List[String] = warnings

  def renderTree = config.renderTree()
}


private[flexiconf] object ConfigNodeVisitor {
  def apply(opts: ConfigVisitorOptions,
            stack: Stack[DefaultConfigNode] = Stack.empty,
            context: ConfigNodeVisitorContext = ConfigNodeVisitorContext()) = new ConfigNodeVisitor(opts, stack, context)
}


private[flexiconf] case class ConfigVisitorOptions(sourceFile: String,
                                                allowUnknownDirectives: Boolean = false,
                                                allowDuplicateDirectives: Boolean = false,
                                                allowMissingGroups: Boolean = false,
                                                allowMissingIncludes: Boolean = false,
                                                allowIncludeCycles: Boolean = false,
                                                directives: Set[Directive] = Set.empty)

/** Not all data can be carried on the stack, so we have an additional context object that carries this state */
private[flexiconf] case class ConfigNodeVisitorContext(var groupsByNode: Map[DefaultConfigNode, Map[String, DirectiveListContext]] = Map.empty,
                                                    var directivesByNode: Map[DefaultConfigNode, Set[Directive]] = Map.empty) {

  def addGroup(frame: DefaultConfigNode, name: String, directives: DirectiveListContext): Unit = {
    val combinedGroups = groupsByNode.get(frame) map { gs =>
      gs + (name -> directives)
    } getOrElse {
      Map(name -> directives)
    }

    groupsByNode = groupsByNode + (frame -> combinedGroups)
  }

  def addDirective(frame: DefaultConfigNode, directive: Directive): Unit = {
    val combinedDirectives = directivesByNode.get(frame) map { ds =>
      ds + directive
    } getOrElse {
      Set(directive)
    }

    directivesByNode = directivesByNode + (frame -> combinedDirectives)
  }
}

/** Converts ASTs from ANTLR into usable configuration trees */
private[flexiconf] class ConfigNodeVisitor(options: ConfigVisitorOptions,
                                        stack: Stack[DefaultConfigNode] = Stack.empty,
                                        context: ConfigNodeVisitorContext = ConfigNodeVisitorContext())
  extends ConfigBaseVisitor[Option[DefaultConfigNode]] {

  /** Entry point for a configuration file */
  override def visitDocument(ctx: DocumentContext): Option[DefaultConfigNode] = {
    val root = stack.peek.map(_.directive) getOrElse Directive.root(options.directives)
    val arguments = stack.peek.map(_.arguments).getOrElse(List.empty)
    val document = DefaultConfigNode(root, arguments, sourceFromContext(ctx))

    stack.enterFrame(document) {
      Some(document.copy(children = visitDirectives(ctx.directiveList())))
    }
  }

  /** Resolves all included files and parses them, adding new directives to the configuration tree */
  override def visitInclude(ctx: IncludeContext): Option[DefaultConfigNode] = {
    val pattern = ctx.stringArgument.getText
    val allowedDirectives = stack.peek.map(_.allowedDirectives).getOrElse(Set.empty)
    val inputStream = Parser.streamFromSourceFile(pattern)
    val parser = Parser.antlrConfigParserFromStream(inputStream)
    val node = DefaultConfigNode(Directive.include(allowedDirectives), ArgumentVisitor(ctx.stringArgument), sourceFromContext(ctx))

    stack.enterFrame(node) {
      ConfigNodeVisitor(options.copy(sourceFile = pattern), stack, context).visitDocument(parser.document()) map { list =>
        node.copy(arguments = List(Argument(pattern)), children = list.children)
      }
    }
  }

  /** Saves the directives referenced within this group on the stack for lookup later */
  override def visitGroup(ctx: GroupContext): Option[DefaultConfigNode] = {
    val name = ctx.stringArgument.getText
    val directives = ctx.directiveList

    addGroup(name, directives)

    Some(DefaultConfigNode(Directive.group,
      ArgumentVisitor(ctx.stringArgument),
      sourceFromContext(ctx)))
  }

  /** Inserts the directive list specified by the given group if it exists */
  override def visitUse(ctx: UseContext): Option[DefaultConfigNode] = {
    val name = ctx.stringArgument.getText
    val allowedDirectives = stack.peek.map(_.allowedDirectives).getOrElse(Set.empty)
    val use = DefaultConfigNode(Directive.use(allowedDirectives),
      ArgumentVisitor(ctx.stringArgument),
      sourceFromContext(ctx))

    findGroup(name).map { ctx =>
      use.copy(children = visitDirectives(ctx))
    } orElse {
      val source = sourceFromContext(ctx)
      val reason = s"Unknown group: $name"

      if (options.allowMissingGroups) {
        Some(use.copy(children = List(DefaultConfigNode(Directive.warning, List(Argument(reason)), source))))
      } else {
        throw new IllegalStateException(s"$reason at $source")
      }
    }
  }


  /** Searches and returns a config node if a matching directive can be found */
  override def visitUserDirective(ctx: UserDirectiveContext): Option[DefaultConfigNode] = {
    val name = ctx.directiveName.getText
    val source = sourceFromContext(ctx)
    val arguments = ArgumentVisitor(ctx.argumentList)
    val allowedDirectives = stack.peek.map(_.allowedDirectives).getOrElse(Set.empty)
    val maybeDirective = MaybeDirective(name, arguments, hasBlock = ctx.directiveList != null)

    // Determine if the directive can be matched
    Directive.find(maybeDirective, allowedDirectives) map { directive =>
      val node = DefaultConfigNode(directive, arguments, sourceFromContext(ctx))

      // Check for duplicates
      if (directive.allowOnce && directiveAlreadyExists(directive)) {
        val reason = s"Duplicate directive: $directive"

        if (options.allowDuplicateDirectives) {
          return Some(DefaultConfigNode(Directive.warning, List(Argument(reason)), source))
        } else {
          throw new IllegalStateException(s"$reason at $source")
        }
      }

      // Record the directive so we can check for duplicates when parsing additional directives
      addDirective(directive)

      // Enter a new frame and parse all children (if any)
      stack.enterFrame(node) {
        node.copy(children = visitDirectives(ctx.directiveList()))
      }
    } orElse {
      val reason = s"Unknown directive: $maybeDirective"

      if (options.allowUnknownDirectives) {
        Some(DefaultConfigNode(Directive.warning, List(Argument(reason)), source))
      } else {
        throw new IllegalStateException(s"$reason at $source")
      }
    }
  }

  /** Returns list of nodes from a context containing possible directives */
  def visitDirectives(ctx: ParserRuleContext): List[DefaultConfigNode] = ctx match {
    case ctx: DirectiveListContext => (ctx.directive flatMap visitDirective).toList
    case _ => List.empty
  }

  /** Associate a named group of directives with the closest, non-internal node or the root node */
  def addGroup(name: String, directives: DirectiveListContext) = {
    context.addGroup(stack.find(_.isUserNode).get, name, directives)
  }

  /** Associate a directive with the closest, non-internal node or the root node */
  def addDirective(directive: Directive) = {
    context.addDirective(stack.find(_.isUserNode).get, directive)
  }

  /** Finds a saved group of directives in the current stack */
  def findGroup(name: String): Option[DirectiveListContext] = {
    stack.filter(_.isUserNode).find { f =>
      context.groupsByNode.get(f).exists(_.contains(name))
    } flatMap { f =>
      context.groupsByNode(f).get(name)
    }
  }

  /** Returns true if the directives associated with the nearest non-internal or root node */
  def directiveAlreadyExists(directive: Directive): Boolean = {
    stack.find(_.isUserNode).exists { f =>
      context.directivesByNode.get(f).exists(_.contains(directive))
    }
  }

  /** Returns a new Source object based on the provided context */
  def sourceFromContext(ctx: ParserRuleContext): Source = {
    Source.fromContext(options.sourceFile, ctx)
  }
}


/** Container class for argument/parameter pairs */
case class ArgumentParameterPair(argument: Argument, parameter: Parameter)


/** Public interface for consuming configuration */
trait ConfigNode {
  def exists(name: String): Boolean
  def get(name: String): ConfigNode
  def getAll(name: String): List[ConfigNode]
  def getIntArg(name: String): Long
  def getStringArg(name: String): String
  def getBoolArg(name: String): Boolean
  def getDecimalArg(name: String): Double
  def getDurationArg(name: String): Long
  def getPercentageArg(name: String): Double
  def getWarnings: List[String]
}


/**
 * Tree node containing a directive and arguments that satisfy it
 */
private[flexiconf] case class DefaultConfigNode(private[flexiconf] val directive: Directive,
                                             private[flexiconf] val arguments: List[Argument],
                                             private[flexiconf] val source: Source,
                                             private[flexiconf] val children: List[DefaultConfigNode] = List.empty) extends ConfigNode {

  private val argumentKinds = arguments.map(_.kind)
  private val parameterKinds = directive.parameters.map(_.kind)

  if (argumentKinds != parameterKinds) {
    throw new IllegalStateException(s"Argument count and types must match parameter count and types: " +
      s"expected ${directive.parameters.mkString(" ")} but got ${arguments.mkString(" ")}")
  }

  private lazy val parametersAndArgs = (directive.parameters zip arguments).map({ pair =>
    pair._1.name -> ArgumentParameterPair(pair._2, pair._1)
  }).toMap

  lazy val directives = children.groupBy(_.name)

  // ---

  /** Return the node matching the provided name in this scope */
  override def get(name: String): ConfigNode = {
    directives.get(name).map(_.apply(0)) getOrElse { null }
  }

  /** Check for the existance of a node in this scope **/
  override def exists(name: String): Boolean = directives.contains(name)

  /** Return all nodes matching the provided name in this scope */
  override def getAll(name: String): List[ConfigNode] = directives.getOrElse(name, List.empty)

  /** Get int value of argument **/
  override def getIntArg(argName: String): Long = getArg(argName, IntArgument)

  /** Get string value of argument **/
  override def getStringArg(argName: String): String = getArg(argName, StringArgument)

  /** Get boolean value of argument **/
  override def getBoolArg(argName: String): Boolean = getArg(argName, BoolArgument)

  /** Get decimal value of argument **/
  override def getDecimalArg(argName: String): Double = getArg(argName, DecimalArgument)

  /** Get duration value of argument **/
  override def getDurationArg(argName: String): Long = getArg(argName, DurationArgument)

  /** Get percentage value of argument **/
  override def getPercentageArg(argName: String): Double = getArg(argName, PercentageArgument)

  /** Get all warnings */
  override def getWarnings: List[String] = {
    children.flatMap { node =>
      if (node.directive == Directive.warning) {
        val msg = node.getStringArg("message")
        List(s"$msg at $source")
      } else {
        node.getWarnings
      }
    }
  }

  // ---

  /** Return the argument value if it exists, throws exception otherwise */
  private def getArg[T](argName: String, kind: ArgumentType[T]): T = {
    // throw exception if the directive accepts no argument
    if (parametersAndArgs.size == 0) {
      throw new IllegalStateException(s"Unknown argument: $argName - directive '$name' accepts no arguments")
    }

    // Get argument value or throw exception if the named argument isn't valid for this directive
    argument(argName).map(kind.valueOf) getOrElse {
      val validArgs = directive.parameters.mkString(" ")
      throw new IllegalStateException(s"Unknown argument: $argName - valid arguments for directive '$name': $validArgs")
    }
  }

  /** The node is named after the directive */
  val name = directive.name

  /** True if the node is for an built-in directive */
  val isInternalNode = name.startsWith("$")

  /** True if the node is a root node */
  val isRootNode = name == "$root"

  /** True if the node is not internal, or is a root node - the root node is considered an internal and user node
    * so directives and groups at the top-most level of the configuration can be associated with a node */
  val isUserNode = !isInternalNode || isRootNode

  /** Return argument value by name */
  def argument(name: String) = parametersAndArgs.get(name).map(_.argument.value)

  /** Return allowed directives within this scope */
  val allowedDirectives = directive.children

  /** Collapse the tree and remove all internal nodes */
  def collapse: DefaultConfigNode = {
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
