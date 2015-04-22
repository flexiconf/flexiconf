package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.ConfigBaseVisitor
import se.blea.flexiconf.parser.gen.ConfigParser._

import scala.collection.JavaConversions._


/** Converts ASTs from ANTLR into usable configuration trees */
private[flexiconf] class ConfigVisitor(options: ConfigVisitorOptions,
                                       stack: Stack[ConfigVisitorContext] = Stack.empty)
  extends ConfigBaseVisitor[Option[ConfigNode]] {

  /** Entry point for a configuration file */
  override def visitDocument(ctx: DocumentContext): Option[ConfigNode] = {
    val root = stack.peek.map(_.node.directive) getOrElse DirectiveDefinition.root(options.directives)
    val arguments = stack.peek.map(_.node.arguments).getOrElse(List.empty)
    val document = ConfigNode(root, arguments, sourceFromContext(ctx))

    stack.enterFrame(ConfigVisitorContext(document)) {
      Some(document.copy(children = visitDirectives(ctx.directiveList())))
    }
  }

  /** Resolves all included files and parses them, adding new directives to the configuration tree */
  override def visitInclude(ctx: IncludeContext): Option[ConfigNode] = {
    val pattern = ctx.stringArgument.getText
    val allowedDirectives = stack.peek.map(_.node.allowedDirectives).getOrElse(Set.empty)
    val inputStream = Parser.streamFromSourceFile(pattern)
    val parser = Parser.antlrConfigParserFromStream(inputStream)
    val node = ConfigNode(DirectiveDefinition.include(allowedDirectives), ArgumentVisitor(ctx.stringArgument), sourceFromContext(ctx))

    stack.enterFrame(ConfigVisitorContext(node)) {
      ConfigVisitor(options.copy(sourceFile = pattern), stack).visitDocument(parser.document()) map { list =>
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
    val allowedDirectives = stack.peek.map(_.node.allowedDirectives).getOrElse(Set.empty)
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
    val allowedDirectives = stack.peek.map(_.node.allowedDirectives).getOrElse(Set.empty)
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
      stack.enterFrame(ConfigVisitorContext(node)) {
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
    stack.find(_.node.isUserNode).map(_.withGroup(name, directives))
  }

  /** Associate a directive with the closest, non-internal node or the root node */
  def addDirective(directive: DirectiveDefinition) = {
    stack.find(_.node.isUserNode).map(_.withDirective(directive))
  }

  /** Finds a saved group of directives in the current stack */
  def findGroup(name: String): Option[DirectiveListContext] = {
    stack.find { ctx =>
      ctx.node.isUserNode && ctx.groups.contains(name)
    }.flatMap(_.groups.get(name))
  }

  /** Returns true if the directives associated with the nearest non-internal or root node */
  def directiveAlreadyExists(directive: DirectiveDefinition): Boolean = {
    stack.find(_.node.isUserNode).exists(_.directives.contains(directive))
  }

  /** Returns a new Source object based on the provided context */
  def sourceFromContext(ctx: ParserRuleContext): Source = {
    Source.fromContext(options.sourceFile, ctx)
  }
}

/** Companion for ConfigNodeVisitor */
private[flexiconf] object ConfigVisitor {
  def apply(opts: ConfigVisitorOptions,
            stack: Stack[ConfigVisitorContext] = Stack.empty) = new ConfigVisitor(opts, stack)
}
