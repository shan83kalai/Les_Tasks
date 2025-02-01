package ltd.kalai.les.assignment4

import java.io.File

object ImageFileChecker {

  def isImage(file: File): Boolean = {
    val imageExtensions = Set("jpg", "jpeg", "png", "gif", "bmp", "tiff", "webp", "heic", "heif")
    file.isFile && imageExtensions.exists(file.getName.toLowerCase.endsWith)
  }

  def listFiles(dir: File): List[File] = {
    val contents = dir.listFiles
    if (contents != null) {
      contents.toList.flatMap { file =>
        if (file.isDirectory) listFiles(file) else List(file)
      }
    } else Nil
  }

  def main(args: Array[String]): Unit = {

    val dir = new File("/Users/shan/Les/")

    if (!dir.exists || !dir.isDirectory) {
      println(s"Invalid directory: ${args(0)}")
      sys.exit(1)
    }

    val files = listFiles(dir)

    val imageFiles = files.filter(isImage)

    if (imageFiles.isEmpty) {
      println("No image files found.")
    } else {
      println("Image files found:")
      imageFiles.foreach(file => println(file.getAbsolutePath))
    }
  }

}