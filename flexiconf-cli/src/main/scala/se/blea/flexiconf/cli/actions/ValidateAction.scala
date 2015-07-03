package se.blea.flexiconf.cli.actions

import se.blea.flexiconf.cli.CLI

/** Validate a provided configuration and schema */
object ValidateAction extends Action {
  override def name: String = "validate"
  override def usage: String = "<configPath> <schemaPath>"
  override def documentation: String = "Validate a configuration against a schema"

  override def apply(args: List[String]): Unit = {
    if (args.length != 2) {
      exitWithUsageError()
    }

    val configPath = args(0)
    val schemaPath = args(1)

    parseWithWarnings(configPath, schemaPath, { config =>
      if (config.warnings.length == 0) {
        CLI.out("OK")
      } else {
        CLI.err("Failed:")
        config.warnings.foreach(r => CLI.err(s"- $r"))
      }
    })
  }
}
