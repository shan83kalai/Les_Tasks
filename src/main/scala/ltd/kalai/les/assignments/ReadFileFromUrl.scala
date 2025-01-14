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

    urls match {
      case Left(error) =>
        logger.error(s"Error loading URLs: $error")
      case Right(urlList) =>
        createConcatenatedFile(urlList, outputDir, outputFileName) match {
          case Left(error) =>
            logger.error(s"Error creating concatenated file: $error")
          case Right(_) =>
            logger.info("Concatenated file created successfully.")
        }
    }
  }

  def createConcatenatedFile(urls: List[String], outputDir: String, outputFileName: String): Either[String, Unit] = {
    val outputDirectory = new File(outputDir)
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs()
    }
    val outputFile = new File(outputDir + outputFileName)

    val concatenatedContent = urls.map { url =>
      logger.info(s"Processing URL: $url")
      downloadFile(url).flatMap { localFile =>
        readFile(localFile).map { content =>
          logger.info(s"Successfully read content from: ${localFile.getName}")
          content
        }
      }.left.map { error =>
        logger.error(s"failed to process from $url: $error")
        ""
      }.merge
    }.mkString("\n")

    writeToFile(outputFile, concatenatedContent)
  }

  def loadUrlsFromYaml(fileName: String, isClasspathResource: Boolean): Either[String, List[String]] = {

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
      Right(data.get("urls").asScala.toList)
    } catch
      case e: Exception =>
        Left(s"Failed to load URLs from $fileName: ${e.getMessage}")
  }

  def downloadFile(fileUrl: String): Either[String, File] = {
    try {
      val url = new URI(fileUrl)
      val utl = url.toURL
      val fileName = Paths.get(url.getPath).getFileName.toString
      val tempDir = Files.createTempDirectory("downloaded_files")
      val localFilePath = tempDir.resolve(fileName)
      Files.copy(utl.openStream(), localFilePath, StandardCopyOption.REPLACE_EXISTING)
      Right(new File(localFilePath.toString))
    } catch
      case e: java.net.URISyntaxException =>
        Left(s"Invalid URL syntax: $fileUrl: ${e.getMessage}")
      case e: java.io.IOException =>
        Left(s"Failed to download file from $fileUrl: ${e.getMessage}")
  }

  def writeToFile(outputFile: File, content: String): Either[String, Unit] = {
    try {
      val writer = new BufferedWriter(new FileWriter(outputFile))
      try {
        writer.write(content)
        Right(())
      } finally {
        writer.close() // Ensure the writer is closed properly
      }
    } catch {
      case e: Exception =>
        Left(s"Failed to write to file: ${e.getMessage}")
    }
  }

  def readFile(file: File): Either[String, String] = {
    try {
      val source = Source.fromFile(file)
      try {
        Right(source.mkString)
      } finally {
        source.close()
      }
    } catch {
      case e: Exception =>
        Left(s"Failed to read from file ${file.getAbsolutePath}: ${e.getMessage}")
    }
  }

}
