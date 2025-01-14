package ltd.kalai.les.assignments.util

import java.io.{BufferedWriter, File, FileWriter}

object CreateFilesApp {

  def createFiles(directoryPath: String, count: Int): Either[String, Unit] = {
    val directory = new File(directoryPath)

    if (!directory.exists()) {
      if (!directory.mkdirs()) {
        return Left(s"Failed to create directory at: $directoryPath")
      }
    }

    try {
      for (i <- 1 to count) {
        val file = new File(directory, s"File_$i.txt")
        writeToFile(file, s"This is the content of file $i.")
      }
      Right(())
    } catch {
      case ex: Exception => Left(s"Failed to create files: ${ex.getMessage}")
    }
  }

  def writeToFile(file: File, content: String): Unit = {
    val writer = new BufferedWriter(new FileWriter(file))
    try {
      writer.write(content)
    } finally {
      writer.close()
    }
  }

  def main(args: Array[String]): Unit = {
    val directoryPath = "/Users/shan/http/docs/"
    val numberOfFiles = 100

    createFiles(directoryPath, numberOfFiles) match {
      case Right(_) =>
        println(s"Successfully created $numberOfFiles files in directory: $directoryPath")
      case Left(error) =>
        println(s"Error: $error")
    }
  }
}
