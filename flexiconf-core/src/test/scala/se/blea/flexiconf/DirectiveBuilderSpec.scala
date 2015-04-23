package se.blea.flexiconf

import org.scalatest.{Matchers, FlatSpec}

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
