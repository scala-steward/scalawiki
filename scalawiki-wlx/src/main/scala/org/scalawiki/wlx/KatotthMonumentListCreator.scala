package org.scalawiki.wlx

import org.scalawiki.MwBot
import org.scalawiki.wlx.dto.{AdmDivision, Contest, Country, Katotth, Koatuu}
import org.scalawiki.wlx.query.MonumentQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class Koatuu2Katotth(koatuu: Option[String], katotth: Option[AdmDivision], monumentIds: Seq[String]/*, candidates: Candidate*/)

object KatotthMonumentListCreator {

  val UkraineKatotth: Country = new Country("UA", "Ukraine", Seq("uk"), Katotth.regions(() => Some(UkraineKatotth)))
  val regionsKatotth = UkraineKatotth.regions
  val katotthMap = UkraineKatotth.mapByCode

  val UkraineKoatuu: Country = new Country("UA", "Ukraine", Seq("uk"), Koatuu.regionsNew(() => Some(UkraineKoatuu)))
  val regionsKoatuu = UkraineKoatuu.regions
  val koatuuMap = UkraineKoatuu.mapByCode

  val parentCodes = Set("H", "P", "K", "O")

  def main(args: Array[String]): Unit = {
    val ukWiki = MwBot.fromHost(MwBot.ukWiki)
    val contest = Contest.WLMUkraine(2021)
    val query = MonumentQuery.create(contest)
    val monumentDB = MonumentDB.getMonumentDb(contest, query)
    val sequence: Seq[Koatuu2Katotth] = getMapping(monumentDB)

    val (mapped, unmapped) = sequence.partition(_.katotth.nonEmpty)
    val grouped = groupByAdm(mapped)
    reportUnmapped(unmapped)

    Future.sequence(grouped.map { case (adm, k2k) =>
      val pageName = s"Вікіпедія:Вікі любить пам'ятки/новий АТУ/${adm.namesList.tail.mkString("/")}"

//      val toRedirectList = adm.namesList.tail.init ++ Seq(adm.shortNamesList.last)
//      val toRedirect = s"Вікіпедія:Вікі любить пам'ятки/новий АТУ/${toRedirectList.mkString("/")}"
      val monumentIds = k2k.flatMap(_.monumentIds).toSet
      val header = "{{WLM-шапка}}\n"
      val monuments = monumentDB.monuments.filter(m => monumentIds.contains(m.id))
      val koatuuPages = monuments.map(_.page).distinct.sorted
      val pageText = header + monuments.map(_.asWiki()).mkString("") + "\n|}" +
        "\n== Взято з ==\n" + koatuuPages.map(page => s"* [[$page]]").mkString("\n") +
        "\n== Примітки ==\n{{reflist}}"

//      val toDeleteText = "{{q-delete|author request}}"
//      if (toRedirect != pageName) {
//        val toRedirectText = s"#redirect[[$pageName]]"
//        ukWiki.page(toRedirect).edit(toRedirectText)
//      }
      ukWiki.page(pageName).edit(pageText)
    })
  }

  def groupByAdm(sequence: Seq[Koatuu2Katotth]): Map[AdmDivision, Seq[Koatuu2Katotth]] = {
    sequence.groupBy { k2k =>
      val k = k2k.katotth.get
      var groupK = k

      while (!groupK.regionType.exists(rt => parentCodes.contains(rt.code)) && groupK.parent().nonEmpty) {
        groupK = groupK.parent().get
      }
      groupK
    }
  }

  def reportUnmapped(sequence: Seq[Koatuu2Katotth]) = {
    // TODO
  }

  def getMapping(monumentDB: MonumentDB): Seq[Koatuu2Katotth] = {
    val placeByMonumentId = monumentDB.placeByMonumentId

    monumentDB.monuments.map { m =>
      val koatuuOpt = placeByMonumentId.get(m.id)
      val katotthOpt = koatuuOpt.flatMap { koatuu =>
        val paddedKoatuu = koatuu.padTo(10, "0").mkString
        val candidates = Katotth.toKatotth.getOrElse(paddedKoatuu, Nil).flatMap(katotthMap.get)
        if (candidates.nonEmpty) Some(candidates.maxBy(_.level)) else None
      }

      Koatuu2Katotth(koatuuOpt, katotthOpt, List(m.id))
    }
  }
}
