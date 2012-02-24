import sbt._
import Keys._

object FastReloadBuild extends Build {
  lazy val root =
    Project(".", file("."))
      .aggregate(plugin, runner)
  
  lazy val plugin =
    Project("plugin", file("plugin"))
  
  lazy val runner =
    Project("runner", file("runner"))
}