package se.blea.flexiconf.cli

import org.scalatest.{FlatSpec, Matchers}

class OptionParserSpec extends FlatSpec with Matchers{
  behavior of "#apply"

  it should "return a list of arguments and options" in {
    case class Options(first: String = "",
                       second: Boolean = false)

    val args = "-f foobar --some-flag arg1 arg2".split(" ").toList

    val (options, parsedArgs) = OptionParser(args, Options(), (opts: Options) => {
      case "-f" :: first :: remaining => (opts.copy(first = first), remaining)
      case "--some-flag" :: remaining => (opts.copy(second = true), remaining)
    })

    assert(options.first == "foobar")
    assert(options.second)
    assert(List("arg1", "arg2") == parsedArgs)
  }

  it should "not pass options past the first argument" in {
    case class Options(first: String = "",
                       second: Boolean = false)

    val args = "-f foobar --some-flag arg1 arg2 -c -d -e foo".split(" ").toList

    val (options, parsedArgs) = OptionParser(args, Options(), (opts: Options) => {
      case "-f" :: first :: remaining => (opts.copy(first = first), remaining)
      case "--some-flag" :: remaining => (opts.copy(second = true), remaining)
    })

    assert(options.first == "foobar")
    assert(options.second)
    assert(List("arg1", "arg2", "-c", "-d", "-e", "foo") == parsedArgs)
  }

  it should "throw an exception for unknown options" in {
    case class Options(first: String = "",
                       second: Boolean = false)

    val args = "-x -f foobar --some-flag arg1 arg2 -c -d -e foo".split(" ").toList

    intercept[Exception] {
      OptionParser(args, Options(), (opts: Options) => {
        case "-f" :: first :: remaining => (opts.copy(first = first), remaining)
        case "--some-flag" :: remaining => (opts.copy(second = true), remaining)
      })
    }
  }
}
