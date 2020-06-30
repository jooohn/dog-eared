import com.amazonaws.regions.{Region, Regions}

val appName = "dog-eared"

val catsVersion = "2.1.1"
val shapelessVersion = "2.3.3"
val http4sVersion = "0.21.4"
val doobieVersion = "0.9.0"
val calibanVersion = "0.8.2"

val circeVersion = "0.12.3"
val circeDependencies = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion)

val dbHost = sys.env.getOrElse("DB_HOST", "localhost")
val dbPort = sys.env.getOrElse("DB_PORT", "5432")
val dbUser = sys.env.getOrElse("DB_USER", "postgres")
val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")

lazy val loggingDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.3",
)

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

lazy val graphql = (project in file("graphql"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-graphql",
    libraryDependencies ++= circeDependencies ++ Seq(
      "com.github.ghostdogpr" %% "caliban" % calibanVersion,
      "com.github.ghostdogpr" %% "caliban-cats" % calibanVersion,
    ),
  )
  .dependsOn(useCases, drivenAdapters)

lazy val server = (project in file("server"))
  .enablePlugins(S3Plugin, GraalVMNativeImagePlugin)
  .settings(commonSettings)
  .settings(
    name := s"${appName}-server",
    libraryDependencies ++= loggingDependencies ++ Seq(
      "com.github.ghostdogpr" %% "caliban-http4s" % calibanVersion,
    ),
  )
  .dependsOn(useCases, drivenAdapters, graphql)

lazy val domain = (project in file("domain"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-domain"
  )

lazy val useCases = (project in file("use-cases"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-use-cases"
  )
  .dependsOn(domain, drivenPorts)

lazy val drivenPorts = (project in file("driven-ports"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-driven-ports"
  )
  .dependsOn(domain)

lazy val drivenAdapters = (project in file("driven-adapters"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-driven-adapters",
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
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.chrisdavenport" %% "log4cats-slf4j" % "1.1.1",
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

lazy val di = (project in file("di"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-di",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "com.chuusai" %% "shapeless" % shapelessVersion,
    ),
  )

lazy val app = (project in file("app"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-app",
    libraryDependencies ++= Seq(
      "is.cir" %% "ciris" % "1.0.4",
      "software.amazon.awssdk" % "secretsmanager" % "2.13.37",
    )
  )
  .dependsOn(useCases, drivenPorts, drivenAdapters, server, di)

lazy val cli = (project in file("cli"))
  .enablePlugins(JavaAppPackaging, DockerPlugin, EcrPlugin)
  .settings(commonSettings)
  .settings(
    name := s"${appName}-cli",
    libraryDependencies ++= Seq(
      "com.monovore" %% "decline" % "1.0.0",
      "com.monovore" %% "decline-effect" % "1.0.0",
    ) ++ loggingDependencies,
    dockerBaseImage := "openjdk:11",
    dockerExposedPorts := List(8080),
    packageName in Docker := s"${appName}-cli",
    daemonUser in Docker := s"${appName}",
    dockerUpdateLatest := true,
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryName in Ecr := appName,
    repositoryTags in Ecr ++= Seq(version.value),
    localDockerImage in Ecr := (packageName in Docker).value + ":" + (version in Docker).value,
    push in Ecr := ((push in Ecr) dependsOn (publishLocal in Docker, login in Ecr)).value,
  )
  .dependsOn(app)

lazy val tests = (project in file("tests"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-tests",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.4.5" % Test,
      "org.scalameta" %% "munit-scalacheck" % "0.7.7" % Test
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
  .dependsOn(useCases, drivenAdapters, app, cli)

