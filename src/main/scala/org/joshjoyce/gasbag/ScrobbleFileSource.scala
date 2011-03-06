package org.joshjoyce.gasbag

import scala.io.Source

class ScrobbleFileSource(file: String) extends Traversable[SongInfo] {

  override def foreach[B](f: (SongInfo) => B) {
    val source = Source.fromFile(file)
    
    val lines = source.getLines
    // header lines
    lines.drop(3)

    lines.foreach {
      line => {
        val fields = line.split("\t")
        val artist = fields(0)
        val album = fields(1) match {
          case "" => None
          case s => Some(s)
        }
        val title = fields(2)
        val trackNum = fields(3) match {
          case "" => None
          case s => Some(s)
        }
        val durationInSeconds = Some(fields(4))
        val rating = fields(5)
        val timestamp = fields(6).toInt
        val mbid = if (fields.size > 7) Some(fields(7)) else None
        
        if (rating == "L") { // not skipped
          val song = SongInfo(artist, title, album, durationInSeconds, trackNum, mbid, timestamp)
          f(song)
        }
      }
    }
  }
}
