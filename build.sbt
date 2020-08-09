
organization in ThisBuild := "com.github.roti"
version in ThisBuild := "0.5"

intellijBuild in ThisBuild := "193.6494.35"
intellijPluginName in ThisBuild := "lut-intellij-support"

val paradiseVersion = "2.1.1"
val scala213 = "2.13.2"
val scala212 = "2.12.11"
val scala211 = "2.11.12"

scalaVersion in ThisBuild := scala213


lazy val library = project.in(file("library")).settings(
  name := "lut",
  //scalaVersion := "2.12.10",
  scalacOptions ++= (if (scalaVersion.value == scala213) Seq("-Ymacro-annotations") else Seq.empty),

  crossScalaVersions := Seq(scala211, scala212, scala213),

  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,

  libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.0",
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % "test",
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value,


  //resolvers += Resolver.sonatypeRepo("releases"),
  //if (scalaVersion.value != scala213)
  libraryDependencies ++= (if (scalaVersion.value != scala213) Seq(compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full)) else Seq.empty)
)

lazy val intellijSupport = project.in(file("intellij-support")).settings(
  name := "lut-intellij-support",
  scalaVersion := "2.12.10",
  intellijInternalPlugins += "java",
  intellijExternalPlugins += "org.intellij.scala".toPlugin
).enablePlugins(SbtIdeaPlugin)

onLoad in Global := (onLoad in Global).value andThen { s: State => "project library" :: s }

publishMavenStyle in ThisBuild := true
credentials in ThisBuild += Credentials(Path.userHome / ".sonatype_credentials")

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}


scmInfo in ThisBuild := Some(
  ScmInfo(
    url("https://github.com/roti/lut"),
    "scm:git@github.com:roti/lut.git"
  )
)

developers in ThisBuild := List(
  Developer(
    id    = "roti",
    name  = "Răzvan Rotaru",
    email = "razvan.rotaru@gmail.com",
    url   = url("https://github.com/roti")
  )
)

description in ThisBuild := "A library for data modelling in Scala"
licenses in ThisBuild := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt"))
homepage in ThisBuild := Some(url("https://github.com/roti/lut"))


