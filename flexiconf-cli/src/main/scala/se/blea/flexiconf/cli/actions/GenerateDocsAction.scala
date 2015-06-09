package se.blea.flexiconf.cli.actions

import java.io.FileOutputStream

import se.blea.flexiconf.SchemaOptions
import se.blea.flexiconf.cli.OptionParser
import se.blea.flexiconf.docgen.TemplateDocGenerator

/** Generate documentation for a schema */
object GenerateDocsAction extends Action {
  override def name: String = "generate-docs"
  override def usage: String = "[-t|--template templatePath] <schemaPath> [target]"
  override def documentation: String = "Generate schema documentation, outputs to stdout if target is not given"

  case class Options(templatePath:String = "themes/default/layout.mustache")

  override def apply(_args: List[String]): Unit = {
    val (options, args) = OptionParser(_args.toList, Options(), (opts: Options) => {
      case ("-t" | "--template") :: path :: remaining => (opts.copy(templatePath = path), remaining)
    })

    if (args.length == 0 || args.length > 2) {
      exitWithUsageError()
    }

    val schemaPath = args(0)
    val documentationFilePath = args.lift(1)

    for {
      schemaOpts <- Some(SchemaOptions.withSourceFile(schemaPath))
      schema <- parseSchema(schemaOpts)
    } yield {
      val out = documentationFilePath.map(new FileOutputStream(_)).getOrElse(System.out)
      out.write(new TemplateDocGenerator(options.templatePath).process(schema).getBytes)
    }
  }
}
