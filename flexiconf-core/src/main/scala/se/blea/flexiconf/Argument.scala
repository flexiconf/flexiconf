package se.blea.flexiconf

/** Container for an argument value: name, value, and kind */
case class Argument(originalValue: String,
                    kind: ArgumentKind[_] = StringArgument,
                    name: String = "?") {
  val value = kind.valueOf(originalValue)
  override def toString = s"$name:$kind<$value>"
}

