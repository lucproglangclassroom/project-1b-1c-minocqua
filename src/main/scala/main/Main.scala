package edu.luc.cs.cs371.topwords
package main

import scala.collection.mutable
import scala.util.Using
import java.io.{IOException, PrintWriter}
import java.util.logging.{Level, Logger}

trait Observer:
  def update(stats: Seq[(String, Int)]): Unit

class ConsoleObserver(out: PrintWriter = new PrintWriter(System.out, true)) extends Observer:
  def update(stats: Seq[(String, Int)]): Unit =
    try
      val line = stats.map{ case (w, f) => s"${w}: ${f}" }.mkString(" ")
      out.println(line)
      out.flush()
    catch
      case _: IOException =>
        System.exit(0)

object Main:
  private val logger = Logger.getLogger("edu.luc.cs.cs371.topwords.Main")

  def parseArgs(args: Array[String]): (Int, Int, Int) =
    var howMany = 10
    var minLen = 6
    var lastN = 1000
    var i = 0
    while i < args.length do
      args(i) match
        case "-c" | "--cloud-size" if i + 1 < args.length => howMany = args(i + 1).toInt; i += 2
        case "-l" | "--length-at-least" if i + 1 < args.length => minLen = args(i + 1).toInt; i += 2
        case "-w" | "--window-size" if i + 1 < args.length => lastN = args(i + 1).toInt; i += 2
        case unknown =>
          logger.log(Level.WARNING, s"Unknown arg: ${unknown}")
          i += 1
    (howMany, minLen, lastN)

  def main(args: Array[String]): Unit =
    val (howMany, minLen, lastN) = parseArgs(args)
    logger.log(Level.CONFIG, s"howMany=${howMany} minLength=${minLen} lastNWords=${lastN}")

    val lines = scala.io.Source.stdin.getLines()
    import scala.language.unsafeNulls
    val words = lines.flatMap(l => l.split("(?U)[^\\p{Alpha}0-9']+"))

    val queue = new mutable.ArrayDeque[String]()
    val counts = mutable.Map.empty[String, Int]
    val observer: Observer = new ConsoleObserver()

    var processed = 0
    try
      for w0 <- words do
        val w = w0
        if w.length >= minLen then
          queue.append(w)
          counts.updateWith(w){
            case None => Some(1)
            case Some(n) => Some(n + 1)
          }
          processed += 1
          if queue.size > lastN then
            val ev = queue.removeHead()
            (counts.updateWith(ev){
              case None => None
              case Some(n) if n <= 1 => None
              case Some(n) => Some(n - 1)
            }): Unit

          if processed >= lastN then
            val top = counts.toSeq.sortWith{ (a, b) =>
              if a._2 != b._2 then a._2 > b._2 else a._1 < b._1
            }.take(howMany)
            observer.update(top)
    catch
      case _: IOException => // broken pipe or other IO error writing to stdout
        System.exit(0)
      case ex: Exception =>
        logger.log(Level.SEVERE, "Unhandled exception", ex)
        System.exit(1)

end Main

