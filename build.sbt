import com.jsuereth.sbtpgp.PgpKeys.publishSigned
import com.typesafe.tools.mima.core.{Problem, ProblemFilters}
import commandmatrix.extra.*

// Used to configure the build so that it would format on compile during development but not on CI.
lazy val isCI = sys.env.get("CI").contains("true")
ThisBuild / scalafmtOnCompile := !isCI

lazy val ciRelease = taskKey[Unit](
  "Publish artifacts to release or snapshot (skipping sonatypeBundleRelease which is unnecessary for snapshots)"
)
ciRelease := {
  publishSigned.taskValue
  Def.taskIf {
    if (git.gitCurrentTags.value.nonEmpty) {
      sonatypeBundleRelease.taskValue
    }
  }
}

// Versions:

val versions = new {
  val scala212 = "2.12.19"
  val scala213 = "2.13.14"
  val scala3 = "3.3.3"

  // Which versions should be cross-compiled for publishing
  val scalas = List(scala212, scala213, scala3)
  val platforms = List(VirtualAxis.jvm, VirtualAxis.js, VirtualAxis.native)

  // Which version should be used in IntelliJ
  val ideScala = scala3
  val idePlatform = VirtualAxis.jvm

  // Dependencies
  val catsEffect = "3.5.4"
  val monix = "3.4.0"
  val slf4j = "2.0.13"
}

// Common settings:

Global / excludeLintKeys += git.useGitDescribe
Global / excludeLintKeys += ideSkipProject
Global / excludeLintKeys += excludeDependencies
val only1VersionInIDE =
  MatrixAction
    .ForPlatform(versions.idePlatform)
    .Configure(
      _.settings(
        ideSkipProject := (scalaVersion.value != versions.ideScala),
        bspEnabled := (scalaVersion.value == versions.ideScala),
        scalafmtOnCompile := !isCI
      )
    ) +:
    versions.platforms.filter(_ != versions.idePlatform).map { platform =>
      MatrixAction
        .ForPlatform(platform)
        .Configure(_.settings(ideSkipProject := true, bspEnabled := false, scalafmtOnCompile := false))
    }

val settings = Seq(
  git.useGitDescribe := true,
  git.uncommittedSignifier := None,
  scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq(
          // format: off
          "-encoding", "UTF-8",
          "-rewrite",
          "-source", "3.3-migration",
          // format: on
          "-unchecked",
          "-deprecation",
          "-explain",
          "-explain-types",
          "-feature",
          "-Wconf:msg=object ImplicitConversions in package convert is deprecated:s", // 2.12 bincompat
          // "-Wnonunit-statement",
          // "-Wunused:imports", // import x.Underlying as X is marked as unused even though it is! probably one of https://github.com/scala/scala3/issues/: #18564, #19252, #19657, #19912
          "-Wunused:privates",
          "-Wunused:locals",
          "-Wunused:explicits",
          "-Wunused:implicits",
          "-Wunused:params",
          // "-Wvalue-discard",
          "-Xfatal-warnings",
          "-Xcheck-macros",
          "-Ykind-projector:underscores"
        )
      case Some((2, 13)) =>
        Seq(
          // format: off
          "-encoding", "UTF-8",
          "-release", "8",
          // format: on
          "-unchecked",
          "-deprecation",
          "-explaintypes",
          "-feature",
          "-language:higherKinds",
          "-Wconf:msg=object ImplicitConversions in package convert is deprecated:s", // 2.12 bincompat
          "-Wunused:patvars",
          "-Xfatal-warnings",
          "-Xlint:adapted-args",
          "-Xlint:delayedinit-select",
          "-Xlint:doc-detached",
          "-Xlint:inaccessible",
          "-Xlint:infer-any",
          "-Xlint:nullary-unit",
          "-Xlint:option-implicit",
          "-Xlint:package-object-classes",
          "-Xlint:poly-implicit-overload",
          "-Xlint:private-shadow",
          "-Xlint:stars-align",
          "-Xlint:type-parameter-shadow",
          "-Xsource:3",
          "-Ywarn-dead-code",
          "-Ywarn-numeric-widen",
          "-Ywarn-unused:locals",
          "-Ywarn-unused:imports",
          "-Ywarn-macros:after",
          "-Ytasty-reader"
        )
      case Some((2, 12)) =>
        Seq(
          // format: off
          "-encoding", "UTF-8",
          "-target:jvm-1.8",
          // format: on
          "-unchecked",
          "-deprecation",
          "-explaintypes",
          "-feature",
          "-language:higherKinds",
          "-Xexperimental",
          "-Xfatal-warnings",
          "-Xfuture",
          "-Xlint:adapted-args",
          "-Xlint:by-name-right-associative",
          "-Xlint:delayedinit-select",
          "-Xlint:doc-detached",
          "-Xlint:inaccessible",
          "-Xlint:infer-any",
          "-Xlint:nullary-override",
          "-Xlint:nullary-unit",
          "-Xlint:option-implicit",
          "-Xlint:package-object-classes",
          "-Xlint:poly-implicit-overload",
          "-Xlint:private-shadow",
          "-Xlint:stars-align",
          "-Xlint:type-parameter-shadow",
          "-Xlint:unsound-match",
          "-Xsource:3",
          "-Yno-adapted-args",
          "-Ywarn-dead-code",
          "-Ywarn-inaccessible",
          "-Ywarn-infer-any",
          "-Ywarn-numeric-widen",
          "-Ywarn-unused:locals",
          "-Ywarn-unused:imports",
          "-Ywarn-macros:after",
          "-Ywarn-nullary-override",
          "-Ywarn-nullary-unit"
        )
      case _ => Seq.empty
    }
  },
  Compile / doc / scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((3, _)) =>
        Seq("-Ygenerate-inkuire") // type-based search for Scala 3, this option cannot go into compile
      case _ => Seq.empty
    }
  },
  Compile / console / scalacOptions --= Seq("-Ywarn-unused:imports", "-Xfatal-warnings"),
  Test / compile / scalacOptions --= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 12)) => Seq("-Ywarn-unused:locals") // Scala 2.12 ignores @unused warns
      case _             => Seq.empty
    }
  }
)

val versionSchemeSettings = Seq(versionScheme := Some("early-semver"))

val publishSettings = Seq(
  organization := "io.scalaland",
  homepage := Some(url("https://scalaland.io/mdc4s")),
  organizationHomepage := Some(url("https://scalaland.io")),
  licenses := Seq("Apache-2.0" -> url("https://www.apache.org/licenses/LICENSE-2.0")),
  scmInfo := Some(
    ScmInfo(url("https://github.com/scalalandio/mdc4s/"), "scm:git:git@github.com:scalalandio/mdc4s.git")
  ),
  startYear := Some(2017),
  developers := List(
    Developer("MateuszKubuszok", "Mateusz Kubuszok", "", url("https://github.com/MateuszKubuszok"))
  ),
  pomExtra := (
    <issueManagement>
      <system>GitHub issues</system>
      <url>https://github.com/scalalandio/mdc4s/issues</url>
    </issueManagement>
  ),
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  // Sonatype ignores isSnapshot setting and only looks at -SNAPSHOT suffix in version:
  //   https://central.sonatype.org/publish/publish-maven/#performing-a-snapshot-deployment
  // meanwhile sbt-git used to set up SNAPSHOT if there were uncommitted changes:
  //   https://github.com/sbt/sbt-git/issues/164
  // (now this suffix is empty by default) so we need to fix it manually.
  git.gitUncommittedChanges := git.gitCurrentTags.value.isEmpty,
  git.uncommittedSignifier := Some("SNAPSHOT")
)

val mimaSettings = Seq(
  mimaPreviousArtifacts := {
    val previousVersions = moduleName.value match {
      case _ => Set()
    }
    previousVersions.map(organization.value %% moduleName.value % _)
  },
  mimaFailOnNoPrevious := false // before the first release
)

val noPublishSettings =
  Seq(publish / skip := true, publishArtifact := false)

val ciCommand = (platform: String, scalaSuffix: String) => {
  val isJVM = platform == "JVM"

  val clean = Vector("clean")
  def withCoverage(tasks: String*): Vector[String] =
    "coverage" +: tasks.toVector :+ "coverageAggregate" :+ "coverageOff"

  val projects = Vector("mdc4s")
    .map(name => s"$name${if (isJVM) "" else platform}$scalaSuffix")
  def tasksOf(name: String): Vector[String] = projects.map(project => s"$project/$name")

  val tasks = if (isJVM) {
    clean ++
      withCoverage((tasksOf("compile") ++ tasksOf("test") ++ tasksOf("coverageReport")).toSeq *) ++
      tasksOf("mimaReportBinaryIssues")
  } else {
    clean ++ tasksOf("test")
  }

  tasks.mkString(" ; ")
}

val publishLocalForTests = {
  for {
    module <- Vector("mdc4s", "mdc4s-cats-effect", "mdc4s-monix")
    moduleVersion <- Vector(module, module + "3")
  } yield moduleVersion + "/publishLocal"
}.mkString(" ; ")

val releaseCommand = (tag: Seq[String]) =>
  if (tag.nonEmpty) "publishSigned ; sonatypeBundleRelease" else "publishSigned"

lazy val root = project
  .in(file("."))
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .settings(
    moduleName := "mdc4s-build",
    name := "mdc4s-build",
    description := "Build setup for MDC4s modules",
    logo :=
      s"""MDC4s ${(version).value} build for (${versions.scala212}, ${versions.scala213}, ${versions.scala3}) x (Scala JVM, Scala.js $scalaJSVersion, Scala Native $nativeVersion)
         |
         |This build uses sbt-projectmatrix with sbt-commandmatrix helper:
         | - Scala JVM adds no suffix to a project name seen in build.sbt
         | - Scala.js adds the "JS" suffix to a project name seen in build.sbt
         | - Scala Native adds the "Native" suffix to a project name seen in build.sbt
         | - Scala 2.12 adds the suffix "2_12" to a project name seen in build.sbt
         | - Scala 2.13 adds no suffix to a project name seen in build.sbt
         | - Scala 3 adds the suffix "3" to a project name seen in build.sbt
         |
         |When working with IntelliJ or Scala Metals, edit "val ideScala = ..." and "val idePlatform = ..." within "val versions" in build.sbt to control which Scala version you're currently working with.
         |
         |If you need to test library locally in a different project, use publish-local-for-tests or manually publishLocal:
         | - mdc4s (obligatory)
         | - mdc4s-cats-effect (optional)
         | - mdc4s-monix (optional)
         |for the right Scala version and platform (see projects task).
         |""".stripMargin,
    usefulTasks := Seq(
      sbtwelcome.UsefulTask("projects", "List all projects generated by the build matrix").noAlias,
      sbtwelcome
        .UsefulTask(
          "test",
          "Compile and test all projects in all Scala versions and platforms (beware! it uses a lot of memory and might OOM!)"
        )
        .noAlias,
      sbtwelcome.UsefulTask("mdc4s-cats-effect3/console", "Drop into REPL with MDC4s imported (3)").noAlias,
      sbtwelcome.UsefulTask("mdc4s-cats-effect/console", "Drop into REPL with MDC4s imported (2.13)").noAlias,
      sbtwelcome
        .UsefulTask(releaseCommand(git.gitCurrentTags.value), "Publish everything to release or snapshot repository")
        .alias("ci-release"),
      sbtwelcome.UsefulTask(ciCommand("JVM", "3"), "CI pipeline for Scala 3 on JVM").alias("ci-jvm-3"),
      sbtwelcome.UsefulTask(ciCommand("JVM", ""), "CI pipeline for Scala 2.13 on JVM").alias("ci-jvm-2_13"),
      sbtwelcome.UsefulTask(ciCommand("JVM", "2_12"), "CI pipeline for Scala 2.12 on JVM").alias("ci-jvm-2_12"),
      sbtwelcome.UsefulTask(ciCommand("JS", "3"), "CI pipeline for Scala 3 on Scala JS").alias("ci-js-3"),
      sbtwelcome.UsefulTask(ciCommand("JS", ""), "CI pipeline for Scala 2.13 on Scala JS").alias("ci-js-2_13"),
      sbtwelcome.UsefulTask(ciCommand("JS", "2_12"), "CI pipeline for Scala 2.12 on Scala JS").alias("ci-js-2_12"),
      sbtwelcome.UsefulTask(ciCommand("Native", "3"), "CI pipeline for Scala 3 on Scala Native").alias("ci-native-3"),
      sbtwelcome
        .UsefulTask(ciCommand("Native", ""), "CI pipeline for Scala 2.13 on Scala Native")
        .alias("ci-native-2_13"),
      sbtwelcome
        .UsefulTask(ciCommand("Native", "2_12"), "CI pipeline for Scala 2.12 on Scala Native")
        .alias("ci-native-2_12"),
      sbtwelcome
        .UsefulTask(
          publishLocalForTests,
          "Publishes all Scala 2.13 and Scala 3 JVM artifacts to test snippets in documentation"
        )
        .alias("publish-local-for-tests")
    )
  )
  .settings(settings)
  .settings(versionSchemeSettings)
  .settings(publishSettings)
  .settings(noPublishSettings)
  .aggregate(
    (mdc4s.projectRefs ++ mdc4sCatsEffect.projectRefs ++ mdc4sMonix.projectRefs) *
  )

lazy val mdc4s = projectMatrix
  .in(file("modules/mdc4s"))
  .someVariations(versions.scalas, versions.platforms)((only1VersionInIDE) *)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .disablePlugins(WelcomePlugin)
  .settings(
    moduleName := "mdc4s",
    name := "mdc4s",
    description := "Common type class interface for various MDC implementations"
  )
  .settings(settings *)
  .settings(versionSchemeSettings *)
  .settings(publishSettings *)
  .settings(mimaSettings *)
  .settings(
    libraryDependencies += "org.scalameta" %%% "munit" % "1.0.0" % Test
  )

lazy val mdc4sCatsEffect = projectMatrix
  .in(file("modules/mdc4s-cats-effect"))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((only1VersionInIDE) *)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .disablePlugins(WelcomePlugin)
  .settings(
    moduleName := "mdc4s-cats-effect",
    name := "mdc4s-cats-effect",
    description := "MDC implementation for Cats Effect"
  )
  .settings(settings *)
  .settings(versionSchemeSettings *)
  .settings(publishSettings *)
  .settings(mimaSettings *)
  .settings(
    libraryDependencies += "org.typelevel" %% "cats-effect" % versions.catsEffect,
    libraryDependencies += "org.slf4j" % "slf4j-api" % versions.slf4j
  )
  .dependsOn(mdc4s)

lazy val mdc4sMonix = projectMatrix
  .in(file("modules/mdc4s-monix"))
  .someVariations(versions.scalas, List(VirtualAxis.jvm))((only1VersionInIDE) *)
  .enablePlugins(GitVersioning, GitBranchPrompt)
  .disablePlugins(WelcomePlugin)
  .settings(
    moduleName := "mdc4s-monix",
    name := "mdc4s-monix",
    description := "MDC implementation for Monix"
  )
  .settings(settings *)
  .settings(versionSchemeSettings *)
  .settings(publishSettings *)
  .settings(mimaSettings *)
  .settings(
    libraryDependencies += "io.monix" %% "monix" % versions.monix,
    libraryDependencies += "org.slf4j" % "slf4j-api" % versions.slf4j
  )
  .dependsOn(mdc4s)

addCommandAlias("fullTest", "test")
addCommandAlias("fullCoverageTest", "coverage ; test ; coverageReport ; coverageAggregate")
