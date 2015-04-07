package se.blea.flexiconf.cli;

import se.blea.flexiconf.ConfigOptions;
import se.blea.flexiconf.Schema;
import se.blea.flexiconf.SchemaOptions;
import se.blea.flexiconf.javaapi.*;

import java.util.List;

/**
 * @author Tristan Blease (tblease@groupon.com)
 */
public class JavaCLI {
    public static void main(String[] args) {
        SchemaOptions schemaOpts = SchemaOptions.withSourceFile("src/main/resources/sample_schema.conf");
        Schema schema = Parser.parseSchema(schemaOpts);

        ConfigOptions configOpts = ConfigOptions.withSourceFile("src/main/resources/sample_config.conf")
            .ignoreUnknownDirectives()
            .ignoreMissingGroups()
            .withSchema(schema);

        Config config = Parser.parseConfig(configOpts);

        printArgs(config.getDirectives());

        System.out.println("default > connectTimeout > " + config.getDirective("defaults")
                .getDirective("connectTimeout")
                .getIntArg("ms"));

        System.out.println("Filtered args");

        printArgs(config.getDirectives("defaults", "destination"));

        System.out.println(config.renderTree());
        System.out.println(config.getWarnings().size());
        config.getWarnings().forEach(System.out::println);
    }

    private static void printArgs(List<Directive> nodes) {
        nodes.forEach((node) -> {
            System.out.println(node.getName());
            node.getArgs().forEach((arg) -> {
                System.out.println(String.format(">> %s: %s", arg.getName(), arg.getKind()));
            });

            printArgs(node.getDirectives());
        });
    }
}
