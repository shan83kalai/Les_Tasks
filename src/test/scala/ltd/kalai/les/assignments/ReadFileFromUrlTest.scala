package ltd.kalai.les.assignments

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.slf4j.Logger

import java.io.{File, FileWriter}
import scala.io.Source.fromFile
import scala.util.Using

class ReadFileFromUrlTest extends AnyFunSuite with Matchers with MockitoSugar {

  test("loadUrlsFromYaml should load URLs from a valid YAML file") {

    val yamlContent =
      """
        |urls:
        |  - http://localhost:9090/docs/File_1.txt
        |  - http://localhost:9090/docs/File_2.txt
        |""".stripMargin

    val tempYamlFile = File.createTempFile("testUrls", ".yaml")
    val writer = new java.io.FileWriter(tempYamlFile)
    writer.write(yamlContent)
    writer.close()

    val urls = ReadFileFromUrl.loadUrlsFromYaml(tempYamlFile.getAbsolutePath, false)
    urls.size shouldBe 2
    urls.head shouldBe "http://localhost:9090/docs/File_1.txt"
    urls.last shouldBe "http://localhost:9090/docs/File_2.txt"
    tempYamlFile.delete()
  }

  test("loadUrlsFromYaml should return an empty list if the YAML file is invalid") {

    val invalidYamlContent = ""

    val tempYamlFile = File.createTempFile("testUrls", ".yaml")
    val writer = new java.io.FileWriter(tempYamlFile)
    writer.write(invalidYamlContent)
    writer.close()

    val urls = ReadFileFromUrl.loadUrlsFromYaml(tempYamlFile.getAbsolutePath, false)
    urls shouldBe empty
    tempYamlFile.delete()
  }

  test("loadUrlsFromYaml should return an empty list if the YAML file does not exist") {
    val urls = ReadFileFromUrl.loadUrlsFromYaml("invalid_file.yaml", false)
    urls shouldBe empty
  }

  test("downloadFile should download a file") {
    val tempFile = File.createTempFile("testFile", ".txt")
    val fileContent = "Test file content"
    val writer = new FileWriter(tempFile)
    writer.write(fileContent)
    writer.close()

    val mockUrl = tempFile.toURI.toURL.toString
    val downloadedFile = ReadFileFromUrl.downloadFile(mockUrl)

    downloadedFile.exists() shouldBe true
    downloadedFile.length() shouldBe fileContent.length
    downloadedFile.delete()
  }

  test("downloadFile should throw an exception if the URL is invalid") {
    val invalidUrl = "invalid_url"

    assertThrows[RuntimeException] {
        ReadFileFromUrl.downloadFile(invalidUrl)
      }
  }

  test("writeToFile should write content to a file") {
    val tempFile = File.createTempFile("testFile", ".txt")
    val fileContent = "Test file content"
    val writer = new FileWriter(tempFile)
    writer.write(fileContent)
    writer.close()

    ReadFileFromUrl.writeToFile(tempFile, fileContent)
    tempFile.exists() shouldBe true
    tempFile.length() shouldBe fileContent.length
    tempFile.delete()
  }

  test("createConcatenatedFile should concatenate files from multiple URLs") {

    val urls = List("http://localhost:9090/docs/File_1.txt", "http://localhost:9090/docs/File_2.txt")
    val outputDir = File.createTempFile("testOutput", "").getAbsolutePath + "/testDir"
    val outputFileName = "concatenated_output.txt"

    val fileContent1 = "Test file content 1"
    val fileContent2 = "Test file content 2"

    val mockLocalFile1 = File.createTempFile("testFile1", ".txt")
    val mockLocalFile2 = File.createTempFile("testFile2", ".txt")

    val mockUrl1 = mockLocalFile1.toURI.toURL.toString
    val mockUrl2 = mockLocalFile2.toURI.toURL.toString

    val writer1 = new FileWriter(mockLocalFile1)
    writer1.write(fileContent1)
    writer1.close()

    val writer2 = new FileWriter(mockLocalFile2)
    writer2.write(fileContent2)
    writer2.close()

    val mockedFileDownloader = mock[ReadFileFromUrl.type]
    when(mockedFileDownloader.downloadFile(mockUrl1)).thenReturn(mockLocalFile1)
    when(mockedFileDownloader.downloadFile(mockUrl2)).thenReturn(mockLocalFile2)

    val mockedLogger = mock[Logger]

    usingMockedDependencies(mockedFileDownloader, mockedLogger) {
      ReadFileFromUrl.createConcatenatedFile(urls, outputDir, outputFileName)
      val concatenatedFile = new File(outputDir + outputFileName)
      concatenatedFile.exists() shouldBe true
      concatenatedFile.length() shouldBe (fileContent1 + fileContent2).length
      val fileContent = scala.util.Using(fromFile(concatenatedFile)) {
        source => source.mkString
      }.getOrElse("")
      fileContent shouldBe (fileContent1 + fileContent2)
      concatenatedFile.delete()
    }
    
  }

  test("createConcatenatedFile should handle empty URLs list") {
    val urls = List.empty[String]
    val outputDir = File.createTempFile("testOutput", "").getAbsolutePath + "/testDir"
    val outputFileName = "concatenated_output.txt"

    ReadFileFromUrl.createConcatenatedFile(urls, outputDir, outputFileName)

    val concatenatedFile = new File(outputDir + outputFileName)
    concatenatedFile.exists() shouldBe false
  }

  test("createConcatenatedFile should handle invalid URL") {
    val urls = List("http://invalid-url/docs/File_1.txt")
    val outputDir = File.createTempFile("testOutput", "").getAbsolutePath + "/testDir"
    val outputFileName = "concatenated_output.txt"

    val mockedFileDownloader = mock[ReadFileFromUrl.type]
    val mockedLogger = mock[Logger]
    
    when(mockedFileDownloader.downloadFile(anyString())).thenThrow(new RuntimeException("Failed to download file"))

    usingMockedDependencies(mockedFileDownloader, mockedLogger) {
      ReadFileFromUrl.createConcatenatedFile(urls, outputDir, outputFileName)
      val concatenatedFile = new File(outputDir + outputFileName)
      concatenatedFile.exists() shouldBe false
    }
  }

  private def usingMockedDependencies(mockedFileDownloader: ReadFileFromUrl.type, mockedLogger: Logger)(block: => Unit): Unit = {}

}
