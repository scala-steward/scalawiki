package org.scalawiki.wlx.stat

import org.scalawiki.wlx.dto.{Country, RegionType, SpecialNomination}
import org.specs2.mutable.Specification

class SpecialNominationsSpec extends Specification {

  "SpecialNominations" should {
    "load empty" in {
      SpecialNomination.load("wle_ua.conf") === Seq.empty
    }

    "load all" in {

      val expected = Seq(
        new SpecialNomination("Музичні пам'ятки в Україні", "WLM-рядок",
          Seq("Template:WLM-music-navbar")),
        new SpecialNomination("Пам'ятки дерев'яної архітектури України", "WLM-рядок",
          Seq("Template:WLM Дерев'яна архітектура")),
        new SpecialNomination("Замки і фортеці України", "WLM-рядок", Seq("Template:WLM замки і фортеці")),
        new SpecialNomination("Кримськотатарські пам'ятки в Україні", "WLM-рядок",
          Seq("Вікіпедія:Вікі любить пам'ятки/Кримськотатарські пам'ятки в Україні")),
        new SpecialNomination("Пам'ятки національно-визвольної боротьби", "WLM-рядок",
          Seq("Вікіпедія:Вікі любить пам'ятки/Пам'ятки національно-визвольної боротьби")),
        new SpecialNomination("Грецькі пам'ятки в Україні", "WLM-рядок",
          Seq("Вікіпедія:Вікі любить пам'ятки/Грецькі пам'ятки в Україні")),
        new SpecialNomination("Вірменські пам'ятки в Україні", "WLM-рядок",
          Seq("Вікіпедія:Вікі любить пам'ятки/Вірменські пам'ятки в Україні")),
        new SpecialNomination("Бібліотеки", "WLM-рядок",
          Seq("Вікіпедія:Вікі любить пам'ятки/Бібліотеки")),
        new SpecialNomination("Українські пам'ятки Першої світової війни", "WLM-рядок",
          Seq("Вікіпедія:Вікі любить пам'ятки/Українські пам'ятки Першої світової війни")),
        new SpecialNomination("Цивільні споруди доби Гетьманщини", "WLM-рядок",
          Seq("Template:WLM цивільні споруди доби Гетьманщини")),
        new SpecialNomination("Млини", "WLM-рядок",
          Seq("Template:WLM млини та вітряки"), Seq(2019, 2020, 2021)),
        new SpecialNomination("Єврейська спадщина", "WLM-рядок",
          Seq("Template:WLM єврейська спадщина"), Seq(2019, 2020, 2021)),
        new SpecialNomination("Віа Регіа", "ВЛП-рядок", Nil, Seq(2020, 2021),
          Nil),
        new SpecialNomination("Квіти України", "WLM-рядок",
          Seq("Template:WLM Квіти України"), Seq(2021)),
        new SpecialNomination("Національно-визвольні", "WLM-рядок",
          Seq("Template:WLM національно-визвольні"), Seq(2021)),
        new SpecialNomination("Пам'ятки Подесення", "WLM-рядок",
          Seq("Template:WLM Пам'ятки Подесення"), Seq(2021))
      )

      SpecialNomination.load("wlm_ua.conf") === expected
   }
  }
}
