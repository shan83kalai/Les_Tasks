package ltd.kalai.les.assignment3

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object WebScraper {

  private case class LinkInfo(url: String, text: String)
  private def normalizeURL(url: String): String = URLNormalizer.normalizeURL(url).getOrElse(url)

  private def extractLinks(url: String): Either[String, List[LinkInfo]] = {
    Try {
      val doc: Document = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .timeout(10000)
        .followRedirects(true)
        .get()

      val links: Elements = doc.select("a[href]")

      links.asScala
        .map(element => LinkInfo(
          normalizeURL(element.attr("abs:href").trim),
          element.text().trim
        ))
        .filter(link => link.url.nonEmpty && link.url.startsWith("http"))
        .groupBy(_.url)
        .map { case (url, linkInfos) => linkInfos.head }
        .toList
    } match {
      case Success(links) => Right(links)
      case Failure(ex) => Left(s"Error extracting links: ${ex.getMessage}")
    }
  }

  private def saveLinksToFile(links: List[LinkInfo], filename: String): Either[String, Unit] = {
    Try {
      val writer = new PrintWriter(new File(filename), StandardCharsets.UTF_8.name())
      try {
        writer.println("URL,Text")
        links.foreach { link =>
          val escapedUrl = normalizeURL(link.url).replace("\"", "\"\"")
          val escapedText = link.text.replace("\"", "\"\"")
          writer.println(s""""$escapedUrl","$escapedText"""")
        }
      } finally {
        writer.close()
      }
    } match {
      case Success(_) => Right(())
      case Failure(ex) => Left(s"Error saving to file: ${ex.getMessage}")
    }
  }

  def main(args: Array[String]): Unit = {
//    val targetUrl = "https://meet.google.com/landing"
    val targetUrl = "https://www.bbc.co.uk"
    val outputFile = "extracted_links.csv"

    println(s"Starting link extraction from: $targetUrl")
    println("Please wait...")

    extractLinks(targetUrl) match {
      case Right(links) =>
        println(s"\nFound ${links.size} links:")
        links.zipWithIndex.foreach { case (link, index) =>
          println(s"${index + 1}. ${link.url} - ${link.text}")
        }

        saveLinksToFile(links, outputFile) match {
          case Right(_) => println(s"\nLinks have been saved to $outputFile")
          case Left(error) => println(s"\nError saving file: $error")
        }
      case Left(error) => println(s"Error: $error")
    }
  }

}