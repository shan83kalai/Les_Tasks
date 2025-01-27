package ltd.kalai.les.assignment3

import java.net.{URL, URLEncoder}
import java.util.Comparator
import scala.util.Try

object URLNormalizer {

  def normalizeURL(url: String): Option[String] = {
    Try(new URL(url)).toOption.map { parsedUrl =>
      val protocol = parsedUrl.getProtocol.toLowerCase
      val host = parsedUrl.getHost.toLowerCase
      val port = parsedUrl.getPort match {
        case -1 => "" // Default ports will be omitted
        case p => s":$p"
      }
      val path = Option(parsedUrl.getPath)
        .map(_.replaceAll("/+", "/")) // Remove duplicate slashes
        .map(_.replaceAll("index\\.html$", "")) // Remove "index.html" at the end
        .getOrElse("")
      val query = Option(parsedUrl.getQuery)
        .map(_.split("&").sorted.mkString("&")) // Sort query parameters alphabetically
        .map(q => s"?$q")
        .getOrElse("")
      val ref = "" // Always remove fragments
      s"$protocol://$host$port$path$query$ref".replaceAll("/$", "") // Remove trailing slash from the URL
    }
  }
}