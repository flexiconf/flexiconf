scalaVersion := "2.11.2"

libraryDependencies += "org.pegdown" % "pegdown" % "1.5.0"

libraryDependencies += "com.google.guava" % "guava" % "18.0"

libraryDependencies += "commons-io" % "commons-io" % "2.4"

antlr4Settings

antlr4PackageName in Antlr4 := Some("se.blea.flexiconf.parser.gen")

antlr4GenListener in Antlr4 := false

antlr4GenVisitor in Antlr4 := true

libraryDependencies += "org.scalatest" % "scalatest_2.11" % "2.2.1" % "test"

testOptions in Test += Tests.Argument("-oD")
