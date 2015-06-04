package se.blea.flexiconf


/** Public API for accessing parsed configs */
trait Config extends TraversableConfig {
  /** Returns string representation of the config tree **/
  def renderTree: String

  /** Returns string representation of the config tree with internal nodes **/
  private[flexiconf] def renderDebugTree: String
}



















