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



