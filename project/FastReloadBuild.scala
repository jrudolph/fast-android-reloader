import sbt._
import Keys._
import AndroidKeys._

object FastReloadBuild extends Build {
  lazy val root =
    Project("fast-android-reloader", file("."))
      .aggregate(plugin, runner)
  
  lazy val plugin =
    Project("plugin", file("plugin"))
      .settings(
        sbtPlugin := true
      )
      .settings(generalSettings: _*)
      .settings(
        resolvers += Resolver.url("scalasbt snapshots", new URL("http://scalasbt.artifactoryonline.com/scalasbt/sbt-plugin-snapshots"))(Resolver.ivyStylePatterns),
        addSbtPlugin("org.scala-sbt" % "sbt-android-plugin" % "0.6.1-SNAPSHOT")
      )
  
  lazy val runner =
    Project("runner", file("runner"))
      .settings(AndroidSettings.myAndroidSettings: _*)
      .settings(AndroidSettings.config: _*)
      .settings(generalSettings: _*)
      .settings(
        autoScalaLibrary := false,
        crossPaths := false
      )

  lazy val generalSettings = seq(
    organization := "net.virtual-void",
    version := "0.1.0-SNAPSHOT",
    name ~= ("fast-reload-"+_)
  )

  object AndroidSettings {
    def config = seq(
      platformName in Android := "android-11"
    )

    /**
     * Our pseudo AndroidSettings we need just the platform jar dependency
     * in the usual android-plugin configured way.
     */
    def myAndroidSettings = inConfig(Android)(Seq(
      jarPath <<= (platformPath, jarName) (_ / _),
      libraryJarPath <<= (jarPath (_ get)),
      platformPath <<= (sdkPath, platformName) (_ / "platforms" / _),
      sdkPath <<= (envs) { es =>
        AndroidHelpers.determineAndroidSdkPath(es).getOrElse(sys.error(
          "Android SDK not found. You might need to set %s".format(es.mkString(" or "))
        ))
      },
      unmanagedJars in Compile <++= (libraryJarPath) map (_.map(Attributed.blank(_)))
    )) ++ AndroidDefaults.settings
  }
}