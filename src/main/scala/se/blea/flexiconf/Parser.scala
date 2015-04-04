package se.blea.flexiconf

import java.io.{File, InputStream}

import org.antlr.v4.runtime.{ANTLRInputStream, CommonTokenStream}
import org.apache.commons.io.FileUtils
import se.blea.flexiconf.parser.gen.{ConfigLexer, ConfigParser, SchemaLexer, SchemaParser}


case class ConfigOptions private (private[flexiconf] val sourceFile: String = "",
                                  private[flexiconf] val inputStream: Option[InputStream] = None) {
  private val missingSourceFile = sourceFile.isEmpty
  private val missingInputStream = inputStream.isEmpty

  if (missingSourceFile && missingInputStream) {
    throw new IllegalStateException("A source file or valid input stream must be supplied")
  }

  private[flexiconf] var visitorOpts = ConfigVisitorOptions(sourceFile)

  def ignoreUnknownDirectives = {
    visitorOpts = visitorOpts.copy(allowUnknownDirectives = true)
    this
  }

  def ignoreMissingGroups = {
    visitorOpts = visitorOpts.copy(allowMissingGroups = true)
    this
  }

  def ignoreMissingIncludes = {
    visitorOpts = visitorOpts.copy(allowMissingIncludes = true)
    this
  }

  def ignoreIncludeCycles = {
    visitorOpts = visitorOpts.copy(allowIncludeCycles = true)
    this
  }

  def withDirectives(ds: Set[DirectiveDefinition]) = {
    visitorOpts = visitorOpts.copy(directives = visitorOpts.directives ++ ds)
    this
  }

  def withDirectives(d: DirectiveDefinition*) = {
    visitorOpts = visitorOpts.copy(directives = visitorOpts.directives ++ d)
    this
  }

  def withSchema(s: Schema) = {
    visitorOpts = visitorOpts.copy(directives = visitorOpts.directives ++ s.toDirectives)
    this
  }
}


object ConfigOptions {
  def withSourceFile(sourceFile: String) = ConfigOptions(sourceFile = sourceFile)
  def withInputStream(streamName: String, inputStream: InputStream) = ConfigOptions(inputStream = Option(inputStream))
}


case class SchemaOptions private (private[flexiconf] val sourceFile: String = "",
                                  private[flexiconf] val inputStream: Option[InputStream] = None) {
  private val missingSourceFile = sourceFile.isEmpty
  private val missingInputStream = inputStream.isEmpty

  if (missingSourceFile && missingInputStream) {
    throw new IllegalStateException("A source file or valid input stream must be supplied")
  }

  private[flexiconf] var visitorOpts = SchemaVisitorOptions(sourceFile)
}


object SchemaOptions {
  def withSourceFile(sourceFile: String) = SchemaOptions(sourceFile = sourceFile)
  def withInputStream(streamName: String, inputStream: InputStream) = SchemaOptions(inputStream = Option(inputStream))
}


object Parser {
  private[flexiconf] def antlrConfigParserFromStream(inputStream: InputStream) = {
    val input = new ANTLRInputStream(inputStream)
    val lexer = new ConfigLexer(input)
    val tokens = new CommonTokenStream(lexer)

    new ConfigParser(tokens)
  }

  private[flexiconf] def streamFromSourceFile(sourceFile: String): InputStream = {
    FileUtils.openInputStream(new File(sourceFile))
  }

  def parseConfig(opts: ConfigOptions): Option[DefaultConfig] = {
    val createStream = { Parser.streamFromSourceFile(opts.sourceFile) }
    val parser = antlrConfigParserFromStream(opts.inputStream getOrElse createStream)

    ConfigNodeVisitor(opts.visitorOpts)
      .visit(parser.document)
      .map(DefaultConfig)
  }

  private[flexiconf] def antlrSchemaParserFromStream(inputStream: InputStream) = {
    val input = new ANTLRInputStream(inputStream)
    val lexer = new SchemaLexer(input)
    val tokens = new CommonTokenStream(lexer)

    new SchemaParser(tokens)
  }

  def parseSchema(opts: SchemaOptions): Option[Schema] = {
    val createStream = { Parser.streamFromSourceFile(opts.sourceFile) }
    val parser = antlrSchemaParserFromStream(opts.inputStream getOrElse createStream)

    SchemaNodeVisitor(opts.visitorOpts)
      .visit(parser.document)
      .map(Schema)
  }
}
