package ltd.kalai.les.assignments2

import ltd.kalai.les.assignments2.JsonProcessingUtils.*

import java.util.concurrent.{ConcurrentHashMap, Executors}

object ParallelJsonProcessor {
  def main(args: Array[String]): Unit = {
    val filePath = "/Users/shan/Les/Assignments_2/large_events.json"
    val numThreads = Runtime.getRuntime.availableProcessors()
    val executor = Executors.newFixedThreadPool(numThreads)
    val dataMap = new ConcurrentHashMap[Int, ConcurrentHashMap[String, String]]()

    val startTime = System.currentTimeMillis()

    // Submit tasks to process JSON in parallel
    val tasks = (1 to numThreads).map { _ =>
      executor.submit(new Runnable {
        override def run(): Unit = {
          processJsonFile(filePath, dataMap, processEvent)
        }
      })
    }

    tasks.foreach(_.get())
    executor.shutdown()

    val endTime = System.currentTimeMillis()

    val totalTimeMinutes = (endTime - startTime) / (1000 * 60)

    println(s"Processed data size: ${dataMap.size()}")
    println(s"Total time taken: $totalTimeMinutes minutes")
  }

}


//  Processed data size: 1000000
//  Total time taken: 51 minutes
//  
//  Process finished with exit code 0
