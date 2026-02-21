package edu.luc.cs.cs371.topwords

import scala.collection.mutable

trait Observer:
  def onStats(stats: WordCloudStats): Unit

case class WordCloudStats(windowSize: Int, wordCounts: Map[String, Int])

class WordFrequencyTracker(
  private val minLength: Int,
  private val windowSize: Int,
  private val observer: Observer
):
  private val window = mutable.Queue[String]()
  private val wordCounts = mutable.Map[String, Int]()

  def processWord(word: String): Unit =
    val lowerWord = word.toLowerCase
    if lowerWord.length >= minLength then
      window.enqueue(lowerWord)
      wordCounts.updateWith(lowerWord)(count => Some(count.getOrElse(0) + 1))

      if window.size > windowSize then
        val oldest = window.dequeue()
        wordCounts.updateWith(oldest) { count =>
          val newCount = count.getOrElse(0) - 1
          if newCount <= 0 then None else Some(newCount)
        }: Unit

      if window.size == windowSize then
        val stats = WordCloudStats(window.size, wordCounts.toMap)
        observer.onStats(stats)