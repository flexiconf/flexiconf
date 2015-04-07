package se.blea.flexiconf

import org.antlr.v4.runtime.ParserRuleContext
import se.blea.flexiconf.parser.gen.SchemaParser._
import se.blea.flexiconf.parser.gen.SchemaParserBaseVisitor

import scala.collection.JavaConversions._

trait SchemaNode {
  def name:String
  def parameters: List[Parameter]
  def source: Source
  def flags: Set[DirectiveFlag]
  def documentation: String
  def children: List[SchemaNode]
  def toDirectives: Set[DirectiveDefinition]
}

case class Schema(private val rawSchema: DefaultSchemaNode) extends SchemaNode {
  private lazy val schema = rawSchema.collapse

  override def name: String = schema.name
  override def children: List[SchemaNode] = schema.children
  override def parameters: List[Parameter] = schema.parameters
  override def source: Source = schema.source
  override def documentation: String = schema.documentation
  override def flags: Set[DirectiveFlag] = schema.flags
  override def toDirectives: Set[DirectiveDefinition] = schema.toDirectives
}

private[flexiconf] case class DefaultSchemaNode(name: String,
                                             parameters: List[Parameter],
                                             source: Source,
                                             flags: Set[DirectiveFlag] = Set.empty,
                                             documentation: String = "",
                                             children: List[DefaultSchemaNode] = List.empty) extends SchemaNode {

  // Parameter validation

  // Names must be unique
  val paramNames = parameters.map(_.name)
  if (paramNames.size != paramNames.toSet.size) {
    throw new IllegalStateException(s"Directive '$name' must use a unique name for each parameter at $source")
  }

  // Parameter types must be known
  parameters.foreach { p =>
    if (p.kind == UnknownArgument) {
      throw new IllegalStateException(s"Unknown type for parameter '${p.name}' at $source")
    }
  }

  override def toDirectives: Set[DirectiveDefinition] = {
    children.map(_.toDirective).toSet
  }

  /** Collapse the tree and remove all internal nodes */
  def collapse: DefaultSchemaNode = {
    copy(children = children.flatMap { node =>
      if (node.name.startsWith("$")) {
        node.collapse.children
      } else {
        List(node.collapse)
      }
    })
  }

  /** Convert this SchemaNode to a tree of directives */
  def toDirective: DirectiveDefinition = {
    DirectiveDefinition.withUnsafeName(name)
      .withParameters(parameters)
      .withFlags(flags)
      .withDocumentation(documentation)
      .withDirectives(collapse.children.map(_.toDirective):_*)
      .build
  }

  override def toString = {
    var res = name

    if (parameters.nonEmpty) {
      res ++= (parameters map (_.toString)).mkString(" ", " ", "")
    }

    if (flags != DirectiveFlags()) {
      res ++= s" $flags"
    }

    res ++ s" ($source)"
  }

  /** Return a string representing a configuration tree */
  def renderTree(depth: Int = 0): String = {
    val sb = StringBuilder.newBuilder

    sb ++= ("  " * depth)
    sb ++= s"> $this\n"
    sb ++= children flatMap (_.renderTree(depth + 1))

    sb.mkString
  }
}


case class SchemaVisitorOptions(sourceFile: String,
                                allowMissingGroups: Boolean = false)

/** Not all data can be carried on the stack, so we have an additional context object that carries this state */
private[flexiconf] case class SchemaNodeVisitorContext(var groupsByNode: Map[DefaultSchemaNode, Map[String, DirectiveListContext]] = Map.empty) {

  /** Associate a named group of directives with the closest, non-internal node or the root node */
  def addGroup(node: DefaultSchemaNode, name: String, directives: DirectiveListContext): Unit = {
    val combinedGroups = groupsByNode.get(node) map { gs =>
      gs + (name -> directives)
    } getOrElse {
      Map(name -> directives)
    }

    groupsByNode = groupsByNode + (node -> combinedGroups)
  }
}

private[flexiconf] case class SchemaNodeVisitor(options: SchemaVisitorOptions,
                                             stack: Stack[DefaultSchemaNode] = Stack.empty,
                                             visitorCtx: SchemaNodeVisitorContext = SchemaNodeVisitorContext())
  extends SchemaParserBaseVisitor[Option[DefaultSchemaNode]] {

  /** Entry point for a schema file */
  override def visitDocument(ctx: DocumentContext): Option[DefaultSchemaNode] = {
    val name = stack.peek.map(_.name) getOrElse "$root"
    val params = stack.peek.map(_.parameters) getOrElse List.empty
    val flags = stack.peek.map(_.flags) getOrElse Set.empty
    val document = DefaultSchemaNode(name, params, sourceFromContext(ctx), flags)

    stack.enterFrame(document) {
      Some(document.copy(children = visitDirectives(ctx.directiveList())))
    }
  }

  /** Resolves all included files and parses them, adding new directives to the configuration tree */
  override def visitInclude(ctx: IncludeContext): Option[DefaultSchemaNode] = {
    val pattern = ctx.stringArgument.getText
    val inputStream = Parser.streamFromSourceFile(pattern)
    val parser = Parser.antlrSchemaParserFromStream(inputStream)

    SchemaNodeVisitor(options.copy(sourceFile = pattern), stack).visitDocument(parser.document()) map { list =>
      DefaultSchemaNode(name = "$include",
        parameters = List.empty,
        source = sourceFromContext(ctx),
        children = list.children)
    }
  }

  /** Saves the directives referenced within this group on the stack for lookup later */
  override def visitGroup(ctx: GroupContext): Option[DefaultSchemaNode] = {
    val name = ctx.stringArgument.getText
    val directives = ctx.directiveList

    addGroup(name, directives)

    Some(DefaultSchemaNode("$group",
      List.empty,
      sourceFromContext(ctx)))
  }

  /** Inserts the directive list specified by the given group if it exists */
  override def visitUse(ctx: UseContext): Option[DefaultSchemaNode] = {
    val name = ctx.stringArgument.getText
    val allowedDirectives = stack.peek.getOrElse(Set.empty)
    val use = DefaultSchemaNode("$use", List.empty, sourceFromContext(ctx))

    findGroup(name) map { ctx =>
      use.copy(children = visitDirectives(ctx))
    } orElse {
      val source = sourceFromContext(ctx)
      val reason = s"Unknown group: $name"

      if (options.allowMissingGroups) {
        Some(use.copy(children = List(DefaultSchemaNode("$warning", List(Parameter(reason)), source))))
      } else {
        throw new IllegalStateException(s"$reason at $source")
      }
    }
  }


  /** Searches and returns a config node if a matching directive can be found */
  override def visitUserDirective(ctx: UserDirectiveContext): Option[DefaultSchemaNode] = {
    val name = ctx.directiveName.getText
    val source = sourceFromContext(ctx)
    val parameters = ParameterVisitor(ctx.parameterList())
    val flags = DirectiveFlagListVisitor(ctx.flagList())
    val docs = DocumentationVisitor(ctx.documentationBlock()).mkString("\n")
    val node = DefaultSchemaNode(name, parameters, sourceFromContext(ctx), flags, docs)

    stack.enterFrame(node) {
      Some(node.copy(children = visitDirectives(ctx.directiveList())))
    }
  }


  /** * Returns list of nodes from a context containing possible directives */
  def visitDirectives(ctx: ParserRuleContext): List[DefaultSchemaNode] = ctx match {
    case ctx: DirectiveListContext => (ctx.directive flatMap visitDirective).toList
    case _ => List.empty
  }

  /** Records a group of directives with the given name in the current frame */
  def addGroup(name: String, directives: DirectiveListContext) = {
    visitorCtx.addGroup(stack.peek.get, name, directives)
  }

  /** Finds a group of directives in the current stack */
  def findGroup(name: String): Option[DirectiveListContext] = {
    stack.find { f =>
      visitorCtx.groupsByNode.get(f).exists(_.contains(name))
    } flatMap { f =>
      visitorCtx.groupsByNode(f).get(name)
    }
  }

  /** Returns a new Source object based on the provided context */
  def sourceFromContext(ctx: ParserRuleContext): Source = {
    Source(options.sourceFile, ctx.getStart.getLine, ctx.getStart.getCharPositionInLine)
  }
}

