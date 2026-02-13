package edu.luc.cs.cs371.topwords

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, PrintStream}
import scala.jdk.CollectionConverters.*

class TopWordsJUnit:

  @Test
  def testSmallWindowBehavior(): Unit =
    val input = "a b b a"
    val inBytes = input.getBytes("UTF-8")
    val prevIn = System.in
    val prevOut = System.out
    val baos = new ByteArrayOutputStream()
    try
      System.setIn(new ByteArrayInputStream(inBytes))
      System.setOut(new PrintStream(baos))
      // use small window so output is produced quickly
      edu.luc.cs.cs371.topwords.main.Main.main(Array("-c", "2", "-l", "1", "-w", "2"))
      val lines = baos.toString("UTF-8").linesIterator.toList
      // Expect at least two output lines after the window fills
      assertTrue(lines.size >= 2)
      // First printed cloud after 2 processed words
      assertEquals("a: 1 b: 1", lines(0).trim)
      // Second printed cloud after 3 processed words
      assertEquals("b: 2", lines(1).trim)
    finally
      System.setIn(prevIn)
      System.setOut(prevOut)

  @Test
  def testLongerSequence(): Unit =
    val input = "aa bb cc aa bb aa"
    val prevIn = System.in
    val prevOut = System.out
    val baos = new ByteArrayOutputStream()
    try
      System.setIn(new ByteArrayInputStream(input.getBytes("UTF-8")))
      System.setOut(new PrintStream(baos))
      // window size 3, min length 2, show top 2
      edu.luc.cs.cs371.topwords.main.Main.main(Array("-c", "2", "-l", "2", "-w", "3"))
      val outLines = baos.toString("UTF-8").linesIterator.toList
      assertTrue(outLines.nonEmpty)
      // After the window fills, expect the top words to reflect counts in the last 3 words
      // The sequence of sufficiently-long words: aa bb cc aa bb aa
      // After 3 processed words (aa,bb,cc) -> top two could be aa:1 bb:1 (lex order)
      assertEquals("aa: 1 bb: 1", outLines(0).trim)
    finally
      System.setIn(prevIn)
      System.setOut(prevOut)

end TopWordsJUnit
