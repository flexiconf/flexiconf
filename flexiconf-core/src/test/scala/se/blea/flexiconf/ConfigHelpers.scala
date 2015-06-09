package se.blea.flexiconf

import java.io.ByteArrayInputStream

import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ANTLRInputStream
import se.blea.flexiconf.parser.gen.{ConfigParser, ConfigLexer, SchemaParser, SchemaLexer}

/** Helper methods for working with config parsers */
trait ConfigHelpers {
  def defaultOptions = ConfigOptions.withSourceFile("test")

  def node(d: DirectiveDefinition): ConfigNode = ConfigNode(d, List.empty, Source("", 0, 0))
  def node(n: String): ConfigNode = node(DirectiveDefinition.withName(n).build)

  def rootNode(ds: DirectiveDefinition*): ConfigNode = node(DirectiveDefinition.root(ds:_*))

  def makeStack(node: ConfigNode) = Stack(List(new ConfigVisitorContext(node)))

  def visitor(opts: ConfigOptions): ConfigVisitor = ConfigVisitor(opts.visitorOpts)
  def visitor(opts: ConfigOptions, stack: Stack[ConfigVisitorContext]): ConfigVisitor = ConfigVisitor(opts.visitorOpts, stack)

  def nodeWithSchema(inputString: String) = node(schema(inputString))
  def emptyStackWithSchema(inputString: String) = makeStack(nodeWithSchema(inputString))

  def schema(inputString: String) = {
    val bytes = inputString.getBytes
    val input = new ANTLRInputStream(new ByteArrayInputStream(bytes))
    val lexer = new SchemaLexer(input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new SchemaParser(tokens)
    val document = parser.document()

    val opts = SchemaVisitorOptions("test")
    val visitor = new SchemaVisitor(opts)

    visitor.visitDocument(document).get.toDirective
  }

  def parse(inputString: String) = {
    val bytes = inputString.getBytes
    val input = new ANTLRInputStream(new ByteArrayInputStream(bytes))
    val lexer = new ConfigLexer(input)
    val tokens = new CommonTokenStream(lexer)

    new ConfigParser(tokens)
  }
}
