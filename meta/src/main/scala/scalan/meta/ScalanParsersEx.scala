package scalan.meta

import java.io.File
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.reporters.StoreReporter
import scala.reflect.internal.util.SourceFile

trait ScalanParsersEx extends ScalanParsers {
  val settings = new Settings
  settings.embeddedDefaults(getClass.getClassLoader)
  settings.usejavacp.value = true
  val reporter = new StoreReporter
  val compiler: Compiler = new Global(settings, reporter)

  // Credit due to Li Haoyi in Ammonite:
  // Initialize scalac to the parser phase immediately, so we can start
  // using Compiler#parse even if we haven't compiled any compilation
  // units yet due to caching
  val run = new compiler.Run()
  compiler.phase = run.parserPhase
  run.cancel()

  def parseEntityModule(file: File) = {
    val sourceFile = compiler.getSourceFile(file.getPath)
    val tree = parseFile(sourceFile)
    parse(file.getPath, tree)
  }

  def parseFile(source: SourceFile): compiler.Tree = {
    compiler.newUnitParser(new compiler.CompilationUnit(source)).parse()
  }
}
