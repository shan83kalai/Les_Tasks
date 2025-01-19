package ltd.kalai.les.assignments2.util

import java.io.{BufferedWriter, FileWriter}
import java.time.Instant
import scala.util.Random

object LargeJsonFileGenerator {

  def main(args: Array[String]): Unit = {
    val filePath = "/Users/shan/Les/Assignments_2/large_events.json"

    val targetSizeGB = 100
    val approximateEventSize = 150 // Approximate size of one JSON event in bytes
    val eventsCount = (targetSizeGB * 1024L * 1024L * 1024L) / approximateEventSize

    val startEpoch = Instant.parse("2000-01-01T00:00:00Z").getEpochSecond
    val endEpoch = Instant.parse("2025-01-01T00:00:00Z").getEpochSecond

    val random = new Random()

    val writer = new BufferedWriter(new FileWriter(filePath))

    try {
      writer.write("[")
      for (i <- 1L to eventsCount) {
        val eventId = i.toString
        val timestamp = random.between(startEpoch, endEpoch)
        val objectId = random.nextInt(1000000) + 1
        val attributeName = "attr_" + random.nextInt(100)
        val attributeValue = random.nextInt(10000).toString

        // Create JSON event
        val eventJson =
          s"""{
             |  "event_id": "$eventId",
             |  "timestamp": $timestamp,
             |  "object_id": $objectId,
             |  "attribute_name": "$attributeName",
             |  "attribute_value": "$attributeValue"
             |}""".stripMargin

        writer.write(eventJson)
        if (i < eventsCount) writer.write(",")

        if (i % 100000 == 0) {
          writer.flush()
          println(s"$i events written...")
        }
      }
      writer.write("]")
    } finally {
      writer.close()
      println(s"JSON file of size ~${targetSizeGB}GB generated at $filePath")
    }
  }
}

//715,800,000 events written