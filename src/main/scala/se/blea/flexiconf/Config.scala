package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.ConfigBaseVisitor
import se.blea.flexiconf.parser.gen.ConfigParser._

import scala.collection.JavaConversions._


/** Public API for accessing parsed configs */
trait Config {
  def directives: List[Directive]
  def warnings: List[String]
  def renderTree: String
}


/** Container for a configuration tree */
case class DefaultConfig(private val config: ConfigNode) extends Config {
  private lazy val collapsedConfig = config.collapse

  override def directives = collapsedConfig.children.map(new DefaultDirective(_))
  override def warnings = config.warnings
  override def renderTree = collapsedConfig.children.map(_.renderTree()).mkString("")
}




/** Options for the ConfigVisitor */
private[flexiconf] case class ConfigVisitorOptions(sourceFile: String,
                                                   allowUnknownDirectives: Boolean = false,
                                                   allowDuplicateDirectives: Boolean = false,
                                                   allowMissingGroups: Boolean = false,
                                                   allowMissingIncludes: Boolean = false,
                                                   allowIncludeCycles: Boolean = false,
                                                   directives: Set[DirectiveDefinition] = Set.empty)

/** Not all data can be carried on the stack, so we have an additional context object that carries this state */
private[flexiconf] case class ConfigNodeVisitorContext(var groupsByNode: Map[ConfigNode, Map[String, DirectiveListContext]] = Map.empty,
                                                    var directivesByNode: Map[ConfigNode, Set[DirectiveDefinition]] = Map.empty) {

  def addGroup(frame: ConfigNode, name: String, directives: DirectiveListContext): Unit = {
    val combinedGroups = groupsByNode.get(frame) map { gs =>
      gs + (name -> directives)
    } getOrElse {
      Map(name -> directives)
    }

    groupsByNode = groupsByNode + (frame -> combinedGroups)
  }

  def addDirective(frame: ConfigNode, directive: DirectiveDefinition): Unit = {
    val combinedDirectives = directivesByNode.get(frame) map { ds =>
      ds + directive
    } getOrElse {
      Set(directive)
    }

    directivesByNode = directivesByNode + (frame -> combinedDirectives)
  }
}


/** Companion for ConfigNodeVisitor */
private[flexiconf] object ConfigNodeVisitor {
  def apply(opts: ConfigVisitorOptions,
            stack: Stack[ConfigNode] = Stack.empty,
            context: ConfigNodeVisitorContext = ConfigNodeVisitorContext()) = new ConfigNodeVisitor(opts, stack, context)
}


/** Converts ASTs from ANTLR into usable configuration trees */
private[flexiconf] class ConfigNodeVisitor(options: ConfigVisitorOptions,
                                           stack: Stack[ConfigNode] = Stack.empty,
                                           context: ConfigNodeVisitorContext = ConfigNodeVisitorContext())
  extends ConfigBaseVisitor[Option[ConfigNode]] {

  /** Entry point for a configuration file */
  override def visitDocument(ctx: DocumentContext): Option[ConfigNode] = {
    val root = stack.peek.map(_.directive) getOrElse DirectiveDefinition.root(options.directives)
    val arguments = stack.peek.map(_.arguments).getOrElse(List.empty)
    val document = ConfigNode(root, arguments, sourceFromContext(ctx))

    stack.enterFrame(document) {
      Some(document.copy(children = visitDirectives(ctx.directiveList())))
    }
  }

  /** Resolves all included files and parses them, adding new directives to the configuration tree */
  override def visitInclude(ctx: IncludeContext): Option[ConfigNode] = {
    val pattern = ctx.stringArgument.getText
    val allowedDirectives = stack.peek.map(_.allowedDirectives).getOrElse(Set.empty)
    val inputStream = Parser.streamFromSourceFile(pattern)
    val parser = Parser.antlrConfigParserFromStream(inputStream)
    val node = ConfigNode(DirectiveDefinition.include(allowedDirectives), ArgumentVisitor(ctx.stringArgument), sourceFromContext(ctx))

    stack.enterFrame(node) {
      ConfigNodeVisitor(options.copy(sourceFile = pattern), stack, context).visitDocument(parser.document()) map { list =>
        node.copy(arguments = List(Argument(pattern)), children = list.children)
      }
    }
  }

  /** Saves the directives referenced within this group on the stack for lookup later */
  override def visitGroup(ctx: GroupContext): Option[ConfigNode] = {
    val name = ctx.stringArgument.getText
    val directives = ctx.directiveList

    addGroup(name, directives)

    Some(ConfigNode(DirectiveDefinition.group,
      ArgumentVisitor(ctx.stringArgument),
      sourceFromContext(ctx)))
  }

  /** Inserts the directive list specified by the given group if it exists */
  override def visitUse(ctx: UseContext): Option[ConfigNode] = {
    val name = ctx.stringArgument.getText
    val allowedDirectives = stack.peek.map(_.allowedDirectives).getOrElse(Set.empty)
    val use = ConfigNode(DirectiveDefinition.use(allowedDirectives),
      ArgumentVisitor(ctx.stringArgument),
      sourceFromContext(ctx))

    findGroup(name).map { ctx =>
      use.copy(children = visitDirectives(ctx))
    } orElse {
      val source = sourceFromContext(ctx)
      val reason = s"Unknown group: $name"

      if (options.allowMissingGroups) {
        Some(use.copy(children = List(ConfigNode(DirectiveDefinition.warning, List(Argument(reason)), source))))
      } else {
        throw new IllegalStateException(s"$reason at $source")
      }
    }
  }


  /** Searches and returns a config node if a matching directive can be found */
  override def visitUserDirective(ctx: UserDirectiveContext): Option[ConfigNode] = {
    val name = ctx.directiveName.getText
    val source = sourceFromContext(ctx)
    val arguments = ArgumentVisitor(ctx.argumentList)
    val allowedDirectives = stack.peek.map(_.allowedDirectives).getOrElse(Set.empty)
    val maybeDirective = MaybeDirective(name, arguments, hasBlock = ctx.directiveList != null)

    // Determine if the directive can be matched
    DirectiveDefinition.find(maybeDirective, allowedDirectives) map { directive =>
      val namedArguments = arguments.zip(directive.parameters) map { pair =>
        pair._1.copy(name = pair._2.name)
      }

      val node = ConfigNode(directive, namedArguments, sourceFromContext(ctx))

      // Check for duplicates
      if (directive.allowOnce && directiveAlreadyExists(directive)) {
        val reason = s"Duplicate directive: $directive"

        if (options.allowDuplicateDirectives) {
          return Some(ConfigNode(DirectiveDefinition.warning, List(Argument(reason)), source))
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
        Some(ConfigNode(DirectiveDefinition.warning, List(Argument(reason)), source))
      } else {
        throw new IllegalStateException(s"$reason at $source")
      }
    }
  }

  /** Returns list of nodes from a context containing possible directives */
  def visitDirectives(ctx: ParserRuleContext): List[ConfigNode] = ctx match {
    case ctx: DirectiveListContext => (ctx.directive flatMap visitDirective).toList
    case _ => List.empty
  }

  /** Associate a named group of directives with the closest, non-internal node or the root node */
  def addGroup(name: String, directives: DirectiveListContext) = {
    context.addGroup(stack.find(_.isUserNode).get, name, directives)
  }

  /** Associate a directive with the closest, non-internal node or the root node */
  def addDirective(directive: DirectiveDefinition) = {
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
  def directiveAlreadyExists(directive: DirectiveDefinition): Boolean = {
    stack.find(_.isUserNode).exists { f =>
      context.directivesByNode.get(f).exists(_.contains(directive))
    }
  }

  /** Returns a new Source object based on the provided context */
  def sourceFromContext(ctx: ParserRuleContext): Source = {
    Source.fromContext(options.sourceFile, ctx)
  }
}


/**
 * Tree node containing a directive and arguments that satisfy it
 */
private[flexiconf] case class ConfigNode(private[flexiconf] val directive: DirectiveDefinition,
                                         private[flexiconf] val arguments: List[Argument],
                                         private[flexiconf] val source: Source,
                                         children: List[ConfigNode] = List.empty) {

  if (arguments.map(_.kind) != directive.parameters.map(_.kind)) {
    throw new IllegalStateException(s"Argument count and types must match parameter count and types: " +
      s"expected ${directive.parameters.mkString(" ")} but got ${arguments.mkString(" ")}")
  }

  /** The node is named after the directive */
  val name = directive.name

  /** Return allowed directives within this scope */
  val allowedDirectives = directive.children

  /** True if the node is for an built-in directive */
  val isInternalNode = name.startsWith("$")

  /** True if the node is a root node */
  val isRootNode = name == "$root"

  /** True if the node is not internal, or is a root node - the root node is considered an internal and user node
    * so directives and groups at the top-most level of the configuration can be associated with a node */
  val isUserNode = !isInternalNode || isRootNode

  /** Collect all warnings for this tree */
  def warnings: List[String] = {
    children.flatMap { node =>
      if (node.directive == DirectiveDefinition.warning) {
        val msg = node.arguments(0).value
        List(s"$msg at $source")
      } else {
        node.warnings
      }
    }
  }

  /** Collapse the tree and remove all internal nodes */
  def collapse: ConfigNode = {
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
