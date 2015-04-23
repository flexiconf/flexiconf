package se.blea.flexiconf.javaapi

import se.blea.flexiconf.{Schema, SchemaOptions, ConfigOptions}


/** Java-friendly wrapper for the Parser API */
object Parser {
  def parseConfig(opts: ConfigOptions): Config = {
    se.blea.flexiconf.Parser.parseConfig(opts)
      .map(new Config(_))
      .orNull
  }

  def parseSchema(opts: SchemaOptions): Schema = {
    se.blea.flexiconf.Parser.parseSchema(opts).orNull
  }
}
