package org.scalawiki.bots.museum

import org.specs2.mutable.Specification

import scala.io.Source

class HtmlParserSpec extends Specification {

  "HtmlParser" should {
    "get list of images" in {
      val is = getClass.getResourceAsStream("/org/scalawiki/bots/museum/imageList.html")
      is !== null
      val s = Source.fromInputStream(is).mkString

      val lines = HtmlParser.trimmedLines(s)
      lines === Seq(
        "Image descriptions",
        "Image list",
        "1. Archaeological Museum. Exterior.",
        "2. Archaeological Museum. Exposition."
      )
    }

    "htmlText" should {
      "join new lines" in {
        HtmlParser.htmlText(
          """ line1
            | line2
            |
            | line3
          """.stripMargin) === "line1 line2 line3"
      }

      "preserve html paragraphs" in {
        HtmlParser.htmlText(
          """<p>line1
            |line2</p>
            |<p>line3</p>
          """.stripMargin).trim ===
          """line1 line2
            |line3""".stripMargin
      }

    }
  }
}