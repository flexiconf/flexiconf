package se.blea.flexiconf


/** Default implementation of a directive */
class DefaultDirective(private val node: ConfigNode) extends Directive {
  /** Name of this directive */
  override def name: String = node.name

  /** Collect all warnings associated with this directive and child directives */
  override def warnings: List[String] = node.warnings

  /** Get all child directives for this directive */
  override def directives: List[Directive] = node.children.map(new DefaultDirective(_))

  /** Get all args **/
  override def args: List[Argument] = node.arguments

  /** Get int value of argument **/
  override def intArg(argName: String): Long = getArg(argName, IntArgument)

  /** Get string value of argument **/
  override def stringArg(argName: String): String = getArg(argName, StringArgument)

  /** Get boolean value of argument **/
  override def boolArg(argName: String): Boolean = getArg(argName, BoolArgument)

  /** Get decimal value of argument **/
  override def decimalArg(argName: String): Double = getArg(argName, DecimalArgument)

  /** Get duration value of argument **/
  override def durationArg(argName: String): Long = getArg(argName, DurationArgument)

  /** Get percentage value of argument **/
  override def percentageArg(argName: String): Double = getArg(argName, PercentageArgument)

  /** Return the argument value if it exists, throws exception otherwise */
  private def getArg[T](argName: String, kind: ArgumentKind[T]): T = {
    // throw exception if the directive accepts no argument
    if (args.size == 0) {
      throw new IllegalStateException(s"Unknown argument: $argName - directive '$name' accepts no arguments")
    }

    // Get argument value or throw exception if the named argument isn't valid for this directive
    args.find(_.name == argName).map(_.value).map(kind.valueOf) getOrElse {
      val validArgs = node.directive.parameters.mkString(" ")
      throw new IllegalStateException(s"Unknown argument: $argName - valid arguments for directive '$name': $validArgs")
    }
  }
}
