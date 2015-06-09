package se.blea.flexiconf

import scala.annotation.varargs

/**
 * Defines the name, parameters, and allowed child directives for a configuration directive
 *
 * User-defined directive names may consist of any non-whitespace characters as long as they
 * do not start with '$' so that they can be identified separately from built-in directives that
 * start with '$' (e.g. \$root, \$use, \$group, \$include).
 */
private[flexiconf] case class DirectiveDefinition private[flexiconf](name: String,
                                                                     parameters: List[Parameter] = List.empty,
                                                                     flags: Set[DirectiveFlag] = Set.empty,
                                                                     documentation: String = "",
                                                                     children: Set[DirectiveDefinition] = Set.empty) {

  /** True if this directive expects a block */
  private[flexiconf] val requiresBlock = children.nonEmpty

  /** True if this directive should not be repeated within a block */
  private[flexiconf] val allowOnce = flags.contains(DirectiveFlags.AllowOnce)

  def toDocumentation: String = {
    documentation ++ "\n" ++ children.map(_.toDocumentation).mkString("\n")
  }

  override def toString: String = {
    var res = name

    if (parameters.nonEmpty) {
      res ++= parameters.map(_.toString).mkString(" ", " ", "")
    }

    if (requiresBlock) {
      res ++= " {}"
    }

    res
  }
}


object DirectiveDefinition {
  /** Indicates the start of the configuration tree */
  private[flexiconf] def root(ds: Set[DirectiveDefinition] = Set.empty) = new DirectiveDefinition(name = "$root", children = ds)

  private[flexiconf] def root(ds: DirectiveDefinition*) = new DirectiveDefinition(name = "$root", children = ds.toSet)

  /** Allows inclusion of multiple, additional configuration trees */
  private[flexiconf] def include(ds: Set[DirectiveDefinition]) = new DirectiveDefinition(name = "$include", children = ds, parameters = List(Parameter("pattern")))

  /** Defines a group of directives that can be used elsewhere in the configuration tree */
  private[flexiconf] def group = new DirectiveDefinition(name = "$group", parameters = List(Parameter("name")))

  /** Includes directives from a pre-defined group in the configuration tree */
  private[flexiconf] def use(ds: Set[DirectiveDefinition]) = new DirectiveDefinition( name = "$use", children = ds, parameters = List(Parameter("pattern")))

  /** Placeholder for errors encountered when parsing a configuration tree */
  private[flexiconf] def warning = new DirectiveDefinition(name = "$warning", parameters = List(Parameter("message")))

  /** Placeholder for unknown directives when reading a configuration tree */
  private[flexiconf] val unknown = new DirectiveDefinition(name = "unknown")

  /** Find the first matching directive given a list of allowed directives */
  private[flexiconf] def find(maybeDirective: MaybeDirective,
                              children: Set[DirectiveDefinition]): Option[DirectiveDefinition] = {
    children.find(maybeDirective.matches)
  }

  /** Returns a directive builder for a directive with the specified name */
  def withName(name: String) = Builder(name)

  /** Returns a directive builder for a directive with the specified name */
  private[flexiconf] def withUnsafeName(name: String) = Builder(name = name, allowInternal = true)

  case class Builder private[flexiconf] (name: String,
                                      parameters: List[Parameter] = List.empty,
                                      flags: Set[DirectiveFlag] = Set.empty,
                                      documentation: String = "",
                                      children: Set[DirectiveDefinition] = Set.empty,
                                      allowInternal: Boolean = false) {

    // Name validation
    if (name == null) {
      throw new NullPointerException
    }

    if (name.isEmpty) {
      throw new IllegalArgumentException("Name cannot be empty")
    }

    if (name.startsWith("$") && !allowInternal) {
      throw new IllegalArgumentException(s"Name '$name' cannot start with '$$'")
    }

    // Public methods

    /** Adds a new string parameter */
    def withStringArg(name: String) = {
      withArgument(name, StringArgument)
    }

    /** Adds a new boolean parameter */
    def withBoolArg(name: String) = {
      withArgument(name, BoolArgument)
    }

    /** Adds a new integer parameter */
    def withIntArg(name: String) = {
      withArgument(name, IntArgument)
    }

    /** Adds a new decimal parameter */
    def withDecimalArg(name: String) = {
      withArgument(name, DecimalArgument)
    }

    /** Adds a new duration parameter */
    def withDurationArg(name: String) = {
      withArgument(name, DurationArgument)
    }

    /** Adds a new percentage parameter */
    def withPercentageArg(name: String) = {
      withArgument(name, PercentageArgument)
    }

    /** Add documentation for this directive */
    def withDocumentation(documentation: String) = {
      copy(documentation = documentation)
    }

    /** Allow a directive to be used multiple times within a scope */
    def allowOnce() = {
      copy(flags = flags + DirectiveFlags.AllowOnce)
    }

    /** Allows one or more child directives within a block supplied to this directive */
    @varargs
    def withDirectives(ds: DirectiveDefinition*) = {
      copy(children = children ++ ds)
    }

    /** Returns new Directive with the previously defined options */
    def build = DirectiveDefinition(name, parameters, flags, documentation, children)


    // Private methods

    /** Adds a new parameter with the provided name and type */
    private def withArgument(name: String, kind: ArgumentKind[_]) = {
      if (name == null) {
        throw new NullPointerException
      }

      if (name.isEmpty) {
        throw new IllegalArgumentException("Name cannot be empty")
      }

      copy(parameters = parameters :+ Parameter(name, kind))
    }

    /** Private builder features only for use with schema */
    private[flexiconf] def withParameters(params: List[Parameter]) = {
      copy(parameters = params)
    }

    /** Private builder features only for use with schema */
    private[flexiconf] def withFlags(flags: Set[DirectiveFlag]) = {
      copy(flags = flags)
    }
  }
}
