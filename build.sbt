import sbt.Keys._

// Common settings
lazy val commonSettings = Seq(
  scalaVersion := "2.11.2",
  organization := "se.blea.flexiconf",
  version := "0.1-SNAPSHOT",

  // Compile options
  javacOptions ++= Seq("-source", "1.7"),
  javacOptions ++= Seq("-target", "1.7"),
  scalacOptions += "-target:jvm-1.7",

  // Test options
  testOptions in Test += Tests.Argument("-oD")
)

lazy val commonDependencies = Seq(
  libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)

lazy val antlr4ConfigSettings = Seq(
  antlr4PackageName in Antlr4 := Some("se.blea.flexiconf.parser.gen"),
  antlr4GenListener in Antlr4 := false,
  antlr4GenVisitor in Antlr4 := true,
  unmanagedSourceDirectories in Compile += baseDirectory.value / "src" / "antlr4"
)

lazy val publishSettings = Seq(
  publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath+"/.m2/repository")))
)

// Project definitions
lazy val `flexiconf-core` = project.in(file("flexiconf-core"))
  .settings(commonSettings:_*)
  .settings(antlr4Settings:_*)
  .settings(antlr4ConfigSettings:_*)
  .settings(publishSettings:_*)
  .settings(commonDependencies:_*)
  .settings(
    libraryDependencies += "org.pegdown" % "pegdown" % "1.5.0",
    libraryDependencies += "commons-io" % "commons-io" % "2.4")

lazy val `flexiconf-docgen` = project.in(file("flexiconf-docgen"))
  .settings(commonSettings:_*)
  .settings(publishSettings:_*)
  .settings(commonDependencies:_*)
  .dependsOn(`flexiconf-core`)

lazy val `flexiconf-java-api` = project.in(file("flexiconf-java-api"))
  .settings(commonSettings:_*)
  .settings(publishSettings:_*)
  .settings(commonDependencies:_*)
  .dependsOn(`flexiconf-core`)

lazy val `flexiconf-cli` = project.in(file("flexiconf-cli"))
  .settings(commonSettings:_*)
  .settings(publishSettings:_*)
  .settings(commonDependencies:_*)
  .settings(
    mainClass in (Compile, run) := Some("se.blea.flexiconf.cli.CLI"))
  .dependsOn(`flexiconf-core`)
  .dependsOn(`flexiconf-docgen`)
