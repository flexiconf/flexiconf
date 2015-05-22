package se.blea.flexiconf.cli

import java.io.{File, FileWriter}
import java.nio.file.Paths

import se.blea.flexiconf._
import se.blea.flexiconf.docgen.MarkdownDocGenerator

object CLI {
  case class CLIOptions(verbose: Boolean = false,
                        showUsage: Boolean = false)

  var opts = CLIOptions()

  val usage =
    """usage: flexiconf [-v] [-h] <action> [<args>]
      |
      |Available actions:
      |  inspect          Parse config, print configuration tree and warnings to stdout
      |                   args: <configPath> <schemaPath>
      |
      |  validate         Ensure config parses successfully without any warnings
      |                   args: <configPath> <schemaPath>
      |
      |  generate-docs    Generate schema documentation
      |                   args: <schemaPath>
      |
      |  debug            Same as inspect, but includes internal nodes in the configuration tree
      |                   args: <configPath> <schemaPath>""".stripMargin

  def main(_args: Array[String]): Unit = {
    val optionalArgs = _args.toList.takeWhile(_.startsWith("-"))
    implicit val args: List[String] = _args.toList.dropWhile(_.startsWith("-"))

    optionalArgs foreach {
      case "-v" => opts = opts.copy(verbose = true)
      case "--verbose" => opts = opts.copy(verbose = true)

      case "-h" => opts = opts.copy(showUsage = true)
      case "--help" => opts = opts.copy(showUsage = true)

      case a: String => exitWithError(s"unknown switch: $a\n\n" + usage)
    }

    if (opts.showUsage) {
      exitWithError(usage, 0)
    }

    if (args.isEmpty) {
      exitWithError(usage)
    }

    val action = args.head

    try {
      action match {
        case "inspect" => requireArgs(2, inspect(args(1), args(2)), "inspect requires both a config and schema")
        case "validate" => requireArgs(2, validate(args(1), args(2)), "validate requires both a config and schema")
        case "generate-docs" => requireArgs(Range(1, 2), generateDocs(args(1), args.lift(2)), "generate-docs requires a schema")
        case "debug" => requireArgs(2, debug(args(1), args(2)), "debug requires both a config and schema")
        case _ => exitWithError(s"$action is not a valid action\n\n" + usage)
      }
    } catch {
      case e: Exception => exitWithError(e.getMessage)
    }
  }

  def inspect(configPath: String, schemaPath: String) = {
    parseWithWarnings(configPath, schemaPath, { config =>
      println(config.renderTree)
      if (config.warnings.length > 0) {
        println("Warnings:")
        config.warnings.foreach(w => println(s"- $w"))
      }
    })
  }

  def debug(configPath: String, schemaPath: String) = {
    parseWithWarnings(configPath, schemaPath, { config =>
      println(config.renderDebugTree)
      if (config.warnings.length > 0) {
        println("Warnings:")
        config.warnings.foreach(w => println(s"- $w"))
      }
    })
  }

  def validate(configPath: String, schemaPath: String) = {
    parseWithWarnings(configPath, schemaPath, { config =>
      if (config.warnings.length == 0) {
        vprintln("\nOK")
      } else {
        fail(config.warnings)
      }
    })
  }

  def generateDocs(schemaPath: String, documentationFilePath: Option[String]) = {
    try {
      for {
        schemaOpts <- Some(SchemaOptions.withSourceFile(schemaPath))
        schema <- parseSchema(schemaOpts)
      } yield {
        val path = documentationFilePath.getOrElse(Paths.get(schemaPath).getFileName + ".html")
        vprintln(s"Generating documentation at $path")
        val docs = new File(path)
        val writer = new FileWriter(docs)
        writer.write(MarkdownDocGenerator.process(schema))
        writer.close()
      }
    } catch {
      case e: Exception => exitWithError(e.getMessage)
    }
  }

  private def requireArgs(num: Int, action: => Any, message: String)(implicit args: List[String]): Unit = {
    requireArgs(Range(num, num), action, message)
  }

  private def requireArgs(range: Range, action: => Any, message: String)(implicit args: List[String]): Unit = {
    val count = args.length - 1
    if (count >= range.start && count <= range.end) {
      action
    } else {
      val prefix = if (count < range.min) {
        "not enough arguments"
      } else {
        "too many arguments"
      }
      exitWithError(s"$prefix: $message")
    }
  }

  private def parseWithWarnings(configPath: String, schemaPath: String, fn: Config => Unit) = {
    for {
      schemaOpts <- Some(SchemaOptions.withSourceFile(schemaPath))
      schema <- parseSchema(schemaOpts)
      configOpts <- Some(ConfigOptions.withSourceFile(configPath)
        .ignoreDuplicateDirectives
        .ignoreIncludeCycles
        .ignoreMissingGroups
        .ignoreMissingIncludes
        .ignoreUnknownDirectives
        .withSchema(schema))
      config <- parseConfig(configOpts)
    } yield {
      fn(config)
    }
  }

  private def vprintln(msg: String) = {
    if (opts.verbose) {
      println(msg)
    }
  }

  private def parseSchema(opts: SchemaOptions) = {
    vprintln(s"Parsing schema ${opts.sourceFile}...")
    Parser.parseSchema(opts)
  }

  private def parseConfig(opts: ConfigOptions) = {
    vprintln(s"Parsing config ${opts.sourceFile}...")
    Parser.parseConfig(opts)
  }

  private def fail(e: Exception) = {
    Console.err.println(s"Failed: ${e.getMessage}")
  }

  private def fail(reasons: List[String]) = {
    Console.err.println("Failed:")
    reasons.foreach(r => Console.err.println(s"- $r"))
  }

  private def exitWithError(reason: String, code: Int = 1) = {
    Console.err.println(reason)
    System.exit(code)
  }
}
