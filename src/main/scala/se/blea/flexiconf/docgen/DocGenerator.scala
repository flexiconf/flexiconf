package se.blea.flexiconf.docgen

import se.blea.flexiconf.SchemaNode

trait DocGenerator {
  def process(node: SchemaNode): String
}


