package ltd.kalai.les.assignment3

import java.net.URI
import scala.util.Try

object URINormalizer {

  def normalizeURL(url: String): Option[String] = {
    Try(new URI(url)).toOption.map { parsedUri =>

      val protocol = parsedUri.getScheme.toLowerCase

      val host = parsedUri.getHost.toLowerCase

      val port = parsedUri.getPort match {
        case -1 => ""
        case p => s":$p"
      }
      val path = Option(parsedUri.getPath)
        .map(_.replaceAll("/+", "/"))
        .map(_.replaceAll("index\\.html$", "")) // Remove "index.html" at the end
        .getOrElse("")
      val query = Option(parsedUri.getQuery)
        .map(_.split("&").sorted.mkString("&")) // Sort query parameters alphabetically
        .map(q => s"?$q")
        .getOrElse("")
      val ref = "" // Always remove fragments
      s"$protocol://$host$port$path$query$ref".replaceAll("/$", "") // Remove trailing slash from the URL
    }
  }
}