package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.Koatuu.{betterName, shortCode, skipGroups}
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

case class AdmDivisionFlat(codeLevels: Seq[String], name: String, regionType: Option[RegionType]) {
  def code: String = codeLevels.last

  def toHierarchy(regions: Seq[AdmDivision],
                  parent: () => Option[AdmDivision]): AdmDivision = {
    val hier = AdmDivision(code, name, regions, parent, regionType)
    regions.map(_.withParents(() => Some(hier)))
    hier
  }

}

object AdmDivisionFlat {
  def apply(l1: String, l2: String, l3: String, l4: String, name: String, regionType: Option[RegionType]): AdmDivisionFlat = {
    val levels = Seq(l1, l2, l3, l4).filterNot(_.isEmpty)
    val codes = levels.zipWithIndex.map { case (c, level) => shortCode(c, level + 2) }
    AdmDivisionFlat(codes, betterName(name), regionType)
  }
}

object KoatuuNew {

  val readStringFromLong: Reads[String] = implicitly[Reads[Long]].map(x => x.toString)

  def stringOrLong(name: String): Reads[String] = {
    (__ \ name).read[String] or
      (__ \ name).read[String](readStringFromLong)
  }

  implicit val regionReads: Reads[AdmDivisionFlat] = (
    stringOrLong("Перший рівень") and
      stringOrLong("Другий рівень") and
      stringOrLong("Третій рівень") and
      stringOrLong("Четвертий рівень") and
      (__ \ "Назва об'єкта українською мовою").read[String].map(betterName) and
      (__ \ "Категорія").readNullable[String].map(_.flatMap(RegionTypes.codeToType.get))
    ) (AdmDivisionFlat.apply(_, _, _, _, _, _))

  def parse(json: JsValue): Seq[AdmDivisionFlat] = {
    json.as[Seq[AdmDivisionFlat]]
  }

  def makeHierarchy(flat: Seq[AdmDivisionFlat], parent: () => Option[AdmDivision] = () => None): Seq[AdmDivision] = {
    flat.groupBy(_.codeLevels.head).map { case (code, regions) =>
      val (topList, subRegions) = regions.partition(_.codeLevels.size == 1)
      val top = topList.head
      top.toHierarchy(skipGroups(subRegions.map(_.toHierarchy(Nil, parent))), () => Some(Country.Ukraine))
    }.toSeq
  }
}
