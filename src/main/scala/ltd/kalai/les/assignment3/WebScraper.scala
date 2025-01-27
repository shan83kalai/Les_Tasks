package ltd.kalai.les.assignment3

import com.google.common.cache.{Cache, CacheBuilder}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import org.slf4j.LoggerFactory

import java.io.{File, PrintWriter}
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.{Failure, Success, Try}

object WebScraper {

  private val visitedUrlsCache: Cache[String, Boolean] =
    CacheBuilder.newBuilder()
      .expireAfterAccess(60, java.util.concurrent.TimeUnit.MINUTES)
      .maximumSize(100000)
      .build()

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val totalUrls = new AtomicInteger(0)
  private val allLinks = scala.collection.mutable.Set[LinkInfo]()

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(
    Executors.newFixedThreadPool(Runtime.getRuntime.availableProcessors())
  )

  private case class LinkInfo(url: String, text: String)

  private def normalizeURL(url: String): String = URINormalizer.normalizeURL(url).getOrElse(url)

  private def extractLinks(url: String): Either[String, List[LinkInfo]] = {
    if (Option(visitedUrlsCache.getIfPresent(url)).isDefined) {
      logger.info(s"URL already visited: $url")
      return Right(List.empty)
    }

    Try {
      val doc: Document = Jsoup.connect(url)
        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .timeout(10000)
        .followRedirects(true)
        .get()

      val links: Elements = doc.select("a[href]")

      val extractedLinks = links.asScala
        .map(element => LinkInfo(
          normalizeURL(element.attr("abs:href").trim),
          element.text().trim
        ))
        .filter(link => link.url.nonEmpty && link.url.startsWith("http"))
        .groupBy(_.url)
        .map { case (_, linkInfos) => linkInfos.head }
        .toList

      visitedUrlsCache.put(url, true)
      extractedLinks
    } match {
      case Success(links) =>
        logger.info(s"Successfully extracted ${links.size} links from URL: $url") // Log successful extraction
        Right(links)
      case Failure(ex) =>
        logger.error(s"Error extracting links from URL: $url - ${ex.getMessage}") // Log errors
        Left(s"Error extracting links: ${ex.getMessage}")
    }
  }

  def main(args: Array[String]): Unit = {
    val targetUrl = "https://www.gov.uk"
    val outputFile = "extracted_links.csv"

    println(s"Starting link extraction from: $targetUrl")
    println("Please wait...")

    extractLinks(targetUrl) match {
      case Right(initialLinks) =>
        println(s"\nFound ${initialLinks.size} links from the root URL.")
        addLinks(initialLinks)

        val future = recursivelyExtractLinksParallel(initialLinks)
        Await.ready(future, Duration.Inf)

        saveLinksToFile(allLinks.toList, outputFile) match {
          case Right(_) =>
            println(s"\nTotal ${totalUrls.get()} links saved to $outputFile")
          case Left(error) =>
            println(s"\nError saving file: $error")
        }
      case Left(error) =>
        println(s"Error: $error")
    }
  }

  private def addLinks(links: List[LinkInfo]): Unit = synchronized {
    links.foreach { link =>
      if (Option(visitedUrlsCache.getIfPresent(link.url)).isEmpty && totalUrls.get() < 1000000) {
        allLinks.add(link)
        totalUrls.incrementAndGet()
      }
    }
  }

  private def recursivelyExtractLinksParallel(links: List[LinkInfo])(implicit ec: ExecutionContext): Future[Unit] = {
    logger.debug(s"Recursively processing ${links.size} links...")

    if (totalUrls.get() >= 10000) {
      logger.info(s"Stopping recursion as total limit (${totalUrls.get()}) is reached.")
      return Future.successful(())
    }

    val futures = links.map { link =>
      Future {
        logger.info(s"Fetching links for URL: ${link.url}")
        extractLinks(link.url) match {
          case Right(nestedLinks) =>
            logger.info(s"Extracted ${nestedLinks.size} links from ${link.url}")
            nestedLinks
          case Left(error) =>
            logger.error(s"Error extracting links from ${link.url}: $error")
            List.empty
        }
      }
    }

    Future.sequence(futures).flatMap { results =>
      val combinedResults = results.flatten.filter(link => Option(visitedUrlsCache.getIfPresent(link.url)).isEmpty)
      logger.info(s"Found ${combinedResults.size} new links for processing.")
      addLinks(combinedResults)

      if (combinedResults.nonEmpty && totalUrls.get() < 1000000) {
        recursivelyExtractLinksParallel(combinedResults)
      } else {
        logger.info("No more links to process or limit reached.")
        Future.successful(())
      }
    }
  }

  private def recursivelyExtractLinks(links: List[LinkInfo], outputFile: String): Unit = {
    links.foreach { link =>
      extractLinks(link.url) match {
        case Right(nestedLinks) =>
          println(s"Extracted ${nestedLinks.size} links from ${link.url}")
          saveLinksToFile(nestedLinks, outputFile) match {
            case Right(_) => println(s"Links from ${link.url} saved to $outputFile")
            case Left(error) => println(s"Error saving links from ${link.url}: $error")
          }
          recursivelyExtractLinks(nestedLinks, outputFile)
        case Left(error) =>
          println(s"Error extracting links from ${link.url}: $error")
      }
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

}