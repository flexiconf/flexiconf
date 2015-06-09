package se.blea.flexiconf.cli

import se.blea.flexiconf.cli.actions._

object CLI {
  case class CLIOptions(verbose: Boolean = false,
                        showUsage: Boolean = false)

  val actions = List(
    InspectAction,
    ValidateAction,
    GenerateDocsAction,
    DebugAction
  )

  def usage = {
    val padLen = 2 + actions.map(_.name)
      .maxBy(_.length)
      .length

    val actionUsage = actions.map { act =>
      val paddedName = act.name.padTo(padLen, ' ')
      val paddedWhSp = "".padTo(padLen, ' ')

      s"  $paddedName  ${act.documentation}\n" +
      s"  $paddedWhSp  args: ${act.usage}\n"
    } mkString "\n"

    "usage: flexiconf [-v|--verbose] [-h|--help] <action> [<args>]\n\n" +
    "Available actions:\n" +
    actionUsage
  }

  def main(_args: Array[String]): Unit = {
    val (options, args) = OptionParser(_args.toList, CLIOptions(), (opts: CLIOptions) => {
      case ("-v" | "--verbose") :: remaining => (opts.copy(verbose = true), remaining)
      case ("-h" | "--help") :: remaining => (opts.copy(showUsage = true), remaining)
    })

    if (options.showUsage) {
      exit(usage)
    }

    if (args.isEmpty) {
      exit(usage, 1)
    }

    val act = args.head

    try {
      actions.find(_.name == act)
        .map(_.apply(args.tail))
        .orElse(throw new Exception(s"$act is not a valid action"))
    } catch {
      case e: Exception => exit(e.getMessage)
    }
  }

  def exit(reason: String, code: Int = 0): Unit = {
    Console.err.println(reason)
    System.exit(code)
  }
}
