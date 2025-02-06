package ltd.kalai.les.assignment4

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import org.apache.commons.io.FileUtils
import org.apache.tika.Tika
import org.slf4j.LoggerFactory

import java.awt.image.BufferedImage
import java.io.*
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import scala.collection.mutable
import scala.collection.parallel.CollectionConverters.*
import scala.util.boundary
import scala.util.boundary.break

object ImageFileChecker {

  private val logger = LoggerFactory.getLogger(this.getClass)
  private val tika = new Tika()
  private val hashAlgorithm = new DifferenceHash(64, DifferenceHash.Precision.Triple)

  private val maxImageSize = 100
  private val imageCache: LoadingCache[File, Either[Exception, BufferedImage]] = CacheBuilder.newBuilder()
    .maximumSize(maxImageSize)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build(new CacheLoader[File, Either[Exception, BufferedImage]]() {
      override def load(file: File): Either[Exception, BufferedImage] = {
        try {
          val bufferedImage = ImageIO.read(file)
          if (bufferedImage == null) {
            Left(new Exception(s"Failed to read image: ${file.getAbsolutePath}"))
          } else {
            Right(bufferedImage)
          }
        } catch {
          case e: Exception => Left(e)
        }
      }
    })

  def isImage(file: File): Boolean = {
    file.isFile && {
      val mimeType = tika.detect(file)
      mimeType.startsWith("image/")
    }
  }
  
  def listFiles(root: File): List[File] = {
    val fileList = mutable.ListBuffer[File]()
    val stack = mutable.Stack[File](root)

    while (stack.nonEmpty) {
      val current = stack.pop()
      if (current.isDirectory) {
        stack.pushAll(current.listFiles())
      } else {
        fileList += current
      }
    }

    fileList.toList
  }

  def validateDirectory(dir: File): Unit = {
    if (!dir.exists || !dir.isDirectory) {
      throw new IllegalArgumentException(s"Invalid directory: ${dir.getAbsolutePath}")
    }
  }

  def processDuplicates(images: List[File]): List[Set[File]] = {
    findDuplicateImageGroups(images)
  }

  def printDuplicateGroups(duplicateGroups: List[Set[File]]): Unit = {
    if (duplicateGroups.isEmpty) {
      println("No duplicate images detected.")
    } else {
      println("Duplicate image groups detected:")
      duplicateGroups.foreach { group =>
        println("Duplicate group:")
        group.foreach(file => println(s" - ${file.getAbsolutePath}"))
      }
    }
  }

  def main(args: Array[String]): Unit = {
    logger.info("Starting ImageFileChecker...")
    try {
      val dir = new File(args(0))
      validateDirectory(dir)
      val imageFiles = listFiles(dir).par.filter(isImage).toList
      val duplicateGroups = findDuplicateImageGroups(imageFiles)
      if (duplicateGroups.nonEmpty) {
        printDuplicateGroups(duplicateGroups)
      } else {
        println("No duplicates found.")
      }
    } catch {
      case e: Exception => logger.error("An error occurred", e)
    }
  }

  private def computePerceptualHash(file: File): String = {
    imageCache.get(file) match {
      case Right(bufferedImage) =>
        val hash = hashAlgorithm.hash(bufferedImage)
        hash.toString
      case Left(exception) =>
        throw new RuntimeException(s"Failed to load image ${file.getAbsolutePath}: ${exception.getMessage}", exception)
    }
  }

  private def findDuplicateImageGroups(imageFiles: List[File]): List[Set[File]] = {
    val hashToFileMap = mutable.Map[String, Set[File]]()

    imageFiles.distinct.foreach { file =>
      val hash = computePerceptualHash(file)

      hashToFileMap.updateWith(hash) {
        case Some(files) => Some(files + file)
        case None => Some(Set(file))
      }
    }

    val duplicateGroups = hashToFileMap.values.flatMap { fileGroup =>
      val fullyValidatedGroup = validateDuplicatesWithFullComparison(fileGroup)
      if (fullyValidatedGroup.size > 1) Some(fullyValidatedGroup) else None
    }

    duplicateGroups.toList
  }

  private def validateDuplicatesWithFullComparison(files: Set[File]): Set[File] = {
    files.filter { file1 =>
      files.exists { file2 =>
        file1 != file2 && {
          (imageCache.get(file1), imageCache.get(file2)) match {
            case (Right(img1), Right(img2)) =>
              imagesAreIdentical(img1, img2) && FileUtils.contentEquals(file1, file2)
            case (Left(error1), _) =>
              logger.warn(s"Unable to process file ${file1.getAbsolutePath}: ${error1.getMessage}")
              false
            case (_, Left(error2)) =>
              logger.warn(s"Unable to process file ${file2.getAbsolutePath}: ${error2.getMessage}")
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