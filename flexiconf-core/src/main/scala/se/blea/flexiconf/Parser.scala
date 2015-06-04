package se.blea.flexiconf

import java.io.{FileNotFoundException, File, InputStream}

import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.apache.commons.io.FileUtils
import se.blea.flexiconf.parser.gen.{ConfigLexer, ConfigParser, SchemaLexer, SchemaParser}


object Parser {
  private[flexiconf] def antlrConfigParserFromStream(inputStream: InputStream) = {
    val input = new ANTLRInputStream(inputStream)
    val lexer = new ConfigLexer(input)
    val tokens = new CommonTokenStream(lexer)

    new ConfigParser(tokens)
  }

  private[flexiconf] def antlrSchemaParserFromStream(inputStream: InputStream) = {
    val input = new ANTLRInputStream(inputStream)
    val lexer = new SchemaLexer(input)
    val tokens = new CommonTokenStream(lexer)

    new SchemaParser(tokens)
  }

  private[flexiconf] def streamFromSourceFile(sourceFile: String): Option[InputStream] = {
    try {
      Some(FileUtils.openInputStream(new File(sourceFile)))
    } catch {
      case e: FileNotFoundException => None
    }
  }

  /** Parses and returns a config with the provided options **/
  def parseConfig(opts: ConfigOptions): Option[Config] = {
    val stream = opts.inputStream orElse Parser.streamFromSourceFile(opts.sourceFile)
    val parser = antlrConfigParserFromStream(stream.get)

    ConfigVisitor(opts.visitorOpts)
      .visit(parser.document)
      .map(DefaultConfig)
  }

  /** Parses and returns a config with the default options and provided schema **/
  def parseConfig(configFile: String, schema: Schema): Option[Config] = {
    parseConfig(ConfigOptions
      .withSourceFile(configFile)
      .withSchema(schema))
  }

  /** Parses and returns a config with the default options and provided schema **/
  def parseConfig(configName: String, configStream: InputStream, schema: Schema): Option[Config] = {
    parseConfig(ConfigOptions
      .withInputStream(configName, configStream)
      .withSchema(schema))
  }

  /** Parses and returns a config with the default options after parsing the schema with the default options **/
  def parseConfig(configFile: String, schemaFile: String): Option[Config] = {
    parseConfig(configFile, parseSchema(schemaFile).get)
  }

  /** Parses and returns a config with the default options after parsing the schema with the default options **/
  def parseConfig(configName: String, configStream: InputStream, schemaName: String, schemaStream: InputStream): Option[Config] = {
    parseConfig(configName, configStream, parseSchema(schemaName, schemaStream).get)
  }

  /** Parses and returns a schema with the provided options **/
  def parseSchema(opts: SchemaOptions): Option[Schema] = {
    val stream = opts.inputStream orElse Parser.streamFromSourceFile(opts.sourceFile)
    val parser = antlrSchemaParserFromStream(stream.get)

    SchemaVisitor(opts.visitorOpts)
      .visit(parser.document)
      .map(Schema)
  }

  /** Parses and returns a schema with the default options **/
  def parseSchema(schemaFile: String): Option[Schema] = {
    parseSchema(SchemaOptions.withSourceFile(schemaFile))
  }

  /** Parses and returns a schema with the default options **/
  def parseSchema(schemaName: String, schemaStream: InputStream): Option[Schema] = {
    parseSchema(SchemaOptions.withInputStream(schemaName, schemaStream))
  }
}
