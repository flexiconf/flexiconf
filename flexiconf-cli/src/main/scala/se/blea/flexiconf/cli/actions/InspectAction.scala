package se.blea.flexiconf.cli.actions

/** Print a representation of a configuration tree */
object InspectAction extends Action {
  override def name: String = "inspect"
  override def usage: String = "<configPath> <schemaPath>"
  override def documentation: String = "Parse config, print configuration tree and warnings to stdout"

  override def apply(args: List[String]): Unit = {
    if (args.length != 2) {
      exitWithUsageError()
    }

    val configPath = args(0)
    val schemaPath = args(1)

    parseWithWarnings(configPath, schemaPath, { config =>
      println(config.renderTree)

      if (config.warnings.nonEmpty) {
        println("Warnings:")
        config.warnings.foreach(w => println(s"- $w"))
      }
    })
  }
}
