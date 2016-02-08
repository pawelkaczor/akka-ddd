package pl.newicom.dddd.monitoring

import kamon.Kamon
import kamon.trace.{Tracer, TraceContext}
import kamon.util.RelativeNanoTimestamp

trait TraceContextSupport {

  def newTraceContext(name: String): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(name))
    } catch {
      case e: NoClassDefFoundError => None // Kamon not initialized, ignore
      case e: ExceptionInInitializerError => None // Kamon not initialized, ignore
    }
  }

  def newTraceContext(name: String, startedOn: Long): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(name, None, new RelativeNanoTimestamp(startedOn), isOpen = true, isLocal = true))
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

  def setNewCurrentTraceContext(name: String, startedOn: Long): Unit =
    newTraceContext(name, startedOn).foreach(setCurrentTraceContext)

  def finishCurrentTraceContext(): Unit =
    Option(Tracer.currentContext).foreach(_.finish())
}
