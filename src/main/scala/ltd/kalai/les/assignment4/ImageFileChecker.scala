package ltd.kalai.les.assignment4

import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import org.apache.commons.io.FileUtils
import org.apache.tika.Tika
import org.slf4j.LoggerFactory

import java.awt.image.BufferedImage
import java.io.*
import java.nio.file.{Files, Path}
import javax.imageio.ImageIO
import scala.collection.concurrent.TrieMap
import scala.jdk.StreamConverters.*
import scala.util.boundary
import scala.util.boundary.break

object ImageFileChecker {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val tika = new Tika()
  private val hashAlgorithm = new DifferenceHash(64, DifferenceHash.Precision.Triple)

  private val maxImageSize = 100

  private val imageCache = new TrieMap[File, Either[Exception, BufferedImage]]

  def loadImage(file: File): Either[Exception, BufferedImage] = {
    imageCache.getOrElseUpdate(file, {
      try {
        val bufferedImage = ImageIO.read(file)
        if (bufferedImage == null)
          Left(new Exception(s"Failed to read image: ${file.getAbsolutePath}"))
        else
          Right(bufferedImage)
      } catch {
        case e: Exception => Left(e)
      }
    })
  }

  def isImage(file: File): Boolean = {
    file.isFile && {
      val mimeType = tika.detect(file)
      mimeType.startsWith("image/")
    }
  }

  def listFiles(root: File): List[File] = {
    Files.walk(root.toPath)
      .filter(Files.isRegularFile(_))
      .map(_.toFile)
      .toScala(List)
  }

  def getImageFiles(dir: File): LazyList[File] = {
    Files.walk(dir.toPath)
      .filter(Files.isRegularFile(_))
      .map(_.toFile)
      .filter(file => file.isFile && isImage(file))
      .toScala(LazyList)
  }

  def processDuplicates(images: LazyList[File]): List[Set[File]] = {
    findDuplicateImageGroups(images)
  }

  def printDuplicateGroups(duplicateGroups: List[Set[File]]): Unit = {
    if (duplicateGroups.isEmpty) {
      println("No duplicate images detected.")
    } else {
      val message = duplicateGroups.map(_.map(_.getAbsolutePath).mkString("\n - ", "\n - ", "\n"))
        .mkString("\nDuplicate group:\n", "\nDuplicate group:\n", "")
      println(s"Duplicate image groups detected:\n$message")
    }
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting ImageFileChecker...")
    try {
      val dir = new File(args(0))
      if (!FileUtils.isDirectory(dir)) {
        throw new IllegalArgumentException(s"Invalid directory: ${dir.getAbsolutePath}")
      }

      val imageFiles = getImageFiles(dir)
      val duplicateGroups = processDuplicates(imageFiles)
      printDuplicateGroups(duplicateGroups)
    } catch {
      case e: Exception => logger.error("An error occurred", e)
    }
  }

  private def computePerceptualHash(file: File): String = {
    loadImage(file) match {
      case Right(bufferedImage) =>
        val hash = hashAlgorithm.hash(bufferedImage)
        hash.toString
      case Left(exception) =>
        logger.error(s"Error loading image ${file.getAbsolutePath}: ${exception.getMessage}")
        "error"
    }
  }

  def findDuplicateImageGroups(imageFiles: LazyList[File]): List[Set[File]] = {
    val hashToFileMap = imageFiles.map(file => computePerceptualHash(file) -> file)
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2).toSet)
      .filter(_._2.size > 1)

    hashToFileMap.values.toList
  }

  private def validateDuplicatesWithFullComparison(files: Set[File]): Set[File] = {
    files.filter { file1 =>
      files.exists { file2 =>
        file1 != file2 && {
          (imageCache.get(file1), imageCache.get(file2)) match {
            case (Some(Right(img1: BufferedImage)), Some(Right(img2: BufferedImage))) =>
              imagesAreIdentical(img1, img2) && FileUtils.contentEquals(file1, file2)
            case (Some(Left(error1: Exception)), _) =>
              logger.warn(s"Unable to process file ${file1.getAbsolutePath}: ${error1.getMessage}")
              false
            case (_, Some(Left(error2: Exception))) =>
              logger.warn(s"Unable to process file ${file2.getAbsolutePath}: ${error2.getMessage}")
              false
            case _ =>
              false
          }
        }
      }
    }
  }

  private def imagesAreIdentical(img1: BufferedImage, img2: BufferedImage): Boolean = {
    if (img1.getWidth != img2.getWidth || img1.getHeight != img2.getHeight) return false

    val raster1 = img1.getData
    val raster2 = img2.getData

    boundary {
      for (y <- 0 until img1.getHeight) {
        val row1: Array[Int] = raster1.getPixels(0, y, img1.getWidth, 1, null.asInstanceOf[Array[Int]])
        val row2: Array[Int] = raster2.getPixels(0, y, img2.getWidth, 1, null.asInstanceOf[Array[Int]])
        if (!row1.sameElements(row2)) {
          break(false)
        }
      }

      true
    }
  }

}