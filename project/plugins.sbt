resolvers += Resolver.url("sbts3 ivy resolver", url("https://dl.bintray.com/emersonloureiro/sbt-plugins"))(
  Resolver.ivyStylePatterns)

addSbtPlugin("io.github.davidmweber" % "flyway-sbt" % "6.2.2")
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.5.0")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.10.0-RC1")
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.7.2")
addSbtPlugin("com.mintbeans" % "sbt-ecr" % "0.15.0")
addSbtPlugin("cf.janga" % "sbts3" % "0.10.3")
