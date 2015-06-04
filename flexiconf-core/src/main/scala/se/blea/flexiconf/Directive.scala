package se.blea.flexiconf


/** Public interface for consuming configuration */
trait Directive extends TraversableConfig {
  /** Returns name of the directive **/
  def name: String

  /** Returns all args **/
  def args: List[Argument]

  /** Return the first value */
  def apply: ArgumentValue

  /** Return the first value, or the other value **/
  def or(other: ArgumentValue): ArgumentValue = apply | other
  def |(other: ArgumentValue) = or(other)

  /** Find an argument by name **/
  def argValue(name: String): ArgumentValue

  /** Find an argument by index **/
  def argValue(index: Int): ArgumentValue

  /** Find multiple arguments by name **/
  def argValue(names: (String, String)): (ArgumentValue, ArgumentValue) =
    (argValue(names._1), argValue(names._2))

  def argValue(names: (String, String, String)): (ArgumentValue, ArgumentValue, ArgumentValue) =
    (argValue(names._1), argValue(names._2), argValue(names._3))

  def argValue(names: (String, String, String, String)): (ArgumentValue, ArgumentValue, ArgumentValue, ArgumentValue) =
    (argValue(names._1), argValue(names._2), argValue(names._3), argValue(names._4))

  def argValue(names: (String, String, String, String, String)): (ArgumentValue, ArgumentValue, ArgumentValue, ArgumentValue, ArgumentValue) =
    (argValue(names._1), argValue(names._2), argValue(names._3), argValue(names._4), argValue(names._5))

  def argValue(names: (String, String, String, String, String, String)): (ArgumentValue, ArgumentValue, ArgumentValue, ArgumentValue, ArgumentValue, ArgumentValue) =
    (argValue(names._1), argValue(names._2), argValue(names._3), argValue(names._4), argValue(names._5), argValue(names._6))

  /** Operators for alternative traversal of configuration by name **/
  def %(name: String) = argValue(name)
  def %(names: (String, String)) = argValue(names)
  def %(names: (String, String, String)) = argValue(names)
  def %(names: (String, String, String, String)) = argValue(names)
  def %(names: (String, String, String, String, String)) = argValue(names)
  def %(names: (String, String, String, String, String, String)) = argValue(names)
}


object Directive {
  /** Implicit conversions **/
  implicit def directive2argumentValue(d: Directive): ArgumentValue = d.apply
  implicit def directive2string(d: Directive): String = d.apply
  implicit def directive2bool(d: Directive): Boolean = d.apply
  implicit def directive2int(d: Directive): Int = d.apply
  implicit def directive2long(d: Directive): Long = d.apply
  implicit def directive2float(d: Directive): Float = d.apply
  implicit def directive2double(d: Directive): Double = d.apply
}




















