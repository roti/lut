
organization in ThisBuild := "com.github.roti"
version in ThisBuild := "0.5-SNAPSHOT"

intellijBuild in ThisBuild := "193.6494.35"
intellijPluginName in ThisBuild := "lut-intellij-support"

lazy val library = project.in(file("library")).settings(
  name := "lut",
  scalaVersion := "2.13.1",
  scalacOptions += "-Ymacro-annotations",

  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,

  libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.0",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % "test",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

lazy val intellijSupport = project.in(file("intellij-support")).settings(
  name := "lut-intellij-support",
  scalaVersion := "2.12.10",
  intellijInternalPlugins += "java",
  intellijExternalPlugins += "org.intellij.scala".toPlugin
).enablePlugins(SbtIdeaPlugin)

onLoad in Global := (onLoad in Global).value andThen { s: State => "project library" :: s }

publishMavenStyle in ThisBuild := true
credentials in ThisBuild += Credentials(Path.userHome / ".sbt" / ".credentials")

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}