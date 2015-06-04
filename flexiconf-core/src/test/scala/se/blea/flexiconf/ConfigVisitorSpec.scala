package se.blea.flexiconf

import java.io.File

import org.scalatest.{FlatSpec, Matchers}

/** Test cases for config parsing */
class ConfigVisitorSpec extends FlatSpec with Matchers with ConfigHelpers {

  behavior of "#sourceFromContext"

  it should "return a Source object based on the provided context" in {
    val ctx = parse("\n\nfoo bar baz")
    val source = visitor(defaultOptions)
      .sourceFromContext(ctx.argumentList().argument(2))

    assert(source.sourceFile == "test")
    assert(source.line == 3)
    assert(source.charPosInLine == 8)
  }


  behavior of "#visitInclude"

  it should "return an include directive node for includes" in {
    val conf = Configs.BASIC_TREE
    val ctx = parse(s"include $conf;")
    val result = visitor(defaultOptions.ignoreUnknownDirectives).visitDirective(ctx.directive())

    assert(result.get.name == "$include")
    assert(result.get.arguments(0).value.stringValue.get == conf)
  }

  it should "resolve absolute file include paths" in {
    val conf = Configs.BASIC_TREE
    val absPath = new File(".").toPath.toAbsolutePath.normalize.toString + "/" + conf
    val ctx = parse(s"include $absPath;")
    val result = visitor(defaultOptions.ignoreUnknownDirectives).visitDirective(ctx.directive())

    assert(result.get.name == "$include")
    assert(result.get.arguments(0).value.stringValue.get == absPath)
  }

  it should "throw an exception when an included file can't be found" in {
    intercept[IllegalStateException] {
      val ctx = parse("include foo/bar/baz_*.conf;")
      val result = visitor(defaultOptions).visitDirective(ctx.directive())
    }
  }

  it should "not allow directives to be repeated if they already exist in the given scope" in {
    intercept[IllegalStateException] {
      val conf = Configs.BASIC_TREE
      val directives = schema("directive arg1:String arg2:String [once];")
      val ctx = parse(s"directive faz qux; include $conf;")

      val opts = defaultOptions
        .ignoreUnknownDirectives
        .withDirectives(directives.children)

      val result = visitor(opts).visitDocument(ctx.document())
    }
  }


  behavior of "#visitGroup"

  it should "return a group directive node for groups" in {
    val ctx = parse("group my_group { foo 123; }")
    val root = node(DirectiveDefinition.root())
    val result = visitor(defaultOptions, makeStack(root)).visitDirective(ctx.directive())

    assert(result.get.name == "$group")
    assert(result.get.arguments(0).value.stringValue.get == "my_group")
    assert(result.get.children.size == 0)
  }


  behavior of "#visitUse"

  it should "return a use directive node for uses" in {
    val stack = emptyStackWithSchema("foo val:Int; bar;")
    val ctx = parse("group my_group { foo 123; } use my_group;")
    val result = visitor(defaultOptions, stack).visitDirectiveList(ctx.directiveList())

    assert(result.get.name == "$use")
    assert(result.get.arguments(0).value.stringValue.get == "my_group")
  }

  it should "return a warning node when an unknown group is encountered and missing groups are ignored" in {
    val ctx = parse("use my_group;")
    val result = visitor(defaultOptions.ignoreMissingGroups).visitDirective(ctx.directive())

    assert(result.get.name == "$use")
    assert(result.get.arguments(0).value.contains("my_group"))
    assert(result.get.children(0).name == "$warning")
    assert(result.get.children(0).arguments(0).value.contains("Unknown group"))
  }

  it should "throw an exception when no groups are defined and missing groups are not ignored" in {
    intercept[IllegalStateException] {
      val ctx = parse("use my_group;")
      visitor(defaultOptions).visitDirective(ctx.directive())
    }
  }

  it should "not allow directives to be repeated if they already exist in the given scope" in {
    intercept[IllegalStateException] {
      val stack = emptyStackWithSchema("foo val:Int [once];")
      val ctx = parse("group my_group { foo 123; } use my_group; foo 234;")

      val result = visitor(defaultOptions, stack).visitDocument(ctx.document())
    }
  }

  it should "allow nested directives to be repeated across different scopes (no duplicate directive exception)" in {
    val stack = emptyStackWithSchema(
      """
        | outer {
        |   inner { single val:Int [once]; }
        | }
      """.stripMargin)

    val ctx = parse(
      """
        | group ref {
        |   inner { single 123; }
        | }
        |
        | outer { use ref; }
        | outer { use ref; }
      """.stripMargin)

    visitor(defaultOptions, stack).visitDocument(ctx.document())
  }


  behavior of "#visitUserDirective"

  it should "return a user directive node for non-built-in directives" in {
    val stack = emptyStackWithSchema("foo val:Int;")
    val ctx = parse("foo 123;")
    val result = visitor(defaultOptions, stack).visitDirective(ctx.directive())

    assert(result.get.name == "foo")
    assert(result.get.arguments(0).value.stringValue.get == "123")
  }

  it should "allow multiple directives if a directive permits multiples" in {
    val stack = emptyStackWithSchema("foo val:Int;")
    val ctx = parse("foo 123; foo 246;")
    visitor(defaultOptions, stack).visitDirectives(ctx.directiveList())
  }

  it should "throw an exception if a directive already exists and does not allow multiples" in {
    intercept[IllegalStateException] {
      val stack = emptyStackWithSchema("foo val:Int [once];")
      val ctx = parse("foo 123; foo 246;")
      visitor(defaultOptions, stack).visitDirectives(ctx.directiveList())
    }
  }

  it should "return an error node when a directive already exists, does not allow multiples, and warnings are enabled" in {
    val stack = emptyStackWithSchema("foo val:Int [once];")
    val ctx = parse("foo 123; foo 246;")
    val result = visitor(defaultOptions.ignoreUnknownDirectives)
      .visitDirective(ctx.directive())

    assert(result.get.name == "$warning")
    assert(result.get.arguments(0).value.contains("Unknown directive"))
  }

  it should "throw an exception if no valid directives are recognized" in {
    intercept[IllegalStateException] {
      val ctx = parse("foo 123 bar;")
      visitor(defaultOptions).visitDirective(ctx.directive())
    }
  }

  it should "return an error node when no valid directives are recognized and warnings are enabled" in {
    val ctx = parse("foo bar bar;")
    val result = visitor(defaultOptions.ignoreUnknownDirectives)
      .visitDirective(ctx.directive())

    assert(result.get.name == "$warning")
    assert(result.get.arguments(0).value.contains("Unknown directive"))
  }


  behavior of "#visitDirectives"

  it should "return an empty list of directives when visiting an empty directive list" in {
    val result = visitor(defaultOptions).visitDirectives(null)

    assert(result.size == 0)
  }

  it should "return list of directives when visiting directive lists" in {
    val stack = emptyStackWithSchema("foo val:Int; bar;")
    val ctx = parse("foo 123; bar;")
    val result = visitor(defaultOptions, stack).visitDirectives(ctx.directiveList())

    assert(result.size == 2)
    assert(result(0).name == "foo")
    assert(result(0).arguments(0).value.stringValue.get == "123")
    assert(result(1).name == "bar")
  }
}
