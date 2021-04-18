name := "scalajs-cqrs"

version := "0.1"

scalaVersion := "2.13.5"

enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin, UniversalPlugin)

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  // Explain type errors in more detail.
  "-explaintypes",
  // Warn when we use advanced language features
  "-feature",
  // Give more information on type erasure warning
  "-unchecked",
  // Enable warnings and lint
  "-Ywarn-unused",
  "-Xlint"
)

version in webpack := "4.16.1"
//useYarn := true // this requires yarn installed
webpackConfigFile := Some(baseDirectory.value / "webpack.config.js")
version in startWebpackDevServer := "3.1.4"

// Optional: Disable source maps to speed up compile times
//scalaJSLinkerConfig ~= { _.withSourceMap(false) }

// Include type defintion for aws lambda handlers
libraryDependencies += "net.exoego" %%% "aws-lambda-scalajs-facade" % "0.11.0"

// Optional: Include the AWS SDK as a dep
val awsSdkVersion              = "2.798.0"
val awsSdkScalajsFacadeVersion = s"0.32.0-v${awsSdkVersion}"
libraryDependencies += "net.exoego"    %%% "aws-sdk-scalajs-facade-sns"      % awsSdkScalajsFacadeVersion
libraryDependencies += "net.exoego"    %%% "aws-sdk-scalajs-facade-dynamodb" % awsSdkScalajsFacadeVersion
libraryDependencies += "net.exoego"    %%% "aws-sdk-scalajs-facade-kms"      % awsSdkScalajsFacadeVersion
npmDependencies in Compile += "aws-sdk" -> awsSdkVersion

// Optional: Include some nodejs types (useful for, say, accessing the env)
//libraryDependencies += "net.exoego" %%% "scala-js-nodejs-v12" % "0.9.1"

// Include scalatest
libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.2" % "test" // %%% (for scala.js lib) not %% (normal scala lib)

// Other dependencies
val monixVersion = "3.2.2"
libraryDependencies += "io.monix" %%% "monix" % monixVersion
val circeVersion = "0.14.0-M4"
libraryDependencies += "io.circe" %%% "circe-core"    % circeVersion
libraryDependencies += "io.circe" %%% "circe-generic" % circeVersion
libraryDependencies += "io.circe" %%% "circe-parser"  % circeVersion
val enumeratumVersion = "1.6.1"
libraryDependencies += "com.beachape" %%% "enumeratum-circe" % enumeratumVersion

npmDependencies in Compile += "libphonenumber-js" -> "1.9.12"
npmDependencies in Compile += "uuid"              -> "8.3.2"
resolvers += "jitpack" at "https://jitpack.io"
// Package lambda as a zip. Use `universal:packageBin` to create the zip
topLevelDirectory := None
mappings in Universal ++= (webpack in (Compile, fullOptJS)).value.map { f =>
  // remove the bundler suffix from the file names
  f.data -> f.data.getName().replace("-opt-bundle", "")
}
