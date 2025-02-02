package ltd.kalai.les.assignment4

import java.awt.image.BufferedImage
import java.io._
import javax.imageio.ImageIO
import org.apache.tika.Tika
import dev.brachtendorf.jimagehash.hashAlgorithms.DifferenceHash
import scala.collection.mutable
import scala.util.boundary, boundary.break
import com.google.common.cache.{CacheBuilder, CacheLoader, LoadingCache}

import java.util.concurrent.TimeUnit

object ImageFileChecker {

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
      val duplicateGroups = findDuplicateImageGroups(imageFiles)

      if (duplicateGroups.isEmpty) {
        println("No duplicate images detected.")
      } else {
        println("Duplicate image groups detected:")
        duplicateGroups.foreach { duplicates =>
          println("Duplicate group:")
          duplicates.foreach(file => println(s" - ${file.getAbsolutePath}"))
        }
      }
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