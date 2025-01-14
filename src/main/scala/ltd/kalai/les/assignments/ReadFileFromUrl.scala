package ltd.kalai.les.assignments

import org.slf4j.LoggerFactory
import org.yaml.snakeyaml.Yaml

import java.io.*
import java.net.URI
import java.nio.file.{Files, Paths, StandardCopyOption}
import scala.io.Source
import scala.jdk.CollectionConverters.*

object ReadFileFromUrl {

  private val logger = LoggerFactory.getLogger(ReadFileFromUrl.getClass)

  def main(args: Array[String]): Unit = {

    val outputDir = "/Users/shan/http/docs/"
    val outputFileName = "concatenated_output.txt"
    val urls = loadUrlsFromYaml("urls.yaml", true)

    createConcatenatedFile(urls, outputDir, outputFileName)
  }

  def createConcatenatedFile(urls: List[String], outputDir: String, outputFileName: String): Unit = {
    val logger = LoggerFactory.getLogger(this.getClass)

    val outputDirectory = new File(outputDir)
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs()
    }

    val outputFile = new File(outputDir + outputFileName)

    try {
      val concatenatedContent = urls.map { url =>
        logger.info(s"Processing URL: $url")
        try {
          val localFile = downloadFile(url)
          logger.info(s"Downloaded file from $url to ${localFile.getAbsolutePath}")
          val source = Source.fromFile(localFile)
          try {
            val content = source.mkString
            logger.info(s"Successfully read content from: ${localFile.getName}")
            content
          } finally {
            source.close()
          }
        } catch {
          case e: Exception =>
            logger.error(s"Failed to read from $url: ${e.getMessage}")
            ""
        }
      }.mkString("\n")

      writeToFile(outputFile, concatenatedContent)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to concatenate files: ${e.getMessage}")
    }
  }

  def loadUrlsFromYaml(fileName: String, isClasspathResource: Boolean): List[String] = {

    val yaml = new Yaml()
    var inputStream: InputStream = null

    try {

      if (!isClasspathResource && Paths.get(fileName).toFile.exists()) {
        logger.info(s"Loading YAML from file path: $fileName")
        inputStream = new FileInputStream(fileName)
      } else {
          logger.info(s"Loading YAML from classpath: $fileName")
          inputStream = Option(getClass.getClassLoader.getResourceAsStream(fileName))
            .getOrElse(throw new FileNotFoundException(s"File $fileName not found on classpath"))
      }

      val data = yaml.load(inputStream).asInstanceOf[java.util.Map[String, java.util.List[String]]]
      inputStream.close()
      data.get("urls").asScala.toList
    } catch
      case e: Exception =>
        logger.error(s"Failed to load URLs from $fileName: ${e.getMessage}")
        List.empty
  }

  def downloadFile(fileUrl: String): File = {
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

  def writeToFile(outputFile: File, content: String): Unit = {
    val writer = new BufferedWriter(new FileWriter(outputFile))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }



}