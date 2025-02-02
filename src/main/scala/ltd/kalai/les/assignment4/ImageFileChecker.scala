package ltd.kalai.les.assignment4

import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
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
  private val imageCache: LoadingCache[File, BufferedImage] = CacheBuilder.newBuilder()
    .maximumSize(maxImageSize)
    .expireAfterAccess(10, TimeUnit.MINUTES)
    .build(new CacheLoader[File, BufferedImage]() {
      override def load(file: File): BufferedImage = {
        ImageIO.read(file)
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

  def processDuplicates(images: List[File]): List[List[File]] = {
    findDuplicateImageGroups(images)
  }

  def printDuplicateGroups(duplicateGroups: List[List[File]]): Unit = {
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
      printDuplicateGroups(duplicateGroups)
    } catch {
      case e: Exception => logger.error("An error occurred", e)
    }
  }

  private def computePerceptualHash(file: File): String = {
    val bufferedImage = imageCache.get(file)
    val hash = hashAlgorithm.hash(bufferedImage)
    hash.toString
  }

  private def findDuplicateImageGroups(imageFiles: List[File]): List[List[File]] = {
    val hashToFileMap = mutable.Map[String, List[File]]()

    imageFiles.foreach { file =>
      val hash = computePerceptualHash(file)

      hashToFileMap.updateWith(hash) {
        case Some(files) => Some(file :: files)
        case None        => Some(List(file))
      }
    }

    val duplicateGroups = hashToFileMap.values.flatMap { fileGroup =>
      val fullyValidatedGroup = validateDuplicatesWithFullComparison(fileGroup)
      if (fullyValidatedGroup.size > 1) Some(fullyValidatedGroup) else None
    }

    duplicateGroups.toList
  }

  private def validateDuplicatesWithFullComparison(files: List[File]): List[File] = {
    val duplicates = mutable.ListBuffer[File]()

    for (i <- files.indices) {
      val image1 = imageCache.get(files(i))
      var isDuplicate = false

      for (j <- i + 1 until files.size) {
        val image2 = imageCache.get(files(j))
        if (imagesAreIdentical(image1, image2)) {
          duplicates.append(files(j))
          isDuplicate = true
        }
      }

      if (!duplicates.contains(files(i))) duplicates.append(files(i))
    }

    duplicates.toList
  }

  private def imagesAreIdentical(img1: BufferedImage, img2: BufferedImage): Boolean = {
    if (img1.getWidth != img2.getWidth || img1.getHeight != img2.getHeight) return false

    boundary {
      for (x <- 0 until img1.getWidth; y <- 0 until img1.getHeight) {
        if (img1.getRGB(x, y) != img2.getRGB(x, y)) {
          break(false)
        }
      }

      true
    }
  }

}