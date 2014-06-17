name := "quality"

// for now we need to build on 2.10.4 instead of 2.11.1
// because sbt 0.13.5 is compiled on 2.10.4, and the
// sbt plugin can only be compiled by that version,
// due to incompatibilities around macros
scalaVersion in ThisBuild := "2.10.4"

lazy val api = project
  .in(file("api"))
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(commonPlaySettings: _*)
  .settings(
    version := "1.0-SNAPSHOT",
    libraryDependencies ++= Seq(
      jdbc,
      anorm,
      ws,
      "org.postgresql" % "postgresql" % "9.3-1101-jdbc4"
    )
  )

lazy val www = project
  .in(file("www"))
  .dependsOn(core)
  .aggregate(core)
  .enablePlugins(PlayScala)
  .settings(commonSettings: _*)
  .settings(
    version := "1.0-SNAPSHOT"
  )

lazy val commonSettings: Seq[Setting[_]] = Seq(
  name <<= name("quality-" + _),
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "2.1.7" % "test"
  ),
  scalacOptions += "-feature"
)
