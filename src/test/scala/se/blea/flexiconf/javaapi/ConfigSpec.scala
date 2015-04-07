package se.blea.flexiconf.javaapi

import org.scalatest.{FlatSpec, Matchers}
import se.blea.flexiconf
import se.blea.flexiconf.{ConfigNode, DirectiveDefinition, Source}

class ConfigSpec extends FlatSpec with Matchers {
  val d1 = DirectiveDefinition.withName("foo").build
  val d2 = DirectiveDefinition.withName("bar").build
  val d3 = DirectiveDefinition.withName("baz").build
  val root = DirectiveDefinition.root(d1, d2, d3)

  val node1 = ConfigNode(d1, List.empty, Source("-", 0, 0))
  val node2 = ConfigNode(d2, List.empty, Source("-", 0, 0))
  val node3 = ConfigNode(d3, List.empty, Source("-", 0, 0))
  val rootNode = ConfigNode(root, List.empty, Source("-", 0, 0))
    .copy(children = List(node1, node2, node3))

  val config = new Config(new flexiconf.DefaultConfig(rootNode))


  behavior of "getDirectives"

  it should "return java.util.List" in {
    config.getDirectives.isInstanceOf[java.util.List[flexiconf.javaapi.Directive]] shouldBe true
  }

  it should "return wrapped directives" in {
    config.getDirectives.size() shouldEqual 3
    config.getDirectives.get(0).isInstanceOf[flexiconf.javaapi.Directive] shouldBe true
  }

  it should "return filtered directives in order" in {
    config.getDirectives("foo", "baz").size() shouldEqual 2
    config.getDirectives("foo", "baz").get(0).getName shouldEqual "foo"
    config.getDirectives("foo", "baz").get(1).getName shouldEqual "baz"
  }


  behavior of "getDirective"

  it should "return a wrapped directive" in {
    config.getDirective("foo").isInstanceOf[flexiconf.javaapi.Directive] shouldBe true
  }

  it should "return a single directive" in {
    config.getDirective("foo").getName shouldEqual "foo"
  }

  it should "return null if the directive doesn't exist" in {
    config.getDirective("qux") shouldBe null
  }
}
