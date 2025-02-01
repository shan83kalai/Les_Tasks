package ltd.kalai.les.assignment4

import java.io._
import org.apache.tika.Tika
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import javax.imageio.ImageIO
import scala.collection.mutable

object ImageFileChecker {

  private val tika = new Tika()
  private val hashAlgorithm = new DifferenceHash(64, DifferenceHash.Precision.Triple)

  def isImage(file: File): Boolean = {
    file.isFile && {
      val mimeType = tika.detect(file)
      mimeType.startsWith("image/")
    }
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
      val duplicates = findDuplicateImages(imageFiles)

      if (duplicates.isEmpty) {
        println("No duplicate images detected.")
      } else {
        println("Duplicate images detected:")
        duplicates.foreach {
          case (hash, fileList) =>
            println(s"Images with hash [$hash]:")
            fileList.foreach(file => println(s" - ${file.getAbsolutePath}"))
        }
      }
    }
  }

  private def computePerceptualHash(file: File): String = {
    val bufferedImage = ImageIO.read(file)
    val hash = hashAlgorithm.hash(bufferedImage)
    hash.toString
  }

  private def findDuplicateImages(imageFiles: List[File]): Map[String, List[File]] = {
    val hashToFileMap = mutable.Map[String, List[File]]()

    imageFiles.foreach { file =>
      val hash = computePerceptualHash(file)

      hashToFileMap.updateWith(hash) {
        case Some(files) => Some(file :: files)
        case None        => Some(List(file))
      }
    }

    hashToFileMap.filter { case (_, fileList) => fileList.size > 1 }.toMap
  }
}