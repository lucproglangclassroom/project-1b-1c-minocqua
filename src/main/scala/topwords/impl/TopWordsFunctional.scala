package edu.luc.cs.cs371.topwords
package impl

import scala.collection.immutable.Queue

/**
 * Purely functional implementation of the TopWords trait.
 *
 * All operations are pure functions with no mutable state.
 * The sliding window is maintained using Iterator.scanLeft
 * over an immutable (Queue[String], Map[String, Int]) accumulator.
 */
class TopWordsFunctional extends TopWords:

  def splitWords(lines: Iterator[String]): Iterator[String] =
    lines.flatMap: line =>
      line.split("(?U)[^\\p{Alpha}0-9']+").nn.iterator.map(_.nn).filter(_.nonEmpty)

  def filterByLength(words: Iterator[String], minLength: Int): Iterator[String] =
    words.filter(_.length >= minLength).map(_.toLowerCase.nn)

  def slidingWordCounts(words: Iterator[String], windowSize: Int): Iterator[Map[String, Int]] =
    val initial: (Queue[String], Map[String, Int]) = (Queue.empty[String], Map.empty[String, Int])

    words
      .scanLeft(initial): (state, word) =>
        val (queue, counts) = state
        val newQueue = queue.enqueue(word)
        val newCounts = counts.updatedWith(word):
          case Some(c) => Some(c + 1)
          case None    => Some(1)

        if newQueue.size > windowSize then
          val (oldest, trimmedQueue) = newQueue.dequeue
          val trimmedCounts = newCounts.updatedWith(oldest):
            case Some(c) if c > 1 => Some(c - 1)
            case _                => None
          (trimmedQueue, trimmedCounts)
        else
          (newQueue, newCounts)
      .filter((queue, _) => queue.size >= windowSize)
      .map((_, counts) => counts)

  def buildCloud(wordCounts: Map[String, Int], cloudSize: Int): Seq[WordFreq] =
    wordCounts
      .map((word, count) => WordFreq(word, count))
      .toSeq
      .sortBy(wf => (-wf.frequency, wf.word))
      .take(cloudSize)

  def formatCloud(cloud: Seq[WordFreq]): String =
    cloud.map(_.toString).mkString(" ")

end TopWordsFunctional
