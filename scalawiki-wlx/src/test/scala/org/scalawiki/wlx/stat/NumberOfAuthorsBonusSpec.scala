package org.scalawiki.wlx.stat

import org.scalawiki.dto.Image
import org.scalawiki.wlx.{ImageDB, MonumentDB}
import org.scalawiki.wlx.dto.{Contest, ContestType, Country, Monument}
import org.specs2.mutable.Specification

class NumberOfAuthorsBonusSpec extends Specification {
  val contest = Contest(ContestType.WLE, Country.Ukraine, 2019)
  val contestStatStub = ContestStat(contest = contest, startYear = 2019)
  val monumentId1 = "01-123-0001"
  val monument1 = Monument(id = monumentId1, name = "name 1")
  val image1 = Image("File:Image1.jpg", pageId = Some(1)).withAuthor("author 1").withMonument(monumentId1)
  val image2 = Image("File:Image2.jpg", pageId = Some(2)).withAuthor("author 1").withMonument(monumentId1)
  val monumentDbStub = Some(new MonumentDB(contest, Seq(monument1)))
  val imageDbStub = new ImageDB(contest, Nil, monumentDbStub)

  "rater" should {

    "rate non-pictured first year" in {
      val imageDb = imageDbStub.copy(images = Seq(image1))
      val rater = new NumberOfAuthorsBonus(contestStatStub.copy(currentYearImageDb = Some(imageDb), totalImageDb = Some(imageDb)))
      rater.rate("01-123-0001", "author 1") === 9
      rater.explain("01-123-0001", "author 1") === "Not pictured before = 9"
    }

    "rate pictured twice by same author" in {
      val imageDb = imageDbStub.copy(images = Seq(image1))
      val totalImageDb = imageDbStub.copy(images = Seq(image1, image2))
      val rater = new NumberOfAuthorsBonus(contestStatStub.copy(currentYearImageDb = Some(imageDb), totalImageDb = Some(totalImageDb)))
      rater.rate("01-123-0001", "author 1") === 0
      rater.explain("01-123-0001", "author 1") === "Pictured by same author before = 0"
    }
  }
}
