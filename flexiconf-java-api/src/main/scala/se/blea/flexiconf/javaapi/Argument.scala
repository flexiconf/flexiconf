package se.blea.flexiconf.javaapi

import se.blea.flexiconf._


/** Java-friendly wrapper for the Argument API */
class Argument(private val _arg: se.blea.flexiconf.Argument) {
  def getName: String = _arg.name
  def getKind: ArgumentKind = _arg.kind match {
    case StringArgument => ArgumentKind.String
    case IntArgument => ArgumentKind.Int
    case BoolArgument => ArgumentKind.Bool
    case DecimalArgument => ArgumentKind.Decimal
    case DurationArgument => ArgumentKind.Duration
    case PercentageArgument => ArgumentKind.Percentage
    case _ => throw new IllegalArgumentException("Unknown argument kind")
  }
}
