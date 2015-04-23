package se.blea.flexiconf

import org.scalatest.{FlatSpec, Matchers}

/** Test cases for Arguments */
class ArgumentSpec extends FlatSpec with Matchers {

  def accepts(v: String)(implicit a: ArgumentKind[_]) = assert(a accepts v)
  def rejects(v: String)(implicit a: ArgumentKind[_]) = assert(!(a accepts v))

  behavior of "StringArument"

  it should "return strings for string values" in {
    val a = StringArgument
    val res = a.valueOf("foo")

    assert(res == "foo")
    assert(res.isInstanceOf[String])
  }

  it should "recognize and accept string values" in {
    implicit val a = StringArgument

    accepts("\"hello\"")
    accepts("'world'")
    accepts("foobar")
    accepts("0")
    accepts("101")
    accepts("101.00001")
    accepts("0.00001")
    accepts("on")
    accepts("off")
    accepts("true")
    accepts("false")
    accepts("off")
    accepts("")
  }


  behavior of "IntArgument"

  it should "return longs for int values" in {
    val a = IntArgument
    val res = a.valueOf("101")

    assert(res == 101L)
    assert(res.isInstanceOf[Long])
  }

  it should "recognize and accept integer values" in {
    implicit val a = IntArgument

    rejects("\"hello\"")
    rejects("'world'")
    rejects("foobar")
    accepts("0")
    accepts("101")
    rejects("101.00001")
    rejects("0.00001")
    rejects("100.00001")
    rejects("on")
    rejects("off")
    rejects("true")
    rejects("false")
    rejects("")
  }


  behavior of "DecimalArgument"

  it should "return doubles for decimal values" in {
    val a = DecimalArgument
    val res = a.valueOf("101.00001")

    assert(res == 101.00001)
    assert(res.isInstanceOf[Double])
  }

  it should "recognize and accept decimal values" in {
    implicit val a = DecimalArgument

    rejects("\"hello\"")
    rejects("'world'")
    rejects("foobar")
    accepts("0")
    accepts("101")
    accepts("101.00001")
    accepts("0.00001")
    accepts("100.00001")
    rejects("on")
    rejects("off")
    rejects("true")
    rejects("false")
    rejects("")
  }

  behavior of "DurationArgument"

  it should "return longs for duration arguments" in {
    val a = DurationArgument
    val res = a.valueOf("10s")

    assert(res == 10000)
    assert(res.isInstanceOf[Long])
  }

  it should "recognize and accept duration values" in {
    implicit val a = DurationArgument

    rejects("\"hello\"")
    rejects("'world'")
    rejects("foobar")
    rejects("0")
    rejects("101")
    accepts("100ms")
    accepts("10s")
    accepts("1.5h")
    accepts("1.5y")
    accepts("1m")
    accepts("2h")
    rejects("on")
    rejects("off")
    rejects("true")
    rejects("false")
    rejects("")
  }

  behavior of "PercentageArgument"

  it should "return doubles for percentage arguments" in {
    val a = PercentageArgument
    val res = a.valueOf("50%")

    assert(res == 0.5)
    assert(res.isInstanceOf[Double])
  }

  it should "recognize and accept percentage values" in {
    implicit val a = PercentageArgument

    rejects("\"hello\"")
    rejects("'world'")
    rejects("foobar")
    rejects("0")
    rejects("101")
    rejects("100ms")
    accepts("10%")
    accepts("10.1%")
    accepts("1000%")
    rejects("on")
    rejects("off")
    rejects("true")
    rejects("false")
    rejects("")
  }

  behavior of "BoolArgument"

  it should "return booleans for boolean values" in {
    val a = BoolArgument

    Seq("true", "y", "yes", "on") foreach { v =>
      val res = a.valueOf(v)
      assert(res)
      assert(res.isInstanceOf[Boolean])
    }

    Seq("off", "no", "n", "false", "", "asdf") foreach { v =>
      val res = a.valueOf(v)
      assert(!res)
      assert(res.isInstanceOf[Boolean])
    }
  }

  it should "recognize and accept boolean values" in {
    implicit val a = BoolArgument

    rejects("\"hello\"")
    rejects("'world'")
    rejects("foobar")
    rejects("0")
    rejects("101")
    rejects("101.00001")
    rejects("0.00001")
    rejects("100.00001")
    accepts("on")
    accepts("off")
    accepts("true")
    accepts("false")
    rejects("")
  }
}


/** Test cases for argument parsing */
class ArgumentVisitorSpec extends FlatSpec with Matchers with ConfigHelpers {

  behavior of "#apply"

  it should "return an empty list of arguments when visiting an empty argument lists" in {
    val result = ArgumentVisitor(null)
    assert(result.size == 0)
  }

  it should "return list of arguments when visiting argument lists" in {
    val ctx = parse( """1 off "true" 500.2 foo bar 100ms 25%""")
    val result = ArgumentVisitor(ctx.argumentList)

    assert(result.size == 8)
    assert(result.forall(_.isInstanceOf[Argument]))

    assert(result(0).kind == IntArgument)
    assert(result(1).kind == BoolArgument)
    assert(result(2).kind == StringArgument)
    assert(result(3).kind == DecimalArgument)
    assert(result(4).kind == StringArgument)
    assert(result(5).kind == StringArgument)
    assert(result(6).kind == DurationArgument)
    assert(result(7).kind == PercentageArgument)

  }

  it should "return string arguments for quoted string arguments" in {
    val ctx = parse("\"foo\"").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).value == "foo")
  }

  it should "return string arguments for unquoted string arguments" in {
    val ctx = parse("foo").argument()
    val result = ArgumentVisitor(ctx)
    assert(result(0).kind == StringArgument)
    assert(result(0).value == "foo")
  }

  it should "visit quoted booleans and return string arguments" in {
    val result = ArgumentVisitor(parse("\"off\"").argument)
    assert(result(0).kind == StringArgument)
    assert(result(0).value == "off")
  }

  it should "return boolean arguments for boolean values" in {
    val result = ArgumentVisitor(parse("on").argument)
    assert(result(0).kind == BoolArgument)
    assert(result(0).value == "on")
  }

  it should "return integer arguments for integer values" in {
    val result = ArgumentVisitor(parse("10001").argument)
    assert(result(0).kind == IntArgument)
    assert(result(0).value == "10001")
  }

  it should "return decimal arguments for decimal values" in {
    val result = ArgumentVisitor(parse("0.300").argument)
    assert(result(0).kind == DecimalArgument)
    assert(result(0).value == "0.300")
  }

  it should "return duration arguments for duration values" in {
    val result = ArgumentVisitor(parse("10s").argument)
    assert(result(0).kind == DurationArgument)
    assert(result(0).value == "10s")
  }

  it should "return percentage arguments for percentage values" in {
    val result = ArgumentVisitor(parse("10%").argument)
    assert(result(0).kind == PercentageArgument)
    assert(result(0).value == "10%")
  }
}
