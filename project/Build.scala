import BuildSettings.{project => _, _}
import com.typesafe.sbt.packager.Keys.scriptClasspath
import com.typesafe.sbt.web.SbtWeb.autoImport._
import play.Play.autoImport._
import play.sbt.PlayImport._
import play.twirl.sbt.Import._
import PlayKeys._
import sbt._
import Keys._

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val root = Project("lila", file("."))
    .enablePlugins(_root_.play.sbt.PlayScala)
    .dependsOn(api)
    .aggregate(api)
    .settings(Seq(
      scalaVersion := globalScalaVersion,
      resolvers ++= Dependencies.Resolvers.commons,
      scalacOptions := compilerOptions,
      incOptions := incOptions.value.withNameHashing(true),
      updateOptions := updateOptions.value.withCachedResolution(true),
      sources in doc in Compile := List(),
      // disable publishing the main API jar
      publishArtifact in (Compile, packageDoc) := false,
      // disable publishing the main sources jar
      publishArtifact in (Compile, packageSrc) := false,
      // don't stage the conf dir
      externalizeResources := false,
      // shorter prod classpath
      scriptClasspath := Seq("*"),
      // offline := true,
      libraryDependencies ++= Seq(
        scalaz, scalalib, hasher, config, apache,
        jgit, findbugs, RM, RPM, akka.actor, akka.slf4j,
        spray.caching, maxmind, spray.json,
        kamon.core, kamon.statsd, bigPipe,  java8compat, semver, scrimage),
      TwirlKeys.templateImports ++= Seq(
        "lila.user.{ User, UserContext }",
        "lila.security.Permission",
        "lila.app.templating.Environment._",
        "lila.api.Context",
        "lila.common.paginator.Paginator"),
      TwirlKeys.templateFormats ++= Map("stream" -> "com.ybrikman.ping.scalaapi.bigpipe.HtmlStreamFormat"),
      TwirlKeys.templateImports ++= Vector("com.ybrikman.ping.scalaapi.bigpipe.HtmlStream", "com.ybrikman.ping.scalaapi.bigpipe._"),
      watchSources <<= sourceDirectory in Compile map { sources =>
        (sources ** "*").get
      }
      // trump sbt-web into not looking at public/
//      resourceDirectory in Assets := (sourceDirectory in Compile).value / "assets"
    ))

  lazy val modules = Seq(
    common, db, user, security, hub, socket, i18n, game, site, relation, pref, notification,
    question, activity, chatRoom, userMessage, vocab, counter
  )

  lazy val moduleRefs = modules map projectToRef
  lazy val moduleCPDeps = moduleRefs map { new sbt.ClasspathDependency(_, None) }

  lazy val api = project("api", moduleCPDeps)
    .settings(
      libraryDependencies ++= provided(
        play.api, hasher, config, apache, jgit, findbugs, RM,
        kamon.core, kamon.statsd)
    ) aggregate (moduleRefs: _*)


  lazy val common = project("common").settings(
    libraryDependencies ++= provided(play.api, play.test, RM, kamon.core)
  )


  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(play.test, play.api, RM, hasher)
  )

  lazy val memo = project("memo", Seq(common, db)).settings(
    libraryDependencies ++= Seq(guava, findbugs, spray.caching) ++ provided(play.api, RM)
  )


  lazy val user = project("user", Seq(common, memo, db, hub)).settings(
    libraryDependencies ++= provided(play.api, play.test, RM, hasher)
  )

  lazy val game = project("game", Seq(common, memo, db, hub, user)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )

  lazy val security = project("security", Seq(common, hub, db, user)).settings(
    libraryDependencies ++= provided(play.api, RM, maxmind, hasher)
  )


  lazy val relation = project("relation", Seq(common, db, memo, hub, user, game, pref)).settings(
    libraryDependencies ++= provided(play.api, RM, RPM)
  )

  lazy val pref = project("pref", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )


  lazy val i18n = project("i18n", Seq(common, db, user, hub)).settings(
    sourceGenerators in Compile += Def.task {
      MessageCompiler(
        (baseDirectory in Compile).value / "messages",
        (sourceManaged in Compile).value / "messages"
      )
    }.taskValue,
    libraryDependencies ++= provided(play.api, RM, jgit)
  )

  lazy val notification = project("notification", Seq(common, user, hub)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val site = project("site", Seq(common, socket)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val socket = project("socket", Seq(common, hub, memo)).settings(
    libraryDependencies ++= provided(play.api)
  )


  lazy val hub = project("hub", Seq(common)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val activity = project("activity", Seq(common, db, memo, hub, user, pref, relation, counter)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )

  lazy val question = project("question", Seq(common, db, memo, hub, user, pref, relation, counter)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )

  lazy val chatRoom = project("chatRoom", Seq(common, db, memo, hub, user, counter)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )

  lazy val userMessage = project("userMessage", Seq(common, db, memo, hub, user, counter)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )

  lazy val vocab = project("vocab" , Seq(common, db, memo, hub, user, counter)).settings(
    libraryDependencies ++= provided(play.api, RM, RPM, spray.json)
  )

  lazy val counter = project("counter", Seq(common, db, memo, hub)).settings(
    libraryDependencies ++= provided(play.api, RM, RPM)
  )

}
