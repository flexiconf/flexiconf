package se.blea.flexiconf.javaapi

import org.scalatest.{Matchers, FlatSpec}
import se.blea.flexiconf
import se.blea.flexiconf._


class ArgumentSpec extends FlatSpec with Matchers {
  behavior of "getName"

  it should "return the name for an argument" in {
    val arg = new Argument(new flexiconf.Argument("bar", StringArgument, "foo"))
    arg.getName shouldEqual "foo"
  }

  behavior of "getKind"

  it should "return the kind of argument as an Enum" in {
    val stringArg = new Argument(new flexiconf.Argument("bar", StringArgument, "foo"))
    val intArg = new Argument(new flexiconf.Argument("123", IntArgument, "foo"))
    val boolArg = new Argument(new flexiconf.Argument("off", BoolArgument, "foo"))
    val decimalArg = new Argument(new flexiconf.Argument("10.1", DecimalArgument, "foo"))
    val durationArg = new Argument(new flexiconf.Argument("15m", DurationArgument, "foo"))
    val percentageArg = new Argument(new flexiconf.Argument("100%", PercentageArgument, "foo"))

    stringArg.getKind shouldEqual ArgumentKind.String
    intArg.getKind shouldEqual ArgumentKind.Int
    boolArg.getKind shouldEqual ArgumentKind.Bool
    decimalArg.getKind shouldEqual ArgumentKind.Decimal
    durationArg.getKind shouldEqual ArgumentKind.Duration
    percentageArg.getKind shouldEqual ArgumentKind.Percentage
  }
}
