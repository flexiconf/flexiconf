package se.blea.flexiconf

import java.io.{File, InputStream}

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

  private[flexiconf] def streamFromSourceFile(sourceFile: String): InputStream = {
    FileUtils.openInputStream(new File(sourceFile))
  }

  def parseConfig(opts: ConfigOptions): Option[DefaultConfig] = {
    val createStream = { Parser.streamFromSourceFile(opts.sourceFile) }
    val parser = antlrConfigParserFromStream(opts.inputStream getOrElse createStream)

    ConfigVisitor(opts.visitorOpts)
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

    SchemaVisitor(opts.visitorOpts)
      .visit(parser.document)
      .map(Schema)
  }
}
