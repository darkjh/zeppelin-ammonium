package me.juhanlol

import java.util
import java.util.Properties

import ammonite.api._
import ammonite.Interpreter
import ammonite.util.Classpath
import ammonite.api.CodeItem.{Definition, Identity, LazyIdentity, Import => CImport}
import ammonite.interpreter.Colors

import coursier._

import org.apache.zeppelin.interpreter.Interpreter.FormType
import org.apache.zeppelin.interpreter.{Interpreter => ZInterpreter, InterpreterContext, InterpreterResult}

import scala.collection.JavaConversions._


class Ammonium(prop: Properties) extends ZInterpreter(prop) {
  override def getProgress(interpreterContext: InterpreterContext): Int = 0

  override def cancel(interpreterContext: InterpreterContext): Unit = {}

  override def close(): Unit = {}

  override def getFormType: FormType = FormType.NATIVE

  override def interpret(line: String,
                         interpreterContext: InterpreterContext): InterpreterResult = {
    val result = AmmoniumInterpreter.interpret(line)
    val code = result match {
      case ("err", _) => InterpreterResult.Code.ERROR
      case ("ok", _) => InterpreterResult.Code.SUCCESS
      case _  => InterpreterResult.Code.INCOMPLETE
    }

    new InterpreterResult(code, result._2)
  }

  override def open(): Unit = {}

  override def completion(s: String, i: Int): util.List[String] = null
}


object WebDisplay {

  /* For now, identical to ammonite.shell.ShellDisplay */

  def apply(d: CodeItem, colors: Colors): String =
    d match {
      case Definition(label, name) =>
        s""" Iterator("defined ", "${colors.`type`()}", "$label", " ", "${colors.ident()}", "$name", "${colors.reset()}") """
      case Identity(ident) =>
        s"""BridgeHolder.shell.printValue($$user.$ident, $$user.$ident, "$ident", _root_.scala.None)"""
      case LazyIdentity(ident) =>
        s"""BridgeHolder.shell.printValue($$user.$ident, $$user.$ident, "$ident", _root_.scala.Some("<lazy>"))"""
      case CImport(imported) =>
        s""" Iterator("${colors.`type`()}", "import ", "${colors.ident()}", "$imported", "${colors.reset()}") """
    }

}



object AmmoniumInterpreter {
  val scalaVersion = scala.util.Properties.versionNumberString
  val scalaBinaryVersion =
    scala.util.Properties.versionNumberString.split('.')
    .take(2).mkString(".")

  val defaultLoader = Thread.currentThread().getContextClassLoader

  val compileLoader = Classpath.isolatedLoader(defaultLoader, "jupyter-scala-compile").getOrElse(defaultLoader)
  val macroLoader = Classpath.isolatedLoader(defaultLoader, "jupyter-scala-macro").getOrElse(compileLoader)

  val initialDependencies = Seq(
//    "compile" -> Dependency(
//      Module("com.github.alexarchambault.jupyter", s"scala-api_$scalaVersion"), BuildInfo.version
//    ),
    "macro" -> Dependency(
      Module("org.scala-lang", "scala-compiler"), scalaVersion
    )
  ) ++ {
    if (scalaVersion.startsWith("2.10."))
      Seq(
        "compile" -> Dependency(
          Module("org.scalamacros", "quasiquotes_2.10"), "2.0.1"
        ),
        "compile" -> Dependency(
          Module("org.scala-lang", "scala-compiler"), scalaVersion
        )
      )
    else
      Seq()
  }

  val initialRepositories = Seq(
    coursier.Cache.ivy2Local,
    MavenRepository("https://repo1.maven.org/maven2"),
    MavenRepository("https://oss.sonatype.org/content/repositories/releases")
  ) ++ {
//    if (BuildInfo.version.endsWith("-SNAPSHOT")) Seq(
//      MavenRepository("https://oss.sonatype.org/content/repositories/snapshots")
//    ) else Nil
    Nil
  }

  lazy val classLoaders0 = Map(
    "runtime" -> compileLoader,
    "compile" -> compileLoader,
    "macro" -> macroLoader,
    "plugin" -> defaultLoader
  )

  val configs = Map(
    "compile" -> Nil,
    "runtime" -> Seq("compile"),
    "macro" -> Seq("compile"),
    "plugin" -> Nil
  )

  lazy val classpath0: Classpath = new Classpath(
    initialRepositories,
    initialDependencies,
    classLoaders0,
    configs,
    Interpreter.initCompiler()(intp)
  )

  def print0(items: Seq[CodeItem], colors: Colors): String =
      s""" Iterator[Iterator[String]](${items.map(WebDisplay(_, colors)).mkString(", ")}).filter(_.nonEmpty).flatMap(_ ++ Iterator("\\n")) """

  lazy val intp = new Interpreter(
    imports = new ammonite.interpreter.Imports(useClassWrapper = true),
    classpath = classpath0
  ) {
    def hasObjWrapSpecialImport(d: ParsedCode): Boolean =
      d.items.exists {
        case CodeItem.Import("special.wrap.obj") => true
        case _                                   => false
      }

    override def wrap(
                       decls: Seq[ParsedCode],
                       imports: String,
                       unfilteredImports: String,
                       wrapper: String
                       ) = {
      // FIXME More or less the same thing in ammonium...

      val (doClassWrap, decls0) =
        if (decls.exists(hasObjWrapSpecialImport))
          (false, decls.filterNot(hasObjWrapSpecialImport))
        else
          (true, decls)

      if (doClassWrap)
        Interpreter.classWrap(print0(_, Colors.BlackWhite), decls0, imports, unfilteredImports, wrapper)
      else
        Interpreter.wrap(print0(_, Colors.BlackWhite), decls0, imports, unfilteredImports, "special" + wrapper)
    }
  }

  def interpret(code: String): (String, String)= {
    val run = Interpreter.run(
      code,
      {},
      None,
      None,
      it => it.asInstanceOf[Iterator[String]].mkString.stripSuffix("\n")
    )

    run(intp) match {
      case Left(err) =>
        ("err", err.msg)
      case Right(Evaluated(_, _, data)) =>
        ("ok", data)
    }
  }
}
