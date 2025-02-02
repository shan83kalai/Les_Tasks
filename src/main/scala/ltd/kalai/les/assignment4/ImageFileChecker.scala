package ltd.kalai.les.assignment4

import java.awt.image.BufferedImage
import java.io._
import javax.imageio.ImageIO
import org.apache.tika.Tika
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
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
      val duplicates = findDuplicateImagesWithFullComparison(imageFiles)

      if (duplicates.isEmpty) {
        println("No duplicate images detected.")
      } else {
        println("Duplicate images detected:")
        duplicates.foreach { case (image1, image2) =>
          println(s"Duplicate pair: ${image1.getAbsolutePath} and ${image2.getAbsolutePath}")
        }
      }
    }
  }

  private def computePerceptualHash(file: File): String = {
    val bufferedImage = ImageIO.read(file)
    val hash = hashAlgorithm.hash(bufferedImage)
    hash.toString
  }

  private def findDuplicateImagesWithFullComparison(imageFiles: List[File]): List[(File, File)] = {
    val hashToFileMap = mutable.Map[String, List[File]]()

    imageFiles.foreach { file =>
      val hash = computePerceptualHash(file)

      hashToFileMap.updateWith(hash) {
        case Some(files) => Some(file :: files)
        case None        => Some(List(file))
      }
    }

    val potentialDuplicates = hashToFileMap.filter { case (_, fileList) => fileList.size > 1 }
    val confirmedDuplicates = for {
      (_, fileList) <- potentialDuplicates
      duplicates <- performFullImageComparison(fileList)
    } yield duplicates

    confirmedDuplicates.toList
  }

  private def performFullImageComparison(files: List[File]): List[(File, File)] = {
    val duplicates = mutable.ListBuffer[(File, File)]()

    for (i <- files.indices; j <- i + 1 until files.size) {
      val image1 = ImageIO.read(files(i))
      val image2 = ImageIO.read(files(j))

      if (imagesAreIdentical(image1, image2)) {
        duplicates.append((files(i), files(j)))
      }
    }

    duplicates.toList
  }

  private def imagesAreIdentical(img1: BufferedImage, img2: BufferedImage): Boolean = {
    if (img1.getWidth != img2.getWidth || img1.getHeight != img2.getHeight) return false

    for (x <- 0 until img1.getWidth; y <- 0 until img1.getHeight) {
      if (img1.getRGB(x, y) != img2.getRGB(x, y)) return false
    }

    true
  }
}