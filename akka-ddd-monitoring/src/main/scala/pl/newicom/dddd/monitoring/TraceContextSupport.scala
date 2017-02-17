package pl.newicom.dddd.monitoring

import kamon.Kamon
import kamon.trace.{Status, TraceContext, Tracer}
import kamon.util.{MilliTimestamp, RelativeNanoTimestamp}

trait TraceContextSupport {

  def newTraceContext(name: String): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(name))
    } catch {
      case _: NoClassDefFoundError => None // Kamon not initialized, ignore
      case _: ExceptionInInitializerError => None // Kamon not initialized, ignore
    }
  }

  def newTraceContext(name: String, startedOnMillis: Long): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(name,
        token = None,
        tags = Map.empty,
        timestamp = new MilliTimestamp(startedOnMillis).toRelativeNanoTimestamp,
        status = Status.Open,
        isLocal = false))
    } catch {
      case _: NoClassDefFoundError => None // Kamon not initialized, ignore
      case _: ExceptionInInitializerError => None // Kamon not initialized, ignore
    }
  }

  def newLocalTraceContext(name: String, startedOnNanos: Long): Option[TraceContext] = {
    try {
      Some(Kamon.tracer.newContext(name,
        token = None,
        tags = Map.empty,
        timestamp = new RelativeNanoTimestamp(startedOnNanos),
        status = Status.Open,
        isLocal = true))
    } catch {
      case _: NoClassDefFoundError => None // Kamon not initialized, ignore
      case _: ExceptionInInitializerError => None // Kamon not initialized, ignore
    }
  }

  def setCurrentTraceContext(traceContext: TraceContext): Unit =
    Tracer.setCurrentContext(traceContext)

  def currentTraceContext: TraceContext =
    Tracer.currentContext

  def setNewCurrentTraceContext(name: String): Unit =
    newTraceContext(name).foreach(setCurrentTraceContext)

  def finishCurrentTraceContext(): Unit =
    Option(Tracer.currentContext).filterNot(_.isClosed).foreach(_.finish())
}
