package org.joshjoyce.gasbag

import dispatch._
import java.util.Date

import org.apache.commons.codec.digest.DigestUtils._
import org.apache.commons.configuration.PropertiesConfiguration

case class SongInfo(artist: String, trackName: String, album: Option[String],
                    lengthInSeconds: Option[String], trackNum: Option[String],
                    mbid: Option[String], startPlayingTime: Int)

class GasbagScrobbleSession(val sessionId: String,
                            val nowPlayingUrl: String,
                            val submissionUrl: String) {
  
  override def toString() = {
    "%s / %s / %s".format(sessionId, nowPlayingUrl, submissionUrl)
  }
}

object Gasbag {
  def main(args: Array[String]) {
    if (args.size >= 3) {
      val fileSource = new ScrobbleFileSource(args.head)
      val gb = new Gasbag
      val session = gb.handshake(args(1), args(2))

      if (!session.isDefined) {
        println("Session was not obtained")
        System.exit(1)
      }
        
      fileSource.foreach {
        gb.submit(session.get, _)
      }
    } else {
      println("arguments: <file> <username> <password>")
      System.exit(1)
    }
  }
}

class Gasbag {
  import scala.collection.JavaConversions._

  val config = new PropertiesConfiguration("lastfm.properties")
  val apiKey = config.getString("api-key")
  val apiSecret = config.getString("api-secret")

  def handshake(user: String, pass: String): Option[GasbagScrobbleSession] = {
    val clientId = "tst"
    val clientVersion = "1.0"
    val ts = timestamp
    val auth = md5Hex(md5Hex(pass) + ts.toString)
    val host = :/("post.audioscrobbler.com")
    val h = new Http
    val req = host <<? List(
      ("hs", "true"),
      ("p", "1.2.1"),
      ("c", clientId),
      ("v", clientVersion),
      ("u", user),
      ("t", ts.toString),
      ("a", auth),
      ("api_key", apiKey)
    )

    h(req >- {
      body => {
        val lines = body.lines.drop(1).toList
        Some(new GasbagScrobbleSession(lines(0), lines(1), lines(2)))
      }
    })
  }
  
  def nowPlaying(session: GasbagScrobbleSession, song: SongInfo) = {
    val h = new Http
    val req = url(session.nowPlayingUrl) << List(
      ("s", session.sessionId),
      ("a", song.artist),
      ("t", song.trackName),
      ("b", song.album.getOrElse("")),
      ("l", song.lengthInSeconds.getOrElse("")),
      ("n", song.trackNum.getOrElse("")),
      ("m", song.mbid.getOrElse(""))
    )
    h(req >- {
      body => {
        val lines = body.lines
        lines.next().trim == "OK"
      }
    })
  }

  def submit(session: GasbagScrobbleSession, song: SongInfo) = {
    val h = new Http
    val req = url(session.submissionUrl) << List(
      ("s", session.sessionId),
      ("a[0]", song.artist),
      ("t[0]", song.trackName),
      ("i[0]", song.startPlayingTime.toString),
      ("o[0]", "P"),
      ("r[0]", ""),
      ("l[0]", song.lengthInSeconds.get),
      ("b[0]", song.album.getOrElse("")),
      ("n[0]", song.trackNum.getOrElse("")),
      ("m[0]", song.mbid.getOrElse(""))
    )

    h(req >- {
      body => {
        val lines = body.lines
        lines.next().trim == "OK"
      }
    })
  }

  def timestamp = new Date().getTime / 1000
}
