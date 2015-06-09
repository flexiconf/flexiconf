package se.blea.flexiconf.cli.actions

import se.blea.flexiconf.cli.CLI
import se.blea.flexiconf.{Config, ConfigOptions, Parser, SchemaOptions}

/**
 * Created by tblease on 5/23/15.
 */
trait Action {
  def apply(args: List[String]): Unit
  def documentation: String
  def name: String
  def usage: String

  def parseWithWarnings(configPath: String, schemaPath: String, fn: Config => Unit) = {
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

  def parseSchema(opts: SchemaOptions) = {
    Parser.parseSchema(opts)
  }

  def parseConfig(opts: ConfigOptions) = {
    Parser.parseConfig(opts)
  }

  def exitWithUsageError() = {
    CLI.exit(s"action $name requires args: $usage", 1)
  }
}
