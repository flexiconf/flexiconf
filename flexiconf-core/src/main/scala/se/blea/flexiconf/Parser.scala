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

  private[flexiconf] def streamFromSourceFile(sourceFile: String): Option[InputStream] = {
    try {
      Some(FileUtils.openInputStream(new File(sourceFile)))
    } catch {
      case e: FileNotFoundException => None
    }
  }

  def parseConfig(opts: ConfigOptions): Option[Config] = {
    val stream = opts.inputStream orElse Parser.streamFromSourceFile(opts.sourceFile)
    val parser = antlrConfigParserFromStream(stream.get)

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
    val stream = opts.inputStream orElse Parser.streamFromSourceFile(opts.sourceFile)
    val parser = antlrSchemaParserFromStream(stream.get)

    SchemaVisitor(opts.visitorOpts)
      .visit(parser.document)
      .map(Schema)
  }
}
