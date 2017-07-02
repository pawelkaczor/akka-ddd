package pl.newicom.dddd.persistence

import akka.persistence.journal.{Tagged, WriteEventAdapter}
import com.typesafe.config.{Config, ConfigFactory, ConfigList}
import pl.newicom.dddd.messaging.event.EventMessage
import pl.newicom.dddd.persistence.TaggingEventAdapter.tagsByEvent

import scala.collection.JavaConverters._
import scala.collection.mutable

object TaggingEventAdapter {

  def config: Config = ConfigFactory.parseResourcesAnySyntax("tags")

  val tagsByEvent: Map[String, List[String]] = {
    val map = new mutable.HashMap[String, mutable.Set[String]] with mutable.MultiMap[String, String]
    config.root().entrySet().asScala.foreach { entry =>
      entry.getValue.asInstanceOf[ConfigList].unwrapped().asScala.foreach { v =>
        map.addBinding(entry.getKey, v.asInstanceOf[String])
      }
    }
    reverseMultimap(map.toMap)
  }

  private def reverseMultimap[T1, T2](myMap: Map[T1, mutable.Set[T2]]): Map[T2, List[T1]] = {
    val instances = for {
      keyValue <- myMap.toList
      value <- keyValue._2
    } yield (value, keyValue._1)
    instances.groupBy(_._1).map(kv => kv._1 -> kv._2.map(_._2))
  }
}

class TaggingEventAdapter extends WriteEventAdapter {

  def toJournal(msg: Any): Any = msg match {
    case em: EventMessage =>
      val tags = em.tags ++ tagsByEvent.getOrElse(em.payloadFullName, Set())
      if (tags.isEmpty) em else Tagged(em.withTags(tags.toSeq :_*), tags)
    case _ => msg
  }

  def manifest(event: Any): String = ""
}
