import sbt.Keys._
import sbtfilter.Plugin.FilterKeys._

// Common settings
lazy val commonSettings = Seq(
  scalaVersion := "2.11.2",
  crossScalaVersions := Seq("2.10.4", "2.11.2"),
  organization := "se.blea.flexiconf",
  version := "0.1.0-SNAPSHOT",

  homepage := Some(url("http://www.github.com/flexiconf/flexiconf")),
  scmInfo := Some(ScmInfo(url("http://www.github.com/flexiconf/flexiconf"), "scm:git:git@github.com:flexiconf/flexiconf.git")),
  licenses := Seq("MIT" -> url("http://opensource.org/licenses/MIT")),

  organizationName := "Flexiconf",
  organizationHomepage := Some(url("http://www.github.com/flexiconf")),

  // Need to use XML here until a fix for rendering the developer key in poms
  // lands in the next SBT release (fixed in 1c8fe704)
  pomExtra := {
    <developers>
      <developer>
        <id>thetristan</id>
        <name>Tristan Blease</name>
        <email>tristan@blea.se</email>
        <url>http://tristan.blea.se/</url>
      </developer>
      <developer>
        <id>dustyburwell</id>
        <name>Dusty Burwell</name>
        <email>dustyburwell@gmail.com</email>
        <url>http://www.dustyburwell.com/</url>
      </developer>
    </developers>
  },

  // Publish options
  publishMavenStyle := true,
  publishArtifact in Test := false,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },

  // Release options
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,

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

// Root project
lazy val flexiconf = project.in(file("."))
  .settings(commonSettings:_*)
  .settings(publishArtifact := false)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .aggregate(
      core,
      docgen,
      javaApi,
      cli)

// Project definitions
lazy val core = project.in(file("flexiconf-core"))
  .settings(
    name := "flexiconf-core",
    description := "Flexible configuration for JVM projects")
  .settings(commonSettings:_*)
  .settings(antlr4Settings:_*)
  .settings(antlr4ConfigSettings:_*)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(commonDependencies:_*)
  .settings(
    libraryDependencies += "commons-io" % "commons-io" % "2.4")

lazy val docgen = project.in(file("flexiconf-docgen"))
  .settings(
    name := "flexiconf-docgen",
    description := "Documentation generators for flexiconf schemas")
  .settings(commonSettings:_*)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(commonDependencies:_*)
  .settings(
    libraryDependencies += "org.pegdown" % "pegdown" % "1.5.0",
    libraryDependencies += "com.github.spullara.mustache.java" % "compiler" % "0.9.0")
  .dependsOn(core)

lazy val javaApi = project.in(file("flexiconf-java-api"))
  .settings(
    name := "flexiconf-java-api",
    description := "Java-friendly API for flexiconf schemas and configs")
  .settings(commonSettings:_*)
  .disablePlugins(sbtassembly.AssemblyPlugin)
  .settings(commonDependencies:_*)
  .dependsOn(core)

lazy val cli = project.in(file("flexiconf-cli"))
  .settings(
    name := "flexiconf-cli",
    description := "CLI utility for working with flexiconf schemas and configs")
  .settings(commonSettings:_*)
  .settings(commonDependencies:_*)
  .settings(filterSettings:_*)
  .settings(
    includeFilter in (Compile, filterResources) := AllPassFilter)
  .settings(distSettings:_*)
  .settings(
    mainClass in Compile := Some("se.blea.flexiconf.cli.CLI"))
  .dependsOn(core)
  .dependsOn(docgen)

lazy val dist = taskKey[File]("Creates a distributable zip file containing flexiconf CLI tools")
lazy val distDirectory = settingKey[File]("Directory to use for the distribution")
lazy val distZip = settingKey[File]("Path to zipfile for the distribution")

lazy val distSettings = Seq(
  target in dist := target.value / ("dist/" + (distDirectory in dist).value),
  resources in dist := (resources in Compile).value,
  distDirectory in dist := new File(name.value + "-" + version.value),
  distZip in dist := new File((target in dist).value + ".zip"),

  dist <<= (streams,
      distZip in dist,
      classDirectory in Compile,
      resourceDirectory in Compile,
      target in dist,
      resources in dist,
      distDirectory in dist,
      assembly) map {
    (streams, distZip, classDir, source, target, resources, distDir, assemblySrc) =>
      // Copy assembly
      val assemblyDest = target / ("libs/" + assemblySrc.name)
      streams.log.debug(s"Copying $assemblySrc to $assemblyDest")
      IO.copy(Seq((assemblySrc, assemblyDest)))

      // Copy resources
      val copiedResources = resources.flatMap(_.relativeTo(source)) map { r =>
        val src = classDir / r.getPath
        val dst = target / r.getPath
        streams.log.debug(s"Copying $src to $dst")

        (src, dst)
      }

      IO.copy(copiedResources)

      // Mark bin scripts as executable
      val copiedResourceDests = copiedResources.map(_._2)
      val binScripts = copiedResourceDests
        .filter(_.toString.contains("/bin/"))

      binScripts foreach { bin =>
        streams.log.debug(s"Marking $bin as executable")
        bin.setExecutable(true)
      }

      // Zip it up - use commons compress so we can set executable bits
      import org.apache.commons.compress.archivers.zip._

      val distSources = copiedResourceDests :+ assemblyDest
      val distPaths = distSources.map(distDir / _.relativeTo(target).get.toString)
      val zipOut = new ZipArchiveOutputStream(new File(distZip.toString))

      (distSources zip distPaths filter(_._1.isFile)) foreach { case (src, dest) =>
        streams.log.debug(s"Adding $dest to $distZip")

        val entry = new ZipArchiveEntry(src, dest.toString)

        if (binScripts.contains(src)) {
          streams.log.debug(s"Marking $dest as executable")
          entry.setUnixMode(0755)
        }

        zipOut.putArchiveEntry(entry)
        zipOut.write(IO.readBytes(src))
        zipOut.closeArchiveEntry()
      }

      zipOut.close()

      streams.log.debug(s"Distribution built: $distZip")
      distZip
  })
