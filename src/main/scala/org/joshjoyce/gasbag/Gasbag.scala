package org.joshjoyce.gasbag

import com.zenkey.net.prowser._

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
    val hostPart = "http://post.audioscrobbler.com/"
    val prowser = new Prowser
    val tab = prowser.createTab
    val req = new Request(hostPart)
    req.addParameter("hs", "true")
    req.addParameter("p", "1.2.1")
    req.addParameter("c", clientId)
    req.addParameter("v", clientVersion)
    req.addParameter("u", user)
    req.addParameter("t", ts.toString)
    req.addParameter("a", auth)
    req.addParameter("api_key", apiKey)
    val response = tab.go(req)
    
    if (200 == response.getStatus) {
      val text = response.getPageSource
      val lines = text.trim.lines.drop(1).toList
      Some(new GasbagScrobbleSession(lines(0), lines(1), lines(2)))
    } else
      None
  }
  
  def nowPlaying(session: GasbagScrobbleSession, song: SongInfo) = {
    val p = new Prowser
    val tab = p.createTab
    val req = new Request(session.nowPlayingUrl)
    req.setHttpMethod("POST")
    req.addParameter("s", session.sessionId)
    req.addParameter("a", song.artist)
    req.addParameter("t", song.trackName)
    req.addParameter("b", song.album.getOrElse(""))
    req.addParameter("l", song.lengthInSeconds.getOrElse(""))
    req.addParameter("n", song.trackNum.getOrElse(""))
    req.addParameter("m", song.mbid.getOrElse(""))
    val res = tab.go(req)
    val lines = res.getPageSource.lines
    lines.next.trim == "OK"
  }

  def submit(session: GasbagScrobbleSession, song: SongInfo) = {
    val p = new Prowser
    val tab = p.createTab
    val req = new Request(session.submissionUrl)
    req.setHttpMethod("POST")
    req.addParameter("s", session.sessionId)
    req.addParameter("a[0]", song.artist)
    req.addParameter("t[0]", song.trackName)
    req.addParameter("i[0]", song.startPlayingTime.toString)
    req.addParameter("o[0]", "P")
    req.addParameter("r[0]", "")
    req.addParameter("l[0]", song.lengthInSeconds.get)
    req.addParameter("b[0]", song.album.getOrElse(""))
    req.addParameter("n[0]", song.trackNum.getOrElse(""))
    req.addParameter("m[0]", song.mbid.getOrElse(""))
    val res = tab.go(req)
    val lines = res.getPageSource.lines
    lines.next.trim == "OK"
  }

  def timestamp = new Date().getTime / 1000
}
