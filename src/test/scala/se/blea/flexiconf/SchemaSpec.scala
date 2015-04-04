package se.blea.flexiconf

import java.io.ByteArrayInputStream

import org.antlr.v4.runtime.{CommonTokenStream, ANTLRInputStream}
import org.scalatest.{Matchers, FlatSpec}
import se.blea.flexiconf.parser.gen.{SchemaParser, SchemaLexer}

/** Test cases for Schema */
class SchemaSpec extends FlatSpec with Matchers {
  def schema(inputString: String) = {
    val bytes = inputString.getBytes
    val input = new ANTLRInputStream(new ByteArrayInputStream(bytes))
    val lexer = new SchemaLexer(input)
    val tokens = new CommonTokenStream(lexer)
    val parser = new SchemaParser(tokens)
    val document = parser.document()

    val opts = SchemaVisitorOptions("test")
    val visitor = new SchemaNodeVisitor(opts)

    visitor.visitDocument(document)
  }


  behavior of "#visitDocument"

  it should "return a tree that includes a root node" in {
    val result = schema(
      """
        |foo val:Int;
        |bar val:String;
        |baz val:Decimal [once];
        |qux val:Bool;
      """.stripMargin)

    assert(result.get.name === "$root")

    val children = result.get.children
    assert(children(0).name === "foo")
    assert(children(0).parameters(0).name === "val")
    assert(children(0).parameters(0).kind === IntArgument)

    assert(children(1).name === "bar")
    assert(children(1).parameters(0).name === "val")
    assert(children(1).parameters(0).kind === StringArgument)

    assert(children(2).name === "baz")
    assert(children(2).parameters(0).name === "val")
    assert(children(2).parameters(0).kind === DecimalArgument)

    assert(children(3).name === "qux")
    assert(children(3).parameters(0).name === "val")
    assert(children(3).parameters(0).kind === BoolArgument)
  }
}
