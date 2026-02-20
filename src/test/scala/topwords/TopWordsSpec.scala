package edu.luc.cs.cs371.topwords
package impl

import org.scalatest.funsuite.AnyFunSuite
import scala.collection.immutable.Queue
import java.io.{ByteArrayOutputStream, PrintStream}

/** CanEqual instances needed for strict equality in tests. */
given CanEqual[Seq[WordFreq], Seq[WordFreq]] = CanEqual.derived
given CanEqual[List[WordFreq], List[WordFreq]] = CanEqual.derived
given CanEqual[Map[String, Int], Map[String, Int]] = CanEqual.derived

/**
 * Unit tests for the purely functional TopWords implementation.
 * Tests each pipeline stage in isolation, the end-to-end composition,
 * and the full runPipeline with captured output.
 */
class TopWordsSpec extends AnyFunSuite:

  val tw = TopWordsFunctional()

  // ==================== splitWords tests ====================

  test("splitWords splits on whitespace and punctuation"):
    val lines = Iterator("hello world", "foo-bar baz")
    val result = tw.splitWords(lines).toList
    assert(result == List("hello", "world", "foo", "bar", "baz"))

  test("splitWords handles empty input"):
    val lines = Iterator.empty[String]
    val result = tw.splitWords(lines).toList
    assert(result == List.empty[String])

  test("splitWords handles empty lines"):
    val lines = Iterator("", "  ", "hello")
    val result = tw.splitWords(lines).toList
    assert(result == List("hello"))

  test("splitWords preserves apostrophes in contractions"):
    val lines = Iterator("don't won't can't")
    val result = tw.splitWords(lines).toList
    assert(result == List("don't", "won't", "can't"))

  test("splitWords handles unicode characters"):
    val lines = Iterator("café naïve résumé")
    val result = tw.splitWords(lines).toList
    assert(result == List("café", "naïve", "résumé"))

  // ==================== filterByLength tests ====================

  test("filterByLength filters short words and lowercases"):
    val words = Iterator("Hi", "Hello", "WORLD", "a", "Functional")
    val result = tw.filterByLength(words, 5).toList
    assert(result == List("hello", "world", "functional"))

  test("filterByLength with minLength 1 keeps all words"):
    val words = Iterator("a", "bb", "CCC")
    val result = tw.filterByLength(words, 1).toList
    assert(result == List("a", "bb", "ccc"))

  test("filterByLength with empty input returns empty"):
    val result = tw.filterByLength(Iterator.empty[String], 5).toList
    assert(result == List.empty[String])

  test("filterByLength excludes words exactly at boundary"):
    val words = Iterator("abcde", "abcd", "abcdef")
    val result = tw.filterByLength(words, 5).toList
    assert(result == List("abcde", "abcdef"))

  // ==================== slidingWordCounts tests ====================

  test("slidingWordCounts with window size 3 produces correct counts"):
    val words = Iterator("aaa", "bbb", "aaa", "ccc", "bbb")
    val results = tw.slidingWordCounts(words, 3).toList
    // Window fills at word 3: [aaa, bbb, aaa] => {aaa:2, bbb:1}
    assert(results.head == Map("aaa" -> 2, "bbb" -> 1))
    // Word 4 (ccc): [bbb, aaa, ccc] => {bbb:1, aaa:1, ccc:1}
    assert(results(1) == Map("bbb" -> 1, "aaa" -> 1, "ccc" -> 1))
    // Word 5 (bbb): [aaa, ccc, bbb] => {aaa:1, ccc:1, bbb:1}
    assert(results(2) == Map("aaa" -> 1, "ccc" -> 1, "bbb" -> 1))

  test("slidingWordCounts with fewer words than window returns empty"):
    val words = Iterator("aaa", "bbb")
    val results = tw.slidingWordCounts(words, 5).toList
    assert(results.isEmpty)

  test("slidingWordCounts with exact window size returns one result"):
    val words = Iterator("aaa", "bbb", "ccc")
    val results = tw.slidingWordCounts(words, 3).toList
    assert(results.length == 1)
    assert(results.head == Map("aaa" -> 1, "bbb" -> 1, "ccc" -> 1))

  test("slidingWordCounts correctly evicts old words"):
    val words = Iterator("aaa", "aaa", "aaa", "bbb")
    val results = tw.slidingWordCounts(words, 3).toList
    // Window [aaa, aaa, aaa] => {aaa:3}
    assert(results.head == Map("aaa" -> 3))
    // Window [aaa, aaa, bbb] => {aaa:2, bbb:1}
    assert(results(1) == Map("aaa" -> 2, "bbb" -> 1))

  test("slidingWordCounts removes words with zero count after eviction"):
    val words = Iterator("aaa", "bbb", "ccc", "ddd")
    val results = tw.slidingWordCounts(words, 3).toList
    // Window [aaa, bbb, ccc] => {aaa:1, bbb:1, ccc:1}
    assert(results.head == Map("aaa" -> 1, "bbb" -> 1, "ccc" -> 1))
    // Window [bbb, ccc, ddd] => aaa evicted, count goes to 0 and is removed
    assert(results(1) == Map("bbb" -> 1, "ccc" -> 1, "ddd" -> 1))
    assert(!results(1).contains("aaa"))

  test("slidingWordCounts window size 1 tracks single word at a time"):
    val words = Iterator("aaa", "bbb", "ccc")
    val results = tw.slidingWordCounts(words, 1).toList
    assert(results.length == 3)
    assert(results(0) == Map("aaa" -> 1))
    assert(results(1) == Map("bbb" -> 1))
    assert(results(2) == Map("ccc" -> 1))

  // ==================== buildCloud tests ====================

  test("buildCloud returns top entries sorted by frequency descending"):
    val counts = Map("alpha" -> 5, "bravo" -> 3, "charlie" -> 8, "delta" -> 1)
    val result = tw.buildCloud(counts, 2)
    assert(result == Seq(WordFreq("charlie", 8), WordFreq("alpha", 5)))

  test("buildCloud breaks ties alphabetically"):
    val counts = Map("bravo" -> 5, "alpha" -> 5, "charlie" -> 5)
    val result = tw.buildCloud(counts, 3)
    assert(result == Seq(WordFreq("alpha", 5), WordFreq("bravo", 5), WordFreq("charlie", 5)))

  test("buildCloud returns at most cloudSize entries"):
    val counts = Map("a" -> 1, "b" -> 2, "c" -> 3, "d" -> 4, "e" -> 5)
    val result = tw.buildCloud(counts, 3)
    assert(result.length == 3)

  test("buildCloud with empty counts returns empty"):
    val result = tw.buildCloud(Map.empty[String, Int], 5)
    assert(result == Seq.empty[WordFreq])

  test("buildCloud with cloudSize larger than available words returns all"):
    val counts = Map("alpha" -> 3, "bravo" -> 1)
    val result = tw.buildCloud(counts, 10)
    assert(result.length == 2)

  // ==================== formatCloud tests ====================

  test("formatCloud formats word frequencies as space-separated string"):
    val cloud = Seq(WordFreq("hello", 5), WordFreq("world", 3))
    val result = tw.formatCloud(cloud)
    assert(result == "hello: 5 world: 3")

  test("formatCloud with empty cloud returns empty string"):
    val result = tw.formatCloud(Seq.empty[WordFreq])
    assert(result == "")

  test("formatCloud with single entry"):
    val cloud = Seq(WordFreq("alpha", 10))
    val result = tw.formatCloud(cloud)
    assert(result == "alpha: 10")

  // ==================== end-to-end pipeline tests ====================

  test("end-to-end pipeline produces correct cloud output"):
    val text = List.fill(10)("alpha bravo charlie delta echo foxtrot").iterator
    val words = tw.splitWords(text)
    val filtered = tw.filterByLength(words, 5)
    val counts = tw.slidingWordCounts(filtered, 6)
    val clouds = counts.map(c => tw.formatCloud(tw.buildCloud(c, 3))).toList
    assert(clouds.nonEmpty)
    assert(clouds.forall(_.nonEmpty))

  test("end-to-end pipeline with known small input produces expected output"):
    // 9 words where all qualify (length >= 3), window = 3, cloud = 2
    val lines = Iterator("aaaaaa aaaaaa bbbbbb bbbbbb bbbbbb cccccc cccccc cccccc cccccc")
    val words = tw.splitWords(lines)
    val filtered = tw.filterByLength(words, 5)
    val counts = tw.slidingWordCounts(filtered, 3)
    val clouds = counts.map(c => tw.buildCloud(c, 2)).toList
    // first window [aaaaaa, aaaaaa, bbbbbb] => {aaaaaa:2, bbbbbb:1} => top2: aaaaaa:2, bbbbbb:1
    assert(clouds.head == Seq(WordFreq("aaaaaa", 2), WordFreq("bbbbbb", 1)))

  // ==================== interactive behavior tests ====================

  test("each input word triggers immediate output when window is full"):
    val inputWords = List("aaaaaa", "bbbbbb", "cccccc", "dddddd", "eeeeee")
    val words = inputWords.iterator
    val filtered = tw.filterByLength(words, 6)
    val counts = tw.slidingWordCounts(filtered, 3)

    // After 3 words, window is full — should get first result
    assert(counts.hasNext)
    val first = counts.next()
    assert(first.nonEmpty)
    // After word 4, should get another result immediately
    assert(counts.hasNext)
    val second = counts.next()
    assert(second.nonEmpty)
    // After word 5, should get another result immediately
    assert(counts.hasNext)
    val third = counts.next()
    assert(third.nonEmpty)
    // No more input
    assert(!counts.hasNext)

  test("interactive: input triggers direct response without buffering"):
    val lines = Iterator("aaaaaa", "bbbbbb", "cccccc", "dddddd")
    val words = tw.splitWords(lines)
    val filtered = tw.filterByLength(words, 5)
    val counts = tw.slidingWordCounts(filtered, 3).toList
    // Should get 2 outputs (once window fills at word 3, then word 4)
    assert(counts.length == 2)

  test("interactive: one output line per input word after window fills"):
    // Verify 1:1 relationship between additional input words and output lines
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    // 10 words (all length >= 6), window = 5 => first output at word 5, then 1 per word
    val input = Iterator(
      "aaaaaa bbbbbb cccccc dddddd eeeeee ffffff gggggg hhhhhh iiiiii jjjjjj"
    )
    Main.runPipeline(input, ps, cloudSize = 3, lengthAtLeast = 6, windowSize = 5, tw = tw)
    ps.flush()
    val outputLines = baos.toString.split("\n").nn.map(_.nn).filter(_.nonEmpty)
    // 10 words - 5 (window fill) + 1 = 6 output lines
    assert(outputLines.length == 6)

  // ==================== runPipeline integration tests ====================

  test("runPipeline produces output to PrintStream"):
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    val input = Iterator(
      "aaaaaa bbbbbb cccccc dddddd eeeeee ffffff gggggg hhhhhh"
    )
    Main.runPipeline(input, ps, cloudSize = 3, lengthAtLeast = 6, windowSize = 5, tw = tw)
    ps.flush()
    val output = baos.toString
    assert(output.nonEmpty)
    // Output should contain word: count format
    assert(output.contains(":"))

  test("runPipeline with empty input produces no output"):
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    Main.runPipeline(Iterator.empty[String], ps, cloudSize = 3, lengthAtLeast = 6, windowSize = 5, tw = tw)
    ps.flush()
    assert(baos.toString.isEmpty)

  test("runPipeline with too few qualifying words produces no output"):
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    // All words are too short for lengthAtLeast=6
    val input = Iterator("hi the to a is it on no do go")
    Main.runPipeline(input, ps, cloudSize = 3, lengthAtLeast = 6, windowSize = 5, tw = tw)
    ps.flush()
    assert(baos.toString.isEmpty)

  test("runPipeline output format matches expected cloud format"):
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    // 6 words all same length, window = 3
    val input = Iterator("aaaaaa aaaaaa aaaaaa")
    Main.runPipeline(input, ps, cloudSize = 2, lengthAtLeast = 6, windowSize = 3, tw = tw)
    ps.flush()
    val output = baos.toString.trim.nn
    // Window: [aaaaaa, aaaaaa, aaaaaa] => aaaaaa: 3
    assert(output == "aaaaaa: 3")

  test("runPipeline respects cloudSize parameter"):
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    val input = Iterator("aaaaaa bbbbbb cccccc dddddd eeeeee")
    Main.runPipeline(input, ps, cloudSize = 2, lengthAtLeast = 6, windowSize = 5, tw = tw)
    ps.flush()
    val outputLines = baos.toString.trim.nn.split("\n").nn.map(_.nn)
    // Each output line should have at most 2 entries (cloudSize=2)
    outputLines.foreach: line =>
      val entries = line.split(" (?=[a-z])").nn
      assert(entries.length <= 2)

  test("runPipeline respects lengthAtLeast parameter"):
    val baos = ByteArrayOutputStream()
    val ps = PrintStream(baos)
    // Mix of short and long words
    val input = Iterator("hi aaaaaa ok bbbbbb no cccccc")
    Main.runPipeline(input, ps, cloudSize = 5, lengthAtLeast = 6, windowSize = 3, tw = tw)
    ps.flush()
    val output = baos.toString
    // Short words (hi, ok, no) should not appear in output
    assert(!output.contains("hi:"))
    assert(!output.contains("ok:"))
    assert(!output.contains("no:"))

end TopWordsSpec
