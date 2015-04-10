package se.blea.flexiconf

import java.io.InputStream

/** Options for parsing schemas */
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
