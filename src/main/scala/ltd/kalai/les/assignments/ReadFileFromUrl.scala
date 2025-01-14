package ltd.kalai.les.assignments

import scala.io.Source

object ReadFileFromUrl {
  def main(args: Array[String]): Unit = {

    val urls = List(
      "http://localhost:9090/docs/File_1.txt",
      "http://localhost:9090/docs/File_2.txt",
      "http://localhost:9090/docs/File_3.txt",
      "http://localhost:9090/docs/File_4.txt",
      "http://localhost:9090/docs/File_5.txt",
      "http://localhost:9090/docs/File_6.txt",
      "http://localhost:9090/docs/File_7.txt",
      "http://localhost:9090/docs/File_8.txt",
      "http://localhost:9090/docs/File_9.txt"
    )

    urls.foreach { url =>
      println(s"Reading from URL: $url")
      try {
        val source = Source.fromURL(url)
        try {
          source.getLines().foreach(println)
        } finally {
          source.close()
        }
      } catch {
        case e: Exception =>
          println(s"Failed to read from $url: ${e.getMessage}")
      }
    }
  }
}