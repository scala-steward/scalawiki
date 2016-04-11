package org.scalawiki.dto

case class Site(
                 langCode: Option[String],
                 family: String,
                 domain: String,
                 protocol: String = "https",
                 scriptPath: String = "/w")

object Site {

  def wikipedia(langCode: String) = project(langCode, "wikipedia")

  val commons = wikimedia("commons")

  val meta = wikimedia("meta")

  val enWiki = wikipedia("en")

  val ukWiki = wikipedia("uk")

  val localhost = Site(None, "wikipedia", "localhost", "http", "/mediawiki")

  def project(langCode: String, family: String) =
    Site(Some(langCode), family, s"$langCode.$family.org")

  def wikimedia(code: String) = Site(None, code, s"$code.wikimedia.org")

  def host(host: String): Site = {
    val list = host.split("\\.").toList
    list match {
      case code :: "wikimedia" :: "org" :: Nil => wikimedia(code)
      case code :: family :: "org" :: Nil => project(code, family)
      case _ => Site(None, host, host)
    }
  }



}