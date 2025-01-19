package ltd.kalai.les.assignments2

import com.google.gson.stream.JsonReader
import java.io.{BufferedReader, FileReader}
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConverters._
import scala.util.Try

object LargeJsonProcessor {

  def main(args: Array[String]): Unit = {
    val filePath = "/Users/shan/Les/Assignments_2/large_events.json"

    val dataMap = new ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]()

    val startTime = System.currentTimeMillis()

    processLargeJsonFile(filePath, dataMap)

    val endTime = System.currentTimeMillis()

    val timeTaken = endTime - startTime

    println(s"Processing started at: $startTime ms")
    println(s"Processing ended at: $endTime ms")
    println(s"Total time taken: $timeTaken ms")

    println(s"Processed data size: ${dataMap.size()}")
    dataMap.asScala.take(5).foreach { case (objectId, attributes) =>
      println(s"Object ID: $objectId, Attributes: ${attributes.asScala}")
    }

  }

  def processLargeJsonFile(filePath: String, dataMap: ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]): Unit = {
    val reader = new JsonReader(new BufferedReader(new FileReader(filePath)))
    try {
      reader.beginArray()

      while (reader.hasNext) {
        val event = parseEvent(reader)
        event.foreach { case (eventId, timestamp, objectId, attributeName, attributeValue) =>
          processEvent(eventId, timestamp, objectId, attributeName, attributeValue, dataMap)
        }
      }

      reader.endArray()
    } finally {
      reader.close()
    }
  }

  def parseEvent(reader: JsonReader): Option[(String, Long, Int, String, String)] = {
    var eventId: String = null
    var timestamp: Long = 0
    var objectId: Int = 0
    var attributeName: String = null
    var attributeValue: String = null

    reader.beginObject()
    while (reader.hasNext) {
      reader.nextName() match {
        case "event_id"        => eventId = reader.nextString()
        case "timestamp"       => timestamp = reader.nextLong()
        case "object_id"       => objectId = reader.nextInt()
        case "attribute_name"  => attributeName = reader.nextString()
        case "attribute_value" => attributeValue = reader.nextString()
        case _                 => reader.skipValue()
      }
    }
    reader.endObject()

    Try((eventId, timestamp, objectId, attributeName, attributeValue)).toOption
  }

  def processEvent(
                    eventId: String,
                    timestamp: Long,
                    objectId: Int,
                    attributeName: String,
                    attributeValue: String,
                    dataMap: ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]
                  ): Unit = {
    val objectMap = dataMap.computeIfAbsent(objectId, _ => new ConcurrentHashMap[String, String]())

    val existingValue = objectMap.get(attributeName)
    if (existingValue == null || existingValue.toLong < timestamp) {
      // Update the attribute value only if it's newer
      objectMap.put(attributeName, attributeValue)
    }
  }

}