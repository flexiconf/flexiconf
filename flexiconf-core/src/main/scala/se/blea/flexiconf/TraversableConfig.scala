package se.blea.flexiconf


/** Interface for objects that traverse configuration */
trait TraversableConfig {
  /** Get all child directives for this directive */
  def directives: List[Directive]

  /** Find one or more child directives **/
  def directives(names: String*): List[Directive]

  /** Find a child directive **/
  def directive(name: String): Directive

  def directive(names: (String, String)): (Directive, Directive) =
    (directive(names._1), directive(names._2))

  def directive(names: (String, String, String)): (Directive, Directive, Directive) =
    (directive(names._1), directive(names._2), directive(names._3))

  def directive(names: (String, String, String, String)): (Directive, Directive, Directive, Directive) =
    (directive(names._1), directive(names._2), directive(names._3), directive(names._4))

  def directive(names: (String, String, String, String, String)): (Directive, Directive, Directive, Directive, Directive) =
    (directive(names._1), directive(names._2), directive(names._3), directive(names._4), directive(names._5))

  def directive(names: (String, String, String, String, String, String)): (Directive, Directive, Directive, Directive, Directive, Directive) =
    (directive(names._1), directive(names._2), directive(names._3), directive(names._4), directive(names._5), directive(names._6))

  /** Returns whether or not this directive exists within this context **/
  def contains(name: String) : Boolean
  def ?(name: String)  = contains(name)

  /** Operators for alternative traversal of configuration **/
  def \(name: String) = directive(name)
  def \(names: (String, String)) = directive(names)
  def \(names: (String, String, String)) = directive(names)
  def \(names: (String, String, String, String)) = directive(names)
  def \(names: (String, String, String, String, String)) = directive(names)
  def \(names: (String, String, String, String, String, String)) = directive(names)
  def \\ = directives
  def \\(names: String*) = directives(names:_*)

  /** Return all warnings for this directive and its children **/
  def warnings: List[String]
}
