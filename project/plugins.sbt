resolvers ++= Seq(
  "simplytyped" at "http://simplytyped.github.io/repo/releases",
  "sonatype-releases" at "https://oss.sonatype.org/content/repositories/releases/")

addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.7.0")

addSbtPlugin("com.simplytyped" % "sbt-antlr4" % "0.7.4")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.13.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.1")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-pack" % "0.6.12")
