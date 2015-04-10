package se.blea.flexiconf

import java.io.InputStream

/** Options for configuration parsing */
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



