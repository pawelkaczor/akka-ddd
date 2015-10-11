package pl.newicom.dddd.serialization

import org.json4s.Formats

object JsonSerializationHintsProvider {
  val settingKey = "serialization.json.hints.providers"
}

trait JsonSerializationHintsProvider {

  def hints(default: Formats = JsonSerHints.DefaultSerializationHints): JsonAbstractSerHints

}