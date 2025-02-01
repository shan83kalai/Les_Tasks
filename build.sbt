ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

Compile / resourceDirectory := baseDirectory.value / "src" / "main" / "resources"
Test / resourceDirectory := baseDirectory.value / "src" / "test" / "resources"

lazy val root = (project in file("."))
  .settings(
    name := "assignments",
    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-api" % "2.0.16",
      "ch.qos.logback" % "logback-classic" % "1.5.16",
      "org.yaml" % "snakeyaml" % "2.3",
      "com.google.code.gson" % "gson" % "2.12.1",
      "org.jsoup" % "jsoup" % "1.18.3",
      "com.google.guava" % "guava" % "33.4.0-jre",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0",
      "org.apache.tika" % "tika-core" % "3.1.0",
      "org.apache.commons" % "commons-imaging" % "1.0.0-alpha5",
      "dev.brachtendorf" % "JImageHash" % "1.0.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.scalatestplus" %% "mockito-5-12" % "3.2.19.0" % Test
    )
  )