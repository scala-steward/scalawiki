package org.scalawiki.query

import java.util.concurrent.TimeUnit

import org.scalawiki.dto.cmd.query.list.EmbeddedIn
import org.scalawiki.dto.cmd.query.prop.rvprop.{Content, Ids, RvProp}
import org.scalawiki.dto.cmd.query.prop.{Info, Prop, Revisions}
import org.scalawiki.dto.cmd.query.{Generator, PageIdsParam, Query}
import org.scalawiki.dto.{Page, Revision}
import org.scalawiki.sql.MediaWiki
import org.scalawiki.sql.dao.PageDao
import org.scalawiki.util.{Command, MockBotSpec}
import org.specs2.mutable.{BeforeAfter, Specification}

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.slick.driver.H2Driver
import scala.slick.driver.H2Driver.simple._

class DslQueryDbCacheSpec extends Specification with MockBotSpec with BeforeAfter {

  sequential

  implicit var session: Session = _
  val pageDao = new PageDao(H2Driver)

  def createSchema() = MediaWiki.createTables()

  override def before = {
    // session = Database.forURL("jdbc:h2:~/test", driver = "org.h2.Driver").createSession()
    session = Database.forURL("jdbc:h2:mem:test", driver = "org.h2.Driver").createSession()
    DslQueryDbCache.session = session
  }

  override def after = session.close()

  override val dbCache: Boolean = true

  val pageText1 = "some vandalism"
  val pageText2 = "more vandalism"

  def revisionContent(content: Option[String]) = content.fold("") {
    text => s""", "*": "$text" """
  }

  def page2(content: Option[String] = None) = s""" "4571809":
    { "pageid": 4571809, "ns": 2, "title": "User:Formator",
      "revisions": [{"revid": 2 ${revisionContent(content)} }]
    }"""

  def page1(content: Option[String] = None) = s""" "569559":
    { "pageid": 569559, "ns": 1, "title": "Talk:Welfare reform",
      "revisions": [{"revid": 1 ${revisionContent(content)} }]
    }"""

  def pagesJson(pages: Seq[String]) =
    s"""{ "query": {"pages": { ${pages.mkString(", ")} }}}"""

  def responseWithRevIds(pages: Seq[String] = Seq(page2(), page1())) =
    pagesJson(pages)

  def responseWithContent(pages: Seq[String] = Seq(page2(Some(pageText2)), page1(Some(pageText1)))) =
    pagesJson(pages)

  "get revisions text with generator and caching" should {
    "first run" in {
      createSchema()

      val expectedCommands = Seq(new Command(Map(
        "action" -> "query",
        "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
        "prop" -> "revisions", "rvprop" -> "ids",
        "continue" -> ""), responseWithRevIds()),
        new Command(Map("action" -> "query",
          "pageids" -> "4571809|569559", "limit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids|content"), responseWithContent())
      )

      val bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content"))

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))

      result must have size 2

      result(0) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), None, None, None, None, Some(pageText2)))
      )
      result(1) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), None, None, None, None, Some(pageText1)))
      )
    }

    "from cache in" in {
      createSchema()

      val expectedCommands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids",
          "continue" -> ""), responseWithRevIds()),

        new Command(Map("action" -> "query",
          "pageids" -> "4571809|569559", "limit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids|content"), responseWithContent()),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids",
          "continue" -> ""), responseWithRevIds())
      )

      val bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content"))

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))

      result must have size 2
      result(0) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), None, None, None, None, Some(pageText2)))
      )
      result(1) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), None, None, None, None, Some(pageText1)))
      )

      pageDao.list.size must be_==(2).eventually
      val byRevs = pageDao.findByRevIds(Seq(4571809L, 569559L), Seq(2, 1))
      byRevs.size === 2
      byRevs.map(_.title) === Seq("Talk:Welfare reform", "User:Formator")
      byRevs.map(_.revisions.head.content.get) === Seq("some vandalism", "more vandalism")

      val futureDb = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content"))

      val resultDb = Await.result(futureDb, Duration(2, TimeUnit.SECONDS))
      resultDb must have size 2
      resultDb(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), Some(0), None, None, None, Some(pageText1),
          textId = resultDb(0).revisions.head.textId))
      )
      resultDb(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), Some(0), None, None, None, Some(pageText2),
          textId = resultDb(1).revisions.head.textId))
      )
    }

    "add page to cache" in {
      createSchema()

      val expectedCommands = Seq(
        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids",
          "continue" -> ""), pagesJson(Seq(page1()))
        ),

        new Command(Map("action" -> "query",
          "pageids" -> "4571809", "limit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids|content"), pagesJson(Seq(page1(Some(pageText1))))
        ),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids",
          "continue" -> ""), pagesJson(Seq(page1(), page2()))
        ),

        new Command(Map("action" -> "query",
          "pageids" -> "569559", "limit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids|content"), pagesJson(Seq(page2(Some(pageText2))))
        ),

        new Command(Map("action" -> "query",
          "generator" -> "categorymembers", "gcmtitle" -> "Category:SomeCategory", "gcmlimit" -> "max",
          "prop" -> "revisions", "rvprop" -> "ids",
          "continue" -> ""), pagesJson(Seq(page1(), page2()))
        )
      )

      val bot = getBot(expectedCommands: _*)

      val future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content"))

      val result = Await.result(future, Duration(2, TimeUnit.SECONDS))

      result must have size 1
      result(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), None, None, None, None, Some(pageText1)))
      )

      pageDao.list.size must be_==(1).eventually
      val byRevs = pageDao.findByRevIds(Seq(569559L), Seq(1))
      byRevs.size === 1
      byRevs.map(_.title) === Seq("Talk:Welfare reform")
      byRevs.map(_.revisions.head.content.get) === Seq("some vandalism")

      val plus1Future = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content"))

      val plus1 = Await.result(plus1Future, Duration(2, TimeUnit.SECONDS))
      plus1 must have size 2
      plus1(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), Some(0), None, None, None, Some(pageText1),
          textId = plus1(0).revisions.head.textId))
      )
      plus1(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), None, None, None, None, Some(pageText2)))
      )

      pageDao.list.size must be_==(2).eventually
      val byRevsFinal = pageDao.findByRevIds(Seq(4571809L, 569559L), Seq(2, 1))
      byRevsFinal.size === 2
      byRevsFinal.map(_.title) === Seq("Talk:Welfare reform", "User:Formator")
      byRevsFinal.map(_.revisions.head.content.get) === Seq("some vandalism", "more vandalism")

      val futureFinal = bot.page("Category:SomeCategory")
        .revisionsByGenerator("categorymembers", "cm", Set.empty, Set("ids", "content"))

      val resultFinal = Await.result(futureFinal, Duration(2, TimeUnit.SECONDS))
      resultFinal must have size 2
      resultFinal(0) === Page(Some(569559L), 1, "Talk:Welfare reform",
        Seq(Revision(Some(1L), Some(569559L), Some(0), None, None, None, Some(pageText1),
          textId = resultFinal(0).revisions.head.textId))
      )
      resultFinal(1) === Page(Some(4571809L), 2, "User:Formator",
        Seq(Revision(Some(2L), Some(4571809L), Some(0), None, None, None, Some(pageText2),
          textId = resultFinal(1).revisions.head.textId))
      )
    }
  }

  "cache" should {
    "return removed revision content parameter" in {

      val namespaces = Set(0)
      val limit = Some("max")
      val title = Some("Template:WLM-row")

      val query = Query(
        Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ),
        Generator(
          new EmbeddedIn(title, None, namespaces, limit)
        )
      )

      val notInDbIds = Seq(1L, 2L, 3L)
      val notInDbQuery = DslQueryDbCache.notInDBQuery(query, notInDbIds)

      notInDbQuery === Query(
        Prop(
          Info(),
          Revisions(RvProp(Ids, Content))
        ),
        PageIdsParam(notInDbIds)
      )
    }
  }

}
