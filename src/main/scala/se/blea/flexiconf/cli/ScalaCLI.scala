package se.blea.flexiconf.cli

import java.io.{FileWriter, File}

import se.blea.flexiconf._
import se.blea.flexiconf.docgen.MarkdownDocumentationGenerator

object ScalaCLI {
  def main(args: Array[String]): Unit = {
    val schemaOpts = SchemaOptions.withSourceFile("src/main/resources/sample_schema.conf")
    val schema = Parser.parseSchema(schemaOpts)

    val configOpts = ConfigOptions.withSourceFile("src/main/resources/sample_config.conf")
      .ignoreUnknownDirectives
      .ignoreMissingGroups
      .withSchema(schema.get)

    val config = Parser.parseConfig(configOpts)

    println(config.get.renderTree)

    val warnings = config.get.warnings

    if (warnings.nonEmpty) {
      println("Warnings:")
      println(warnings.mkString("- ", "\n- ", ""))
    }

    val docs = new File("docs.html")
    val writer = new FileWriter(docs)
    writer.write(MarkdownDocumentationGenerator.process(schema.get))
    writer.close()

    // ConfigNode defaults = config.get("defaults");                               // get() for a single directive returns the node
    // long requestTimeout = defaults.get("requestTimeout").getIntArg("ms")        // Type-safe argument value getters
    // long requestTimeout = defaults.getIntArg("requestTimeout")                  // returns value for single-argument directives
    //
    // long errorStatusCode = defaults.getIntArg("serverErrorPage.statusCode")     // returns value for multi-argument directives when specified with dot
    // String errorFilePath = defaults.getStringArg("serverErrorPage.filePath")
    // long errorStatusCode = defaults.getIntArg("userErrorPage.statusCode")       // returns value for multi-argument directive for last config node in block
    //
    // List<ConfigNode> destinations = config.getAll("destination");               // getAll() returns list of config nodes
    // String destinationName = destination.get(0).getStringArg("name");
    //
    // List<ConfigNode> filters = destinations.get(0).getAll("filter");
    // String filterName = filters.get(0).getStringArg("name");
    // bool filterEnabled = filters.get(0).getBoolArg("enabled");

    //
    // ConfigNode filter = destinations.get(0).get("filter");                      // get() for multiple directives returns last config node
    // String filterName = filters.getStringArg("name");
    // bool filterEnabled = filters.getBoolArg("enabled");

//    val defaults = config.get("defaults")
//    //val badArg = config.get("defaults").getBoolArg("bahahah")
//    val badD = config.get("defaultzzzz")
//
//    val compression = defaults.get("compression").getBoolArg("enabled")
//    val requestTimeout = defaults.get("requestTimeout").getIntArg("ms")
//    val connectTimeout = defaults.get("connectTimeout").getIntArg("ms")
//    println(compression, requestTimeout, connectTimeout)
  }
}
