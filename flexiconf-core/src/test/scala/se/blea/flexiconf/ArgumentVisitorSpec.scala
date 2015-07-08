package se.blea.flexiconf

import org.scalatest.{Matchers, FlatSpec}

/** Test cases for argument parsing */
class ArgumentVisitorSpec extends FlatSpec with Matchers with ConfigHelpers {

  behavior of "#apply"

  it should "return an empty list of arguments when visiting an empty argument lists" in {
    val result = ArgumentVisitor.apply
    assert(result.size == 0)
  }

  it should "return list of arguments when visiting argument lists" in {
    // scalastyle:off magic.number
    val ctx = parse( """1 off "true" 500.2 foo bar 100ms 25% -600""")
    val result = ArgumentVisitor(ctx.argumentList)

    assert(result.size == 9)
    assert(result.forall(_.isInstanceOf[Argument]))

    assert(result(0).kind == IntArgument)
    assert(result(1).kind == BoolArgument)
    assert(result(2).kind == StringArgument)
    assert(result(3).kind == DecimalArgument)
    assert(result(4).kind == StringArgument)
    assert(result(5).kind == StringArgument)
    assert(result(6).kind == DurationArgument)
    assert(result(7).kind == PercentageArgument)
    assert(result(8).kind == IntArgument)
    // scalastyle:on magic.number
  }

  it should "return string arguments for quoted string arguments" in {
    val ctx = parse("\"foo\"").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).originalValue == "foo")
  }

  it should "return string arguments for double quoted string arguments with escaped quotes in them" in {
    val ctx = parse("\"foo bar\\t \\r \\n \\\\ \\\"\"").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).originalValue == "foo bar\\t \\r \\n \\\\ \\\"")
  }

  it should "return string arguments for single quoted string arguments with escaped quotes in them" in {
    val ctx = parse("'foo bar\\t \\r \\n \\\\ \\''").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).originalValue == "foo bar\\t \\r \\n \\\\ \\'")
  }

  it should "return string arguments for quoted string arguments with escaped slashes in them" in {
    val ctx = parse("'foo bar\\t \\r \\n \\\\ \\'\\\\'").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).originalValue == "foo bar\\t \\r \\n \\\\ \\'\\\\")
  }

  it should "return string arguments for unquoted string arguments" in {
    val ctx = parse("foo").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).originalValue == "foo")
  }

  it should "visit quoted booleans and return string arguments" in {
    val result = ArgumentVisitor(parse("\"off\"").argument)
    assert(result(0).kind == StringArgument)
    assert(result(0).originalValue == "off")
  }

  it should "return boolean arguments for boolean values" in {
    val result = ArgumentVisitor(parse("on").argument)
    assert(result(0).kind == BoolArgument)
    assert(result(0).originalValue == "on")
  }

  // scalastyle:off magic.number
  it should "return integer arguments for integer values" in {
    val result = ArgumentVisitor(parse("10001").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (IntArgument, "10001", LongValue(10001))

  }

  it should "return integer arguments for negative integer values" in {
    val result = ArgumentVisitor(parse("-10001").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (IntArgument, "-10001", LongValue(-10001))
  }

  it should "return decimal arguments for decimal values" in {
    val result = ArgumentVisitor(parse("0.300").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (DecimalArgument, "0.300", DoubleValue(0.3))
  }

  it should "return decimal arguments for negative decimal values" in {
    val result = ArgumentVisitor(parse("-0.300").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (DecimalArgument, "-0.300", DoubleValue(-0.3))
  }

  it should "return duration arguments for duration values" in {
    val result = ArgumentVisitor(parse("10s").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (DurationArgument, "10s", LongValue(10*1000))
  }

  it should "return duration arguments for negative duration values" in {
    val result = ArgumentVisitor(parse("-10s").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (DurationArgument, "-10s", LongValue(-1*10*1000))
  }

  it should "return percentage arguments for percentage values" in {
    val result = ArgumentVisitor(parse("10%").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (PercentageArgument, "10%", DoubleValue(0.1))
  }

  it should "return percentage arguments for negative percentage values" in {
    val result = ArgumentVisitor(parse("-10%").argument)(0)
    (result.kind, result.originalValue, result.value) shouldEqual (PercentageArgument, "-10%", DoubleValue(-0.1))
  }
  // scalastyle:on magic.number
}
