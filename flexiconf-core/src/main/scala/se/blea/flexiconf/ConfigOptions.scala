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

  def ignoreDuplicateDirectives: ConfigOptions = {
    visitorOpts = visitorOpts.copy(allowDuplicateDirectives = true)
    this
  }

  def ignoreUnknownDirectives: ConfigOptions = {
    visitorOpts = visitorOpts.copy(allowUnknownDirectives = true)
    this
  }

  def ignoreMissingGroups: ConfigOptions = {
    visitorOpts = visitorOpts.copy(allowMissingGroups = true)
    this
  }

  def ignoreMissingIncludes: ConfigOptions = {
    visitorOpts = visitorOpts.copy(allowMissingIncludes = true)
    this
  }

  def ignoreIncludeCycles: ConfigOptions = {
    visitorOpts = visitorOpts.copy(allowIncludeCycles = true)
    this
  }

  def withDirectives(ds: Set[DirectiveDefinition]): ConfigOptions = {
    visitorOpts = visitorOpts.copy(directives = visitorOpts.directives ++ ds)
    this
  }

  def withDirectives(d: DirectiveDefinition*): ConfigOptions = {
    visitorOpts = visitorOpts.copy(directives = visitorOpts.directives ++ d)
    this
  }

  def withSchema(s: Schema): ConfigOptions = {
    visitorOpts = visitorOpts.copy(directives = visitorOpts.directives ++ s.toDirectives)
    this
  }
}

object ConfigOptions {
  def withSourceFile(sourceFile: String): ConfigOptions = ConfigOptions(sourceFile = sourceFile)
  def withInputStream(streamName: String, inputStream: InputStream): ConfigOptions = ConfigOptions(inputStream = Option(inputStream))
}



