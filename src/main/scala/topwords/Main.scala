package edu.luc.cs.cs371.topwords

import mainargs.{main, arg, ParserForMethods}
import java.io.PrintStream

/**
 * Entry point for the topwords application.
 *
 * Composes the purely functional pipeline: stdin lines → words → filtered →
 * sliding window counts → word clouds → formatted output.
 *
 * Handles SIGPIPE by catching IOException and checking PrintStream.checkError().
 */
object Main:

  /**
   * Run the topwords pipeline, reading from the given input and writing to the given output.
   * This method is separated from main for testability.
   */
  def runPipeline(
    input: Iterator[String],
    output: PrintStream,
    cloudSize: Int,
    lengthAtLeast: Int,
    windowSize: Int,
    tw: TopWords = impl.TopWordsFunctional()
  ): Unit =
    val words = tw.splitWords(input)
    val filtered = tw.filterByLength(words, lengthAtLeast)
    val counts = tw.slidingWordCounts(filtered, windowSize)

    try
      counts.foreach: wordCounts =>
        val cloud = tw.buildCloud(wordCounts, cloudSize)
        if cloud.nonEmpty then
          output.println(tw.formatCloud(cloud))
          // Handle SIGPIPE: check if output stream has encountered an error
          if output.checkError() then
            sys.exit(1)
    catch
      case _: java.io.IOException =>
        // Handle SIGPIPE gracefully — broken pipe causes IOException
        ()

  @main
  def run(
    @arg(short = 'c', doc = "Size of the word cloud (number of top words to show)")
    cloudSize: Int = 10,
    @arg(short = 'l', doc = "Minimum word length to consider")
    lengthAtLeast: Int = 6,
    @arg(short = 'w', doc = "Size of moving window of words")
    windowSize: Int = 1000
  ): Unit =
    val lines = scala.io.Source.stdin.getLines()
    runPipeline(lines, System.out, cloudSize, lengthAtLeast, windowSize)

  def main(args: Array[String]): Unit =
    ParserForMethods(this).runOrExit(args.toIndexedSeq)
    ()

end Main