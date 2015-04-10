package se.blea.flexiconf


trait SchemaNode {
  def name:String
  def parameters: List[Parameter]
  def source: Source
  def flags: Set[DirectiveFlag]
  def documentation: String
  def children: List[SchemaNode]
  def toDirectives: Set[DirectiveDefinition]
}
