package pl.newicom.dddd.test.dummy

import akka.actor.ExtendedActorSystem
import org.json4s.Formats
import pl.newicom.eventstore.Json4sEsSerializer

class EventStoreSerializer(val sys: ExtendedActorSystem) extends Json4sEsSerializer(sys) {

   override implicit val formats: Formats = dummyOffice.serializationHints ++ defaultFormats


 }
