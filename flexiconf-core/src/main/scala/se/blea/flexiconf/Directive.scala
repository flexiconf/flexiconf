package se.blea.flexiconf


/** Public interface for consuming configuration */
trait Directive {
  def name: String
  def args: List[Argument]
  def directives: List[Directive]
  def warnings: List[String]

  def intArg(name: String): Long
  def stringArg(name: String): String
  def boolArg(name: String): Boolean
  def decimalArg(name: String): Double
  def durationArg(name: String): Long
  def percentageArg(name: String): Double
}























