package pl.newicom.dddd.monitoring

import kamon.Kamon
import kamon.trace.{TraceContext, Tracer}
import kamon.util.MilliTimestamp

trait TraceContextSupport {

  def newTraceContext(name: String): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(name))
    } catch {
      case e: NoClassDefFoundError => None // Kamon not initialized, ignore
      case e: ExceptionInInitializerError => None // Kamon not initialized, ignore
    }
  }

  def newTraceContext(name: String, startedOnMillis: Long): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(
        name,
        token = None,
        timestamp = new MilliTimestamp(startedOnMillis).toRelativeNanoTimestamp,
        isOpen = true,
        isLocal = false))
    } catch {
      case e: NoClassDefFoundError => None // Kamon not initialized, ignore
      case e: ExceptionInInitializerError => None // Kamon not initialized, ignore
    }
  }

  def setCurrentTraceContext(traceContext: TraceContext): Unit =
    Tracer.setCurrentContext(traceContext)

  def currentTraceContext: TraceContext =
    Tracer.currentContext

  def setNewCurrentTraceContext(name: String): Unit =
    newTraceContext(name).foreach(setCurrentTraceContext)

  def finishCurrentTraceContext(): Unit =
    Option(Tracer.currentContext).filter(_.isOpen).foreach(_.finish())
}
