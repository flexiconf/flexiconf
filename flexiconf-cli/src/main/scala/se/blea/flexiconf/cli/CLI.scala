package se.blea.flexiconf.cli

import java.io.{File, FileWriter}
import java.nio.file.Paths

import se.blea.flexiconf._
import se.blea.flexiconf.docgen.MarkdownDocGenerator

object CLI {
  val usage =
    """ usage: flexiconf action ...
      |
      | actions:
      |   inspect <configFilePath> <schemaFilePath>
      |   validate <configFilePath> <schemaFilePath>
      |   generate-docs <schemaFilePath> [documentationFilePath]
      |   debug <configFilePath> <schemaFilePath>
    """.stripMargin

  def main(_args: Array[String]): Unit = {
    implicit val args: List[String] = _args.toList

    if (args.isEmpty) {
      exitWithError(usage)
    }

    val action = args.head

    action match {
      case "inspect" => requireArgs(2, inspect(args(1), args(2)), "inspect requires both a config and schema")
      case "validate" => requireArgs(2, validate(args(1), args(2)), "validate requires both a config and schema")
      case "generate-docs" => requireArgs(Range(1, 2), generateDocs(args(1), args.lift(2)), "generate-docs requires a schema")
      case "debug" => requireArgs(2, debug(args(1), args(2)), "debug requires both a config and schema")
      case _ => exitWithError(s"$action is not a valid action\n\n" + usage)
    }
  }

  def inspect(configFilePath: String, schemaFilePath: String) = {
    try {
      for {
        schemaOpts <- Some(SchemaOptions.withSourceFile(schemaFilePath))
        schema <- parseSchema(schemaOpts)
        configOpts <- Some(ConfigOptions.withSourceFile(configFilePath)
          .ignoreDuplicateDirectives
          .ignoreIncludeCycles
          .ignoreMissingGroups
          .ignoreMissingIncludes
          .ignoreUnknownDirectives
          .withSchema(schema))
        config <- parseConfig(configOpts)
      } yield {
        println(config.renderTree)
        if (config.warnings.length > 0) {
          println("Warnings:")
          config.warnings.foreach(w => println(s"- $w"))
        }
      }
    } catch {
      case e: Exception => exitWithError(e.getMessage)
    }
  }

  def debug(configFilePath: String, schemaFilePath: String) = {
    try {
      for {
        schemaOpts <- Some(SchemaOptions.withSourceFile(schemaFilePath))
        schema <- parseSchema(schemaOpts)
        configOpts <- Some(ConfigOptions.withSourceFile(configFilePath)
          .ignoreDuplicateDirectives
          .ignoreIncludeCycles
          .ignoreMissingGroups
          .ignoreMissingIncludes
          .ignoreUnknownDirectives
          .withSchema(schema))
        config <- parseConfig(configOpts)
      } yield {
        println(config.renderDebugTree)
        if (config.warnings.length > 0) {
          println("Warnings:")
          config.warnings.foreach(w => println(s"- $w"))
        }
      }
    } catch {
      case e: Exception => exitWithError(e.getMessage)
    }
  }

  def validate(configFilePath: String, schemaFilePath: String) = {
    try {
      for {
        schemaOpts <- Some(SchemaOptions.withSourceFile(schemaFilePath))
        schema <- parseSchema(schemaOpts)
        configOpts <- Some(ConfigOptions.withSourceFile(configFilePath)
          .ignoreDuplicateDirectives
          .ignoreIncludeCycles
          .ignoreMissingGroups
          .ignoreMissingIncludes
          .ignoreUnknownDirectives
          .withSchema(schema))
        config <- parseConfig(configOpts)
      } yield {
        if (config.warnings.length == 0) {
          println("\nOK")
        } else {
          fail(config.warnings)
        }
      }
    } catch {
      case e: Exception => exitWithError(e.getMessage)
    }
  }

  def generateDocs(schemaFilePath: String, documentationFilePath: Option[String]) = {
    try {
      for {
        schemaOpts <- Some(SchemaOptions.withSourceFile(schemaFilePath))
        schema <- parseSchema(schemaOpts)
      } yield {
        val path = documentationFilePath.getOrElse(Paths.get(schemaFilePath).getFileName + ".html")
        println(s"Generating documentation at $path")
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

  private def parseSchema(opts: SchemaOptions) = {
    println(s"Parsing schema ${opts.sourceFile}...")
    Parser.parseSchema(opts)
  }

  private def parseConfig(opts: ConfigOptions) = {
    println(s"Parsing config ${opts.sourceFile}...")
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
