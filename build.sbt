import java.io.FileOutputStream
import java.nio.file.Files

import com.amazonaws.regions.{Region, Regions}
import org.apache.commons.compress.archivers.zip.{ZipArchiveEntry, ZipArchiveOutputStream}

lazy val appName = "dog-eared"

lazy val catsVersion = "2.1.1"
lazy val http4sVersion = "0.21.4"
lazy val doobieVersion = "0.9.0"
lazy val calibanVersion = "0.8.2"

lazy val circeVersion = "0.12.3"
lazy val circeDependencies = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion)

lazy val dbHost = sys.env.getOrElse("DB_HOST", "localhost")
lazy val dbPort = sys.env.getOrElse("DB_PORT", "5432")
lazy val dbUser = sys.env.getOrElse("DB_USER", "postgres")
lazy val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")

val extraGraalvmNativeImageOptions = Seq(
  "-H:+ReportExceptionStackTraces",
  "-H:+TraceClassInitialization",
  "--static",
  "--verbose",
  "--allow-incomplete-classpath",
  "--no-fallback",
  "--initialize-at-build-time",
  "--enable-http",
  "--enable-https",
  "--enable-all-security-services",
)

lazy val loggingDependencies = Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "net.logstash.logback" % "logstash-logback-encoder" % "6.3",
)

val lambdaRuntimeTargetZip = taskKey[File]("lambda runtime zip")
val lambdaRuntime = taskKey[File]("create lambda runtime zip")

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

lazy val lambda = (project in file("lambda"))
  .enablePlugins(S3Plugin, GraalVMNativeImagePlugin)
  .settings(commonSettings)
  .settings(
    name := s"${appName}-lambda",
    libraryDependencies
      ++= circeDependencies
      ++ loggingDependencies,
    name in GraalVMNativeImage := "bootstrap",
    graalVMNativeImageOptions ++= extraGraalvmNativeImageOptions,
    lambdaRuntimeTargetZip := {
      val targetDirectory = target.value / "lambda-runtime"
      targetDirectory.mkdirs()
      targetDirectory / "runtime.zip"
    },
    lambdaRuntime := {
      val source = (target in GraalVMNativeImage).value / (name in GraalVMNativeImage).value
      val targetFile = lambdaRuntimeTargetZip.value
      val out = new ZipArchiveOutputStream(new FileOutputStream(targetFile))
      val entry = new ZipArchiveEntry("bootstrap")
      entry.setUnixMode(0x1ed)
      out.putArchiveEntry(entry)
      Files.copy(source.toPath, out)
      out.closeArchiveEntry()
      out.close()
      targetFile
    },
    lambdaRuntime := (lambdaRuntime dependsOn (packageBin in GraalVMNativeImage)).value,
    s3Host in s3Upload := sys.env.getOrElse("LAMBDA_S3_HOST", "lambda-functions.jooohn.me.s3-website-ap-northeast-1.amazonaws.com"),
    s3Upload := (s3Upload dependsOn lambdaRuntime).value,
    mappings in s3Upload := Seq(
      lambdaRuntimeTargetZip.value -> s"${appName}/runtime"
    ),
  )
  .dependsOn(app)

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

lazy val app = (project in file("app"))
  .settings(commonSettings)
  .settings(
    name := s"${appName}-app",
    libraryDependencies ++= Seq(
      "is.cir" %% "ciris" % "1.0.4",
      "software.amazon.awssdk" % "secretsmanager" % "2.13.37",
    )
  )
  .dependsOn(useCases, drivenPorts, drivenAdapters, server)

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
    packageName in Docker := s"${appName}-cli",
    daemonUser in Docker := s"${appName}",
    dockerUpdateLatest := true,
    region in Ecr := Region.getRegion(Regions.AP_NORTHEAST_1),
    repositoryName in Ecr := (packageName in Docker).value,
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
  .dependsOn(useCases, drivenAdapters, app, cli, lambda)

