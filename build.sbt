name := "cqrs-db"

version := "1.0"

scalaVersion := "2.11.8"


// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

val slickVersion = "3.2.0"

libraryDependencies ++= Seq(
  "net.glorat" %% "eventstore" % "0.1.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "com.typesafe.slick" %% "slick" % slickVersion,
  "org.slf4j" % "slf4j-nop" % "1.6.4",
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "com.h2database" % "h2" % "1.4.191"
)
