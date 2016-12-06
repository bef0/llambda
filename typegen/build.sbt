name := "typegen"

version := "0.0.1"

organization := "io.llambda"

scalaVersion := "2.11.8"

scalacOptions ++= Seq("-deprecation", "-feature", "-Xlint")

libraryDependencies += "org.scalatest" %% "scalatest" % "2.2.4" % "test"

libraryDependencies += "com.github.scopt" %% "scopt" % "3.3.0"

libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3"
