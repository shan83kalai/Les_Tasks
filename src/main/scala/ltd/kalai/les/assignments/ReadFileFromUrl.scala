package ltd.kalai.les.assignments

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

import java.io.{File, FileNotFoundException, InputStream}
import java.net.URI
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*

object ReadFileFromUrl {

  private val logger = LoggerFactory.getLogger(ReadFileFromUrl.getClass)

  def main(args: Array[String]): Unit = {

    val urls = loadUrlsFromYaml("urls.yaml")

    urls.foreach { url =>
      logger.info(s"Processing URL: $url")
      try {
        val localFile = downloadFile(url)
        logger.info(s"Downloaded file from $url to ${localFile.getAbsolutePath}")
        val source = Source.fromFile(localFile)
        try {
          source.getLines().foreach(line => logger.info(s"Lines from $url: $line"))
        } finally {
          source.close()
        }
        logger.info(s"Successfully read from URL: $url")
      } catch
        case e: Exception =>
          logger.error(s"Failed to read from $url: ${e.getMessage}")
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

  private def downloadFile(fileUrl: String): File = {
    try {
      val url = new URI(fileUrl)
      val utl = url.toURL
      val fileName = Paths.get(url.getPath).getFileName.toString
      val tempDir = Files.createTempDirectory("downloaded_files")
      val localFilePath = tempDir.resolve(fileName)
      Files.copy(utl.openStream(), localFilePath, StandardCopyOption.REPLACE_EXISTING)
      new File(localFilePath.toString)
    } catch
      case e: java.net.URISyntaxException =>
        throw new RuntimeException(s"Invalid URL syntax: $fileUrl", e)
      case e: java.io.IOException =>
        throw new RuntimeException(s"Failed to download file from $fileUrl: ${e.getMessage}", e)
  }

}