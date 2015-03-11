package org.scalawiki.dto.history

import org.scalawiki.dto.Revision
import org.specs2.mutable.Specification
import com.github.nscala_time.time.Imports._

class RevisionFilterSpec extends Specification {

  "revision filter by date"  should {

    val now: DateTime = DateTime.now

    "filter from" in {
      val r1 = Revision(1).withTimeStamp(now - 2.months)
      val r2 = Revision(2).withTimeStamp(now)

      val rf = new RevisionFilter(from = Some(now - 1.month))

      val filtered = rf(Seq(r1,r2))

      filtered.size === 1
      filtered.head.revId === Some(2)
    }

    "filter to" in {
      val r1 = Revision(1).withTimeStamp(now - 2.months)
      val r2 = Revision(2).withTimeStamp(now)

      val rf = new RevisionFilter(to = Some(now - 1.month))

      val filtered = rf(Seq(r1,r2))

      filtered.size === 1
      filtered.head.revId === Some(1)
    }

    "filter from and to" in {
      val r1 = Revision(1).withTimeStamp(now - 3.months)
      val r2 = Revision(2).withTimeStamp(now - 2.months)
      val r3 = Revision(3).withTimeStamp(now - 1.months)
      val r4 = Revision(4).withTimeStamp(now)

      val rf = new RevisionFilter(from = Some(now - 2.month), to = Some(now - 1.month))

      val filtered = rf(Seq(r1,r2, r3, r4))

      filtered.size === 2
      filtered.map(_.revId) === Seq(2, 3)
    }
  }

}
