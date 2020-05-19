
lazy val catsVersion = "2.1.1"
lazy val http4sVersion = "0.21.4"
lazy val doobieVersion = "0.9.0"

lazy val dbHost = sys.env.getOrElse("DB_HOST", "localhost")
lazy val dbPort = sys.env.getOrElse("DB_PORT", "5432")
lazy val dbUser = sys.env.getOrElse("DB_USER", "postgres")
lazy val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.2",
  scalacOptions ++= Seq(
    "-language:higherKinds",
//    "-Yimports:java.lang,scala,scala.Predef,cats.implicits"
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % catsVersion,
    "org.typelevel" %% "simulacrum" % "1.0.0",
  ),
  addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.11.0" cross CrossVersion.full),
  test in assembly := {}
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
      "com.danielasfregola" %% "twitter4s" % "6.2",
      "net.ruippeixotog" % "scala-scraper_2.13" % "2.2.0",
      "org.scanamo" %% "scanamo" % "1.0.0-M12-1",
      "org.scanamo" %% "scanamo-cats-effect" % "1.0.0-M12-1",
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % http4sVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1",
      "ch.qos.logback" % "logback-classic" % "1.2.3",

    ),
    dependencyOverrides ++= Seq(
      "org.typelevel" %% "cats-core" % catsVersion,
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

lazy val app = (project in file("app"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-app",
    libraryDependencies ++= Seq(
      "is.cir" %% "ciris" % "1.0.4",
    )
  )
  .dependsOn(useCases, drivenPorts, drivenAdapters)

lazy val cli = (project in file("cli"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-cli",
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % "1.0.0",
      "com.monovore" %% "decline-effect" % "1.0.0",
    ),
  )
  .dependsOn(app)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    name := "dog-eared-tests",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.4.5" % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.7" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(useCases, drivenAdapters, app)

