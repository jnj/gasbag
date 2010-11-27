import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) with ProguardProject {
  val commonsCodec = "commons-codec" % "commons-codec" % "1.4"
  val commonsConfig = "commons-configuration" % "commons-configuration" % "1.6"
  
  override val artifactID = "gasbag"
  
  override def mainClass: Option[String] = None
  
  override def proguardOptions = List(
    "-dontoptimize",
    "-dontobfuscate",
    proguardKeepLimitedSerializability,
    proguardKeepAllScala,
    "-keep interface scala.ScalaObject",
    "-keep class org.joshjoyce.gasbag.Gasbag",
    "-dontshrink"
  )
    
  override def proguardInJars = Path.fromFile(scalaLibraryJar) +++ super.proguardInJars
}
