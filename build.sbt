name := "cqrs-db"

version := "1.0"

scalaVersion := "2.11.8"


// resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"


libraryDependencies ++= Seq(
  "net.glorat" %% "eventstore" % "0.1.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test",
  "com.novocode" % "junit-interface" % "0.10" % "test"
)
