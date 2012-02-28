//package net.virtualvoid.android.sbt

import sbt._
import Keys._

import AndroidKeys._

object FastReloaderPlugin extends Plugin {
  object FastReloaderKeys {
    implicit def multiArgManifest[T: Manifest]: Manifest[(T*) => Unit] =
      manifest[Seq[T] => Unit].asInstanceOf[Manifest[(T*) => Unit]]

    lazy val adbTask = TaskKey[(String*) => Unit]("adb-task", "the function to run something with adb")
    lazy val runOnEmulator = SettingKey[Boolean]("run-on-emulator", "If something should be run on the emulator")

    lazy val initAdbTask = (AndroidKeys.dbPath, streams, runOnEmulator) map { (dbPath, s, runOnEmulator) =>
      {(action: Seq[String]) =>
        println("Running '%s'" format action.mkString(" "))
        AndroidHelpers.adbTask(dbPath.absolutePath, runOnEmulator, s, action:_*)}.asInstanceOf[(String*) => Unit]
    }

    lazy val runInstrumented = TaskKey[Unit]("run-instrumented", "Run the adb with instrumentation enabled classpath loading")
    lazy val initRunInstrumented = (adbTask, manifestPackage in AndroidKeys.Android, launcherActivity in Android) map { (adb, packageName, act) =>
      adb("shell", "am", "instrument",
            "-e", "class", packageName+act,
            "%s/net.virtualvoid.android.runner.InstrumentationRunner" format packageName
      )
    }

    lazy val compileAndCopy = TaskKey[Unit]("compile-and-copy", "compile and install")

    lazy val launcherActivity = TaskKey[String]("launcher-activity", "The launcher activity as defined in the AndroidManifest.xml")
    lazy val initLauncherActivity = (manifestSchema, manifestPackage, manifestPath) map {
      (schema, pkg, path) =>
        AndroidHelpers.launcherActivity(schema, path.head, pkg)
    }

    lazy val settings = inConfig(AndroidKeys.Android)(Seq(
      runOnEmulator := false,
      adbTask <<= initAdbTask,
      runInstrumented <<= initRunInstrumented.dependsOn(compileAndCopy),
      compileAndCopy <<= (adbTask, AndroidKeys.packageDebug) map { (adb, pkg) =>
        adb("push", pkg.getAbsolutePath, "/sdcard/test.apk")
      },
      launcherActivity <<= initLauncherActivity,
      excludeFilter in mainResPath := (".*"  - ".") || HiddenFileFilter
    )) ++ seq(
      libraryDependencies += "net.virtual-void" % "fast-reload-runner" % "0.1.0-SNAPSHOT",
      watchSources <++= (mainResPath in Android, excludeFilter in mainResPath in Android) map {
        (sourceDir, excl) =>
           sourceDir.descendentsExcept("*.*", excl).get
      }
    )
  }
}
