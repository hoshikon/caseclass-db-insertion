name := "caseclass-db-insertion"

version := "0.1"

scalaVersion := "2.13.6"

libraryDependencies ++= Seq(
  "com.chuusai" %% "shapeless" % "2.3.7",
  "org.tpolecat" %% "doobie-core" % "0.13.4",
  "org.scalatest" %% "scalatest" % "3.2.9" % "test"
)
