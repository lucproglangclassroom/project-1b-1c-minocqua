package edu.luc.cs.cs371.topwords
package impl

import org.scalacheck.Properties
import org.scalacheck.Prop.forAll
import org.scalacheck.Gen

/**
 * Property-based tests for the purely functional TopWords implementation.
 * Uses ScalaCheck to verify invariants across random inputs.
 */
object TopWordsCheck extends Properties("TopWords"):

  val tw = TopWordsFunctional()

  // Generator for non-empty alphabetic strings
  val wordGen: Gen[String] = Gen.alphaStr.suchThat(_.nonEmpty)

  // Generator for lists of words
  val wordListGen: Gen[List[String]] = Gen.listOf(wordGen)

  property("splitWords never produces empty strings") = forAll(wordListGen):
    (words: List[String]) =>
      val line = words.mkString(" ")
      tw.splitWords(Iterator(line)).forall(_.nonEmpty)

  property("filterByLength output words all have length >= minLength") = forAll(wordListGen):
    (words: List[String]) =>
      val minLen = 3
      tw.filterByLength(words.iterator, minLen).forall(_.length >= minLen)

  property("filterByLength output words are all lowercase") = forAll(wordListGen):
    (words: List[String]) =>
      tw.filterByLength(words.iterator, 1).forall(w => w == w.toLowerCase)

  property("buildCloud returns at most cloudSize entries") =
    val countsGen = Gen.mapOf(
      for
        word <- wordGen
        count <- Gen.choose(1, 100)
      yield (word, count)
    )
    forAll(countsGen, Gen.choose(1, 20)):
      (counts: Map[String, Int], cloudSize: Int) =>
        tw.buildCloud(counts, cloudSize).size <= cloudSize

  property("buildCloud is sorted by descending frequency") =
    val countsGen = Gen.mapOf(
      for
        word <- wordGen
        count <- Gen.choose(1, 100)
      yield (word, count)
    )
    forAll(countsGen, Gen.choose(1, 20)):
      (counts: Map[String, Int], cloudSize: Int) =>
        val cloud = tw.buildCloud(counts, cloudSize)
        val frequencies = cloud.map(_.frequency)
        frequencies == frequencies.sortBy(-_)

  property("slidingWordCounts with full window has correct size") =
    val longWordGen = Gen.alphaStr.suchThat(_.length >= 5)
    val longWordListGen = Gen.listOfN(20, longWordGen)
    forAll(longWordListGen):
      (words: List[String]) =>
        val windowSize = 5
        val lowerWords = words.map(_.toLowerCase.nn)
        val results = tw.slidingWordCounts(lowerWords.iterator, windowSize).toList
        results.forall(counts => counts.values.sum == windowSize)

end TopWordsCheck
