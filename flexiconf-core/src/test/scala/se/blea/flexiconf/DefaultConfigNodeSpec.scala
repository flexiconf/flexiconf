package se.blea.flexiconf

import org.scalatest.{Matchers, FlatSpec}

/** Test cases for config parsing */
class DefaultConfigNodeSpec extends FlatSpec with Matchers with ConfigHelpers {

  it should "prevent mismatching arguments for parameters" in {
    intercept[IllegalStateException] {
      val d = DirectiveDefinition.withName("foo")
        .withIntArg("val")
        .withIntArg("bar")
        .build

      ConfigNode(d, List(StringArgument("123")), Source("test", 1, 0))
    }
  }

  it should "take the name of the provided directive" in {
    assert(node("foobar").name == "foobar")
  }

  it should "identify whether the node is for a built-in directive" in {
    val node1 = node("foobar")
    val node2 = node(DirectiveDefinition.root())
    val node3 = node(DirectiveDefinition.withUnsafeName("$foo").build)

    assert(!node1.isInternalNode)
    assert(node2.isInternalNode)
    assert(node3.isInternalNode)
  }

  it should "identify whether the node is for a root directive" in {
    val node1 = node("foobar")
    val node2 = node(DirectiveDefinition.root())
    val node3 = node(DirectiveDefinition.withUnsafeName("$foo").build)

    assert(!node1.isRootNode)
    assert(node2.isRootNode)
    assert(!node3.isRootNode)
  }

  it should "identify whether the node is for a user directive" in {
    val node1 = node("foobar")
    val node2 = node(DirectiveDefinition.root())
    val node3 = node(DirectiveDefinition.withUnsafeName("$foo").build)

    assert(node1.isUserNode)
    assert(node2.isUserNode)
    assert(!node3.isUserNode)
  }
}
