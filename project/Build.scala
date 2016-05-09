import sbt._
import Keys._
import sbtassembly._
import sbtassembly.{ Assembly => SbtAssembly }
import sbtassembly.AssemblyKeys._


object ZeppelinAmmoniumBuild extends Build {
  object Versions {
    val ammonium =  "0.4.0-M6-1"
    val zeppelin =  "0.5.6-incubating"
  }

  lazy val depsSettings = Defaults.coreDefaultSettings ++ Seq(
    // dependencies
    libraryDependencies ++= Seq(
      "com.github.alexarchambault.ammonium" % "interpreter-api" % Versions.ammonium cross CrossVersion.full,
      "com.github.alexarchambault.ammonium" % "interpreter" % Versions.ammonium cross CrossVersion.full,
//      "org.apache.zeppelin" % "zeppelin-interpreter" % Versions.zeppelin,
      "org.scalatest" %% "scalatest" % "2.2.4" % "test"
    )
  )

  lazy val commonSettings =
    Seq(
      fork in Test := true,
      cancelable in Global := true,
      scalaVersion := "2.11.8",
      resolvers ++= Seq(
        Resolver.mavenLocal,
        Resolver.sonatypeRepo("releases")
      )
    )

  // project definition
  lazy val ZeppelinAmmonium = Project(id = "zeppelin-ammonium", base = file("."))
    .settings(commonSettings: _*)
    .settings(depsSettings: _*)
}
