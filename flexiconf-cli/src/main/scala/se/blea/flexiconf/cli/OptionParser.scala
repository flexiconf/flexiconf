package se.blea.flexiconf.cli


object OptionParser {

  /** Function that accepts an object representing your options and returns a partial function
    * that receives the remaining arguments to parse. This partial function should return
    * a tuple containing a new representation of your options and any remaining arguments for parsing
    */
  type OptionParserFunction[T] = (T => PartialFunction[List[String], (T, List[String])])

  /** Parse options from a list of arguments, stops parsing options after the first detected argument
    *
    * @param arguments List of arguments to parse
    * @param defaultOpts Object representing default options
    * @param optsParser Function that returns a partial function for parsing options from the argument list
    * @tparam T Type representing your options
    * @return Tuple container parsed options and non-option arguments
    */
  def apply[T](arguments: List[String], defaultOpts: T, optsParser: OptionParserFunction[T]) = {
    def parse(remainingArgs: List[String],
              capturedArgs: List[String],
              opts: T,
              optsParser: OptionParserFunction[T],
              shouldParseOpts: Boolean): (T, List[String]) = {
      val parser = optsParser(opts)

      if (parser.isDefinedAt(remainingArgs)) {
        val (newOpts, xs) = parser(remainingArgs)

        parse(xs, capturedArgs, newOpts, optsParser, shouldParseOpts)
      } else remainingArgs match {
        case x :: xs if shouldParseOpts && x.startsWith("-") => throw new Exception(s"unknown option: $x")
        case x :: xs => parse(xs, capturedArgs :+ x, opts, optsParser, shouldParseOpts = false)
        case _ => (opts, capturedArgs)
      }
    }

    parse(arguments, List(), defaultOpts, optsParser, shouldParseOpts = true)
  }
}
