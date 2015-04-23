package se.blea.flexiconf


/** Public API for accessing parsed configs */
trait Config {
  def directives: List[Directive]
  def warnings: List[String]
  def renderTree: String
  
  private[flexiconf] def renderDebugTree: String
}



















