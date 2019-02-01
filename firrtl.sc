import mill._
import mill.scalalib._
import mill.modules.Jvm._
import mill.define.Task
import ammonite.ops._
import ammonite.ops.ImplicitWd._

import $ivy.`com.github.os72:protoc-jar:3.5.1`
import $ivy.`org.antlr:antlr4:4.7.1`
import $file.^.`scala-wake`.common, common._

trait FirrtlBase extends SbtModule with CommonOptions with BuildInfo {
  override def ivyDeps = Agg(
    ivy"com.typesafe.scala-logging::scala-logging:3.9.0",
    ivy"ch.qos.logback:logback-classic:1.2.3",
    ivy"com.github.scopt::scopt:3.7.0",
    ivy"net.jcazevedo::moultingyaml:0.4.0",
    ivy"org.json4s::json4s-native:3.6.1",
    ivy"org.antlr:antlr4-runtime:4.7.1",
    ivy"${ProtobufConfig.ivyDep}"
  )
  override def millSourcePath = os.pwd / up / 'firrtl

  def antlrSourceRoot = T.sources{ millSourcePath / 'src / 'main / 'antlr4 }

  def generateAntlrSources(p: Path, sourcePath: Path) = {
    val antlr = new Antlr4Config(sourcePath)
    mkdir! p
    antlr.runAntlr(p)
    p
  }

  def protobufSourceRoot = T.sources{ millSourcePath / 'src / 'main / 'proto }

  def generateProtobufSources(p: Path, sourcePath: Path) = {
    val protobuf = new ProtobufConfig(sourcePath)
    mkdir! p
    protobuf.runProtoc(p)
    p
  }

  override def generatedSources = T {
    val antlrSourcePath: Path = antlrSourceRoot().head.path
    val antlrSources = Seq(PathRef(generateAntlrSources(T.ctx().dest/'antlr, antlrSourcePath)))
    val protobufSourcePath: Path = protobufSourceRoot().head.path
    val protobufSources = Seq(PathRef(generateProtobufSources(T.ctx().dest/'proto, protobufSourcePath)))
    protobufSources ++ antlrSources
  }

  override def buildInfoMembers = T {
    Map[String, String](
      "buildInfoPackage" -> artifactName(),
      "version" -> "1.2-SNAPSHOT",
      "scalaVersion" -> scalaVersion()
    )
  }

  case class Antlr4Config(val sourcePath: Path) {
    val antlr4GenVisitor: Boolean = true
    val antlr4GenListener: Boolean = false
    val antlr4PackageName: Option[String] = Some("firrtl.antlr")
    val antlr4Version: String = "4.7"

    val listenerArg: String = if (antlr4GenListener) "-listener" else "-no-listener"
    val visitorArg: String = if (antlr4GenVisitor) "-visitor" else "-no-visitor"
    val packageArg: Seq[String] = antlr4PackageName match {
      case Some(p) => Seq("-package", p)
      case None => Seq.empty
    }
    def runAntlr(outputPath: Path) = {
      val args = (Seq[String]("-o", outputPath.toString, "-lib", sourcePath.toString, listenerArg, visitorArg) ++ packageArg :+ (sourcePath / "FIRRTL.g4").toString) ++ packageArg :+ (sourcePath / "FIRRTL.g4").toString
      val antlr = new org.antlr.v4.Tool(args.toArray)
      antlr.processGrammarsOnCommandLine() // Can error
    }
  }
}

case object ProtobufConfig {
  val version: String = "3.5.0"
  val ivyDep: String = s"com.google.protobuf:protobuf-java:${version}"
  def apply(sourcePath: Path): ProtobufConfig = new ProtobufConfig(sourcePath)
}

case class ProtobufConfig(val sourcePath: Path) {
  // Regex for protobuf source files.
  val protobufIncludeFilter = """.+\.proto""".r
  val protobufIncludePaths = Seq[Path](sourcePath)

  private[this] def executeProtoc(schemas: Set[Path], includePaths: Seq[Path], protocOptions: Seq[String]) : Int =
    try {
      val incPath = includePaths.map("-I" + _)
      val args = Seq[String]("-v351") ++ incPath ++ protocOptions ++ schemas.map(_.toString)
      com.github.os72.protocjar.Protoc.runProtoc(args.toArray)
    } catch { case e: Exception =>
      throw new RuntimeException("error occurred while compiling protobuf files: %s" format(e.getMessage), e)
    }

  def runProtoc(outputPath: Path): Unit = {
    // Find the protobuf source files we need to compile.
    val schemas = ls! sourcePath |? (f => protobufIncludeFilter.pattern.matcher(f.last).matches())
    if (!schemas.isEmpty) {
      executeProtoc(
        schemas = schemas.toSet,
        includePaths = protobufIncludePaths,
        protocOptions = Seq[String](s"--java_out=${outputPath.toString}")
      )
    } else {
      throw new RuntimeException(s"""mill.Protobuf: No schemas in ${sourcePath} matching "${protobufIncludeFilter.toString()}".r""")
    }
  }

}

