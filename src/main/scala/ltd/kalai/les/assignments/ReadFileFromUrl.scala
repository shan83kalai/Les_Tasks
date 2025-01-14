package ltd.kalai.les.assignments

import scala.io.Source
import org.slf4j.LoggerFactory

object ReadFileFromUrl {

  private val logger = LoggerFactory.getLogger(ReadFileFromUrl.getClass)

  def main(args: Array[String]): Unit = {

    val urls = List(
      "http://localhost:9090/docs/File_1.txt",
      "http://localhost:9090/docs/File_2.txt",
      "http://localhost:9090/docs/File_3.txt",
      "http://localhost:9090/docs/File_4.txt",
      "http://localhost:9090/docs/File_5.txt",
      "http://localhost:9090/docs/File_6.txt",
      "http://localhost:9090/docs/File_7.txt",
      "http://localhost:9090/docs/File_8.txt",
      "http://localhost:9090/docs/File_9.txt"
    )

    urls.foreach { url =>
      logger.info(s"Reading from URL: $url")
      try {
        val source = Source.fromURL(url)
        try {
          source.getLines().foreach(line => logger.info(s"Lines from $url: $line"))
        } finally {
          source.close()
        }
        logger.info(s"Successfully read from URL: $url")
      } catch {
        case e: Exception =>
          logger.error(s"Failed to read from $url: ${e.getMessage}")
      }
    }
  }

}