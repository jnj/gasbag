import sbt._

class Project(info: ProjectInfo) extends DefaultProject(info) {
  val commonsCodec = "commons-codec" % "commons-codec" % "1.4"
  val commonsConfig = "commons-configuration" % "commons-configuration" % "1.6"
}
