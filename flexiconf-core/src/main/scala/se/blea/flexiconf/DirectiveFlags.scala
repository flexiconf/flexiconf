package se.blea.flexiconf


/** Base trait for all directive flags */
trait DirectiveFlag {
  def documentation: String
}


/** Flags that affect how directives should be handled when creating the final configuration tree */
case class DirectiveFlags(flags: Set[DirectiveFlag] = Set.empty) {
  def allowOnce = flags.contains(DirectiveFlags.AllowOnce)

  override def toString: String = {
    flags.mkString("[", ",", "]")
  }
}


/** Collection of valid flags */
object DirectiveFlags {
  object AllowOnce extends DirectiveFlag {
    override def documentation = "Allow a directive to be specified only once in a surrounding context"
    override def toString: String = "once"
  }
}
