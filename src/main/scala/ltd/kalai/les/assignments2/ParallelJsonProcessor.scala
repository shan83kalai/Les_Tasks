package ltd.kalai.les.assignments2

import com.google.gson.stream.JsonReader

import java.io.{BufferedReader, FileReader}
import java.util.concurrent.{ConcurrentHashMap, Executors}
import scala.util.Try

object ParallelJsonProcessor {

  def main(args: Array[String]): Unit = {
    val filePath = "/Users/shan/Les/Assignments_2/large_events.json"
    val numThreads = Runtime.getRuntime.availableProcessors()
    val executor = Executors.newFixedThreadPool(numThreads)
    val dataMap = new ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]()

    val startTime = System.currentTimeMillis()
    println(s"Processing started at: ${new java.util.Date(startTime)}")

    val tasks = (1 to numThreads).map { _ =>
      executor.submit(new Runnable {
        override def run(): Unit = processJsonChunk(filePath, dataMap)
      })
    }

    tasks.foreach(_.get())
    executor.shutdown()

    val endTime = System.currentTimeMillis()
    println(s"Processing finished at: ${new java.util.Date(endTime)}")

    val timeTakenMillis = endTime - startTime
    val timeTakenMinutes = timeTakenMillis / 60000.0
    println(s"Total time taken: $timeTakenMinutes minutes")

    println(s"Processed data size: ${dataMap.size()}")
  }

  private def processJsonChunk(filePath: String, dataMap: ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]): Unit = {
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

  private def processEvent(eventId: String, timestamp: Long, objectId: Int, attributeName: String, attributeValue: String,
                           dataMap: ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]): Unit = {
    val objectMap = dataMap.computeIfAbsent(objectId, _ => new ConcurrentHashMap[String, String]())
    val existingValue = objectMap.get(attributeName)
    if (existingValue == null || existingValue.toLong < timestamp) {
      objectMap.put(attributeName, attributeValue)
    }
  }

}

//  Processed data size: 1000000
//  Total time taken: 51 minutes
//
//  Process finished with exit code 0