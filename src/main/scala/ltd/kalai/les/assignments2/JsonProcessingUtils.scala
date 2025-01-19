package ltd.kalai.les.assignments2

import com.google.gson.stream.JsonReader

import java.io.{BufferedReader, FileReader}
import java.util.concurrent.ConcurrentHashMap
import scala.util.Try

object JsonProcessingUtils {

  private def parseEvent(reader: JsonReader): Option[(String, Long, Int, String, String)] = {
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
      objectMap.put(attributeName, attributeValue)
    }
  }

  def processJsonFile(
                       filePath: String,
                       dataMap: ConcurrentHashMap[Int, ConcurrentHashMap[String, String]],
                       processEventFunc: (
                         String,
                           Long,
                           Int,
                           String,
                           String,
                           ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]
                         ) => Unit
                     ): Unit = {
    val reader = new JsonReader(new BufferedReader(new FileReader(filePath)))
    try {
      reader.beginArray()

      while (reader.hasNext) {
        val event = parseEvent(reader) // Parse event
        event.foreach { case (eventId, timestamp, objectId, attributeName, attributeValue) =>
          processEventFunc(eventId, timestamp, objectId, attributeName, attributeValue, dataMap) // Call provided function
        }
      }

      reader.endArray()
    } finally {
      reader.close()
    }
  }

}