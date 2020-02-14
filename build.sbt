
lazy val catsVersion = "2.1.0"
lazy val doobieVersion = "0.8.8"

lazy val dbHost = sys.env.getOrElse("DB_HOST", "localhost")
lazy val dbPort = sys.env.getOrElse("DB_PORT", "5432")
lazy val dbUser = sys.env.getOrElse("DB_USER", "postgres")
lazy val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.1",
  scalacOptions ++= Seq(
    "-language:higherKinds",
//    "-Yimports:java.lang,scala,scala.Predef,cats.implicits"
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "simulacrum" % "1.0.0",
  ),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
)
lazy val server = (project in file("server"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-server",
  )
  .dependsOn(useCases, drivenAdapters)

lazy val domain = (project in file("domain"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-domain"
  )

lazy val useCases = (project in file("use-cases"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-use-cases"
  )
  .dependsOn(domain, drivenPorts)

lazy val drivenPorts = (project in file("driven-ports"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-driven-ports"
  )
  .dependsOn(domain)

lazy val drivenAdapters = (project in file("driven-adapters"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-driven-adapters",
    libraryDependencies ++= Seq(
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres"  % doobieVersion,
    ),
    flywayUrl := s"jdbc:postgresql://${dbHost}:${dbPort}/dog_eared",
    flywayUser := dbUser,
    flywayPassword := dbPassword,
    flywayUrl in Test := s"jdbc:postgresql://${dbHost}:${dbPort}/dog_eared_test",
    flywayUser in Test := dbUser,
    flywayPassword in Test := dbPassword,
  )
  .dependsOn(drivenPorts)
  .enablePlugins(FlywayPlugin)

lazy val config = (project in file("config"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-config",
    libraryDependencies ++= Seq(
      "is.cir" %% "ciris" % "1.0.4",
    )
  )

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-tests",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.4.5"
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(useCases, drivenAdapters, config)

