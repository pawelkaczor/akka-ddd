package pl.newicom.dddd.serialization

import com.typesafe.config.Config
import org.json4s.Formats
import pl.newicom.dddd.serialization.JsonSerHints.NoExtraHints
import pl.newicom.dddd.serialization.JsonSerializationHintsProvider.settingKey
import pl.newicom.dddd.serialization.{JsonAbstractSerHints => AbstractHints, JsonExtraSerHints => ExtraHints, JsonSerHints => FinalizedHints}
import collection.JavaConversions._

import scala.util.Try

class FromConfigJsonSerializationHintsProvider(val config: Config) extends JsonSerializationHintsProvider {

  override def hints(default: Formats): FinalizedHints =
     providers
       .map(hintsFromProvider(default))
       .fold(NoExtraHints)(_ ++ _) match {
         case extra: ExtraHints =>
           FinalizedHints(extra, default)
         case fin: FinalizedHints => fin
     }

  private def hintsFromProvider(default: Formats)(providerName: String): AbstractHints = {
    Class.forName(providerName).getConstructor().newInstance()
     .asInstanceOf[JsonSerializationHintsProvider].hints(default)
  }

  private def providers: List[String] =
    Try(config.getStringList(settingKey).toList).getOrElse(List.empty)

}
