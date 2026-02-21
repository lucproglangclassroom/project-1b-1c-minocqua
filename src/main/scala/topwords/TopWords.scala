package edu.luc.cs.cs371.topwords

import scala.collection.immutable.Queue

/** Data type for a word with its frequency count. */
case class WordFreq(word: String, frequency: Int):
  override def toString: String = s"$word: $frequency"

/** CanEqual instance for WordFreq to support strict equality. */
given CanEqual[WordFreq, WordFreq] = CanEqual.derived

/**
 * Abstract trait defining the purely functional pipeline operations
 * for computing top-word clouds from a stream of text.
 *
 * Implementations must be purely functional with no mutable state.
 */
trait TopWords:

  /** Split lines of text into individual word tokens. */
  def splitWords(lines: Iterator[String]): Iterator[String]

  /** Filter words by minimum length and normalize to lowercase. */
  def filterByLength(words: Iterator[String], minLength: Int): Iterator[String]

  /**
   * Compute word frequency counts over a sliding window of words.
   * Uses Iterator.scanLeft over an immutable (Queue, Map) state.
   * Emits a Map[String, Int] for each step once the window is full.
   */
  def slidingWordCounts(words: Iterator[String], windowSize: Int): Iterator[Map[String, Int]]

  /** Build a word cloud from frequency counts, sorted by descending frequency. */
  def buildCloud(wordCounts: Map[String, Int], cloudSize: Int): Seq[WordFreq]

  /** Format a word cloud as a single-line string. */
  def formatCloud(cloud: Seq[WordFreq]): String

end TopWords