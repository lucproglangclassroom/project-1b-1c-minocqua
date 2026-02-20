package edu.luc.cs.cs371.topwords

case class WordFreq(word: String, frequency: Int):
  override def toString: String = s"$word: $frequency"

object CloudBuilder:
  def buildCloud(wordCounts: Map[String, Int], howMany: Int): Seq[WordFreq] =
    wordCounts
      .map { case (word, count) => WordFreq(word, count) }
      .toSeq
      .sortBy(wf => (-wf.frequency, wf.word))
      .take(howMany)

  def formatCloud(wordFreqs: Seq[WordFreq]): String =
    wordFreqs.map(_.toString).mkString(" ")