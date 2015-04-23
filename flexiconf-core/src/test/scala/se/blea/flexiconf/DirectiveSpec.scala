package se.blea.flexiconf

import org.scalatest.{FlatSpec, Matchers}

/** Test cases for Directive */
class DirectiveSpec extends FlatSpec with Matchers {

  behavior of "MaybeDirective"

  it should "match a node by name" in {
    val d = DirectiveDefinition.withName("test_node").build

    assert(MaybeDirective("test_node") matches d)
    assert(MaybeDirective("node_test") doesNotMatch d)
  }

  it should "match a node by name and argument type" in {
    val d = DirectiveDefinition.withName("test_node")
      .withStringArg("val1")
      .withDecimalArg("val2")
      .build

    val args = Seq(
      StringArgument("any"),
      DecimalArgument("0.0001")
    )

    val badArgs1 = Seq(
      StringArgument("any"),
      StringArgument("0.0001")
    )

    val badArgs2 = Seq(
      StringArgument("any"),
      DecimalArgument("0.0001"),
      BoolArgument("false")
    )

    val badArgs3 = Seq(
      StringArgument("any")
    )

    assert(MaybeDirective("test_node", args) matches d)
    assert(MaybeDirective("test_node", badArgs1) doesNotMatch d)
    assert(MaybeDirective("test_node", badArgs2) doesNotMatch d)
    assert(MaybeDirective("test_node", badArgs3) doesNotMatch d)
    assert(MaybeDirective("test_node") doesNotMatch d)
  }

  it should "match a node by name, args, and block allowance" in {
    val d1 = DirectiveDefinition.withName("without_block")
      .withIntArg("val1")
      .build

    val d2 = DirectiveDefinition.withName("with_block")
      .withBoolArg("val1")
      .withDirectives(d1)
      .build

    val args1 = Seq(IntArgument("123"))
    val args2 = Seq(BoolArgument("true"))

    assert(MaybeDirective("without_block", args1, hasBlock = false) matches d1)
    assert(MaybeDirective("with_block", args2, hasBlock = true) matches d2)
    assert(MaybeDirective("without_block", args1, hasBlock = true) doesNotMatch d1)
    assert(MaybeDirective("with_block", args2, hasBlock = false) doesNotMatch d2)
  }

  it should "require a block with children directives" in {
    val d1 = DirectiveDefinition.withName("without_block")
      .withIntArg("val1")
      .build

    val d2 = DirectiveDefinition.withName("with_block")
      .withBoolArg("val1")
      .withDirectives(d1)
      .build

    assert(!d1.requiresBlock)
    assert(d2.requiresBlock)
  }
}



