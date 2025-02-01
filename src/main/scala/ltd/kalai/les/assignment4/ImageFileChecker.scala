package ltd.kalai.les.assignment4

import java.io._
import org.apache.tika.Tika
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import javax.imageio.ImageIO

object ImageFileChecker {

  private val tika = new Tika()
  private val hashAlgorithm = new DifferenceHash(64, DifferenceHash.Precision.Simple)

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
      println("Image files and their perceptual hashes:")

      imageFiles.foreach { file =>
        val hash = computePerceptualHash(file)
        println(s"${file.getAbsolutePath} -> $hash")
      }
    }
  }

  private def computePerceptualHash(file: File): String = {
    val bufferedImage = ImageIO.read(file)
    val hash = hashAlgorithm.hash(bufferedImage)
    hash.toString
  }

}