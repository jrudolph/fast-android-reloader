import sbt._
import Keys._
import AndroidKeys._

object FastReloadBuild extends Build {
  lazy val root =
    Project(".", file("."))
      .aggregate(plugin, runner)
  
  lazy val plugin =
    Project("plugin", file("plugin"))
  
  lazy val runner =
    Project("runner", file("runner"))
      .settings(AndroidSettings.settings: _*)

  object AndroidSettings {
    def settings = seq(
      platformName in Android := "android-11"
    ) ++ AndroidProject.androidSettings
  }
}