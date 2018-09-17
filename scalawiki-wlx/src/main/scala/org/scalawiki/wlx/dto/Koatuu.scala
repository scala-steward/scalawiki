package org.scalawiki.wlx.dto

import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

object Koatuu {

  implicit lazy val regionReads: Reads[Region] = (
    (__ \ "code").read[String] and
      (__ \ "name").read[String] and
      (__ \ "level2").lazyReadNullable(Reads.seq[Region](regionReads)).map(_.getOrElse(Nil))
    ) (Region)

  def regions: Seq[Region] = {
    val stream = getClass.getResourceAsStream("/koatuu.json")
    val json = Json.parse(stream)

    val raw = (json \ "level1").as[Seq[Region]]

    raw.map { r =>
      r.copy(
        code = shortCode(r.code),
        name = betterName(r.name)
      )
    }
  }

  def shortCode(s: String) =
    s.take(2)

  def betterName(s: String) =
    s.split("/").head
      .replace("М.", "")
      .toLowerCase.capitalize
      .replace("республіка", "Республіка")
      .replace("крим", "Крим")
      .replace("Івано-франківська", "Івано-Франківська")

  def main(args: Array[String]): Unit = {
    println(regions)
  }

}