package ltd.kalai.les.assignments

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

import java.io.{FileNotFoundException, InputStream}
import scala.io.Source
import scala.jdk.CollectionConverters._

object ReadFileFromUrl {

  private val logger = LoggerFactory.getLogger(ReadFileFromUrl.getClass)

  def main(args: Array[String]): Unit = {
    
    val urls = loadUrlsFromYaml("urls.yaml")

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
  
  private def loadUrlsFromYaml(fileName: String): List[String] = {
    try {
      val yaml = new Yaml()
      val inputStream: InputStream = getClass.getClassLoader.getResourceAsStream(fileName)
      if (inputStream == null) {
        throw new FileNotFoundException(s"File $fileName not found")
      }
      val data = yaml.load(inputStream).asInstanceOf[java.util.Map[String, java.util.List[String]]]
      inputStream.close()
      data.get("urls").asScala.toList
    } catch
      case e: Exception =>
        logger.error(s"Failed to load URLs from $fileName: ${e.getMessage}")
        List.empty
  }

}