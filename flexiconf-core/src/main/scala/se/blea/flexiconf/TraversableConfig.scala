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

  /** Returns whether or not the named directive exists within this context **/
  def contains(name: String): Boolean
  def ?(name: String): Boolean = contains(name) // scalastyle:ignore method.name

  /** Returns whether or not the named directive is allowed within this context **/
  // scalastyle:off method.name
  def allows(name: String): Boolean
  def ??(name: String): Boolean = allows(name) // scalastyle:ignore method.name

  /** Operators for alternative traversal of configuration **/
  // scalastyle:off method.name
  def \(name: String): Directive = directive(name)
  def \(names: (String, String)): (Directive, Directive) = directive(names)
  def \(names: (String, String, String)): (Directive, Directive, Directive) = directive(names)
  def \(names: (String, String, String, String)): (Directive, Directive, Directive, Directive) = directive(names)
  def \(names: (String, String, String, String, String)): (Directive, Directive, Directive, Directive, Directive) = directive(names)
  def \(names: (String, String, String, String, String, String)): (Directive, Directive, Directive, Directive, Directive, Directive) = directive(names)
  def \\ : List[Directive] = directives
  def \\(names: String*): List[Directive] = directives(names:_*)
  // scalastyle:on method.name

  /** Return all warnings for this directive and its children **/
  def warnings: List[String]
}
