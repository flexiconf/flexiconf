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


/** Test cases for Directive.Builder */
class DirectiveBuilderSpec extends FlatSpec with Matchers {
  it should "disallow null names" in {
    intercept[NullPointerException] {
      DirectiveDefinition.withName(null)
    }
  }

  it should "disallow empty names" in {
    intercept[IllegalArgumentException] {
      DirectiveDefinition.withName("")
    }
  }

  it should "disallow names starting with $" in {
    intercept[IllegalArgumentException] {
      DirectiveDefinition.withName("$")
    }
  }

  it should "create a new copy when provided an argument" in {
    val d1 = DirectiveDefinition.withName("foo")
    val d2 = d1.withBoolArg("arg1")

    assert(d1 != d2)
    assert(d1.parameters.size == 0)
    assert(d2.parameters.size == 1)
  }

  it should "create a new copy when provided a directive" in {
    val d1 = DirectiveDefinition.withName("foo")
    val dir = d1.build
    val d2 = d1.withDirectives(dir)

    assert(d1 != d2)
    assert(d1.children.size == 0)
    assert(d2.children.size == 1)
  }

  it should "allow adding int args" in {
    val d = DirectiveDefinition.withName("foo").withIntArg("val").build
    assert(d.parameters(0).name == "val")
    assert(d.parameters(0).kind == IntArgument)
  }

  it should "allow adding bool args" in {
    val d = DirectiveDefinition.withName("foo").withBoolArg("val").build
    assert(d.parameters(0).name == "val")
    assert(d.parameters(0).kind == BoolArgument)
  }

  it should "allow adding string args" in {
    val d = DirectiveDefinition.withName("foo").withStringArg("val").build
    assert(d.parameters(0).name == "val")
    assert(d.parameters(0).kind == StringArgument)
  }

  it should "allow adding decimal args" in {
    val d = DirectiveDefinition.withName("foo").withDecimalArg("val").build
    assert(d.parameters(0).name == "val")
    assert(d.parameters(0).kind == DecimalArgument)
  }

  it should "allow a directive to repeat by default" in {
    val d = DirectiveDefinition.withName("foo").build
    assert(!d.allowOnce)
  }

  it should "allow a directive to not be repeated if only allowed once" in {
    val d = DirectiveDefinition.withName("foo").allowOnce().build
    assert(d.allowOnce)
  }

  it should "disallow arguments with null names" in {
    intercept[NullPointerException] {
      DirectiveDefinition.withName("foo").withBoolArg(null)
    }
  }

  it should "disallow arguments with empty names" in {
    intercept[IllegalArgumentException] {
      DirectiveDefinition.withName("foo").withIntArg("")
    }
  }
}
