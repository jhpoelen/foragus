package org.eol.globi.collections

import org.scalatest._
import play.api.libs.json.{JsNumber, JsObject, JsString}

import scala.io.Source

class CollectionBuilder$Test extends FlatSpec with Matchers {

  "A scientific taxon name" should "be transformed with english suffices" in {
    CollectionBuilder.commonize("Phocidae") should be(List("Phocid", "Phocidae"))
    CollectionBuilder.commonize("Arthropoda") should be(List("Arthropoda"))
    CollectionBuilder.commonize("somethingElse") should be(List("somethingElse"))
  }

  "A list of scientific names" should "be transformed into" in {
    val lines: Iterator[String] = Source.fromURL(getClass.getResource("/name-test.csv")).getLines()
    lines.drop(1) foreach (line => {
      val row = line.split(",")
      CollectionBuilder.commonize(row(0)) should contain(row(1))
    })
  }

  "retrieve taxon info" should "include common name" in {
    val rez: Option[(String, Option[String])] = CollectionBuilder.namesForTaxonExternalId(7666L)
    rez should be(Some( """Phocidae""", Some( """true seals""")))
  }

  "retrieve taxon info" should "with taxon w/o common name" in {
    val rez: Option[(String, Option[String])] = CollectionBuilder.namesForTaxonExternalId(11681L)
    rez should be(Some( """Spirogyra""", None))
  }

  "parsing common names" should "split string and language" in {
    val commonNames: Array[Option[(String, String)]] = CollectionBuilder.parseCommonNames(Some( """one@en | zwei@de"""))
    commonNames should contain(Some(("one", "en")))
    commonNames should contain(Some(("zwei", "de")))

    commonNames.flatten should contain("one", "en")
    commonNames.flatten.filter(_._2 == "en") should not(contain("zwei", "de"))
  }

  "parsing an invalid common name" should "split string and language" in {
    val commonNames: Array[Option[(String, String)]] = CollectionBuilder.parseCommonNames(Some( """bla"""))
    commonNames should contain(None)
    commonNames.flatten should be(empty)
    commonNames.flatten.filter(_._2 == "en") should be(empty)
  }

  "parsing an none common name" should "split string and language" in {
    val commonNames: Array[Option[(String, String)]] = CollectionBuilder.parseCommonNames(None)
    commonNames should contain(None)
    commonNames.flatten should be(empty)
    commonNames.flatten.filter(_._2 == "en") should be(empty)
  }

  "finding first english common name" should "return the name" in {
    val englishName = CollectionBuilder.firstEnglishCommonName(Some( """one @en | two @en"""))
    englishName should be(Some("one"))
  }

  "a collection" should "include a link to eol data page and scientific name" in {
    val rez: String = CollectionBuilder.mkCollectionReference(7666L, """Phocidae""")
    rez should be( """This collection was automatically generated from <a href="http://globalbioticinteractions.org">Global Biotic Interactions</a> (GloBI) data. Please visit <a href="http://eol.org/pages/7666/data">this EOL data page</a> for more detailed information about the GloBI interaction data and to find other trait data for Phocidae.""")
  }

  "retrieve taxon info" should "include common name no match" in {
    val rez: Option[(String, Option[String])] = CollectionBuilder.namesForTaxonExternalId(1111111111111111L)
    rez should be(None)
  }

  "create a description" should "include a human readable text" in {
    val commonName = Some( """true seals""")
    val scientificName = """Phocidae"""
    val interactionType = """preysOn"""

    val (collectionName, collectionDescription) = CollectionBuilder.mkCollectionInfo(commonName, scientificName, interactionType)

    collectionName should be( """True Seals Food""")
    collectionDescription should be( """what do true seals eat?
what do phocids eat?
what do phocidae eat?
what do true seals prey on?
what do phocids prey on?
what do phocidae prey on?
what do true seals hunt?
what do phocids hunt?
what do phocidae hunt?
true seals prey
phocid prey
phocidae prey
true seals food
phocid food
phocidae food""".replace("\n", " "))
  }

  "create a description" should "include a human readable text without commonname" in {
    val commonName = None
    val scientificName = """Phocidae"""
    val interactionType = """preysOn"""

    val (collectionName, collectionDescription) = CollectionBuilder.mkCollectionInfo(commonName, scientificName, interactionType)

    collectionName should be( """Phocidae Food""")
    collectionDescription should be( """what do phocids eat?
what do phocidae eat?
what do phocids prey on?
what do phocidae prey on?
what do phocids hunt?
what do phocidae hunt?
phocid prey
phocidae prey
phocid food
phocidae food""".replace("\n", " "))
  }

  "a lucene query" should "be nicely created" in {
    val luceneQuery: String = CollectionBuilder.buildLucenePathQuery(Seq(327955L, 7666L))
    luceneQuery should be( """'path:EOL\\:327955 OR path:EOL\\:7666'""")

  }

  "a query against remote neo4j" should "return something" in {
    val preyIds: Stream[Long] = CollectionBuilder.preyOf(327955L)
    val collectionName = """collectionName"""
    val collectionDescription = """collectionDescription"""
    val eolCollection: JsObject = CollectionBuilder.asEOLCollection(collectionName, collectionDescription, preyIds)
    eolCollection \ "collection" should not(be(null))
    eolCollection \\ "collected_item_type" should contain(JsString("TaxonConcept"))
    eolCollection \\ "collected_item_id" should contain(JsNumber(2849458))
    eolCollection \\ "description" should contain(JsString("collectionDescription"))
    eolCollection \\ "name" should contain(JsString("collectionName"))
  }

}