package se.blea.flexiconf.cli.actions

import se.blea.flexiconf.cli.CLI

/** Print an internal representation of a configuration tree */
object DebugAction extends Action {
  override def name: String = "debug"
  override def usage: String = "<configPath> <schemaPath>"
  override def documentation: String = "Same as inspect, but includes internal nodes in the configuration tree"

  override def apply(args: List[String]): Unit = {
    if (args.length != 2) {
      exitWithUsageError()
    }

    val configPath = args(0)
    val schemaPath = args(1)

    parseWithWarnings(configPath, schemaPath, { config =>
      CLI.out(config.renderDebugTree)

      if (config.warnings.nonEmpty) {
        CLI.out("Warnings:")
        config.warnings.foreach(w => CLI.out(s"- $w"))
      }
    })
  }
}
