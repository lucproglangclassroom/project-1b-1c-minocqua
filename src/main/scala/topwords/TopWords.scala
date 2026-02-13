package edu.luc.cs.cs371.topwords

import mainargs.{main, arg}

@main
def topwords(
  @arg(short = 'c', doc = "Size of the word cloud (number of top words to show)")
  cloudSize: Int = 10,
  @arg(short = 'l', doc = "Minimum word length to consider")
  lengthAtLeast: Int = 6,
  @arg(short = 'w', doc = "Size of moving window of words")
  windowSize: Int = 1000
): Unit =
  val consoleObserver = new ConsoleObserver(cloudSize)
  val tracker = WordFrequencyTracker(lengthAtLeast, windowSize, consoleObserver)

  val lines = scala.io.Source.stdin.getLines()
  val words = lines.flatMap(line =>
    line.split("(?U)[^\\p{Alpha}0-9']+").filter(_.nonEmpty)
  )

  try
    for word <- words do
      tracker.processWord(word)
  catch
    case _: java.io.IOException =>
      // Handle SIGPIPE gracefully

class ConsoleObserver(cloudSize: Int) extends Observer:
  def onStats(stats: WordCloudStats): Unit =
    val cloud = CloudBuilder.buildCloud(stats.wordCounts, cloudSize)
    if cloud.nonEmpty then
      println(CloudBuilder.formatCloud(cloud))