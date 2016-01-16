package pl.newicom.dddd.persistence

import akka.event.{NoLogging, LoggingAdapter}
import akka.persistence.PersistentActor

trait LoggingMode

object LoggingMode {
  case object LiveOnly extends LoggingMode
  case object AlsoWhenRecovering extends LoggingMode
}

/**
  * Suppresses log messages published during recovery unless [[pl.newicom.dddd.persistence.LoggingMode.AlsoWhenRecovering]]
  * is passed as parameter to the log method
  */
trait PersistentActorLogging { this: PersistentActor ⇒
  private var _log: LoggingAdapter = _

  import LoggingMode._

  def log: LoggingAdapter = log(LiveOnly)

  def log(mode: LoggingMode): LoggingAdapter = {
    // only used in Actor, i.e. thread safe
    if (recoveryRunning && mode == LiveOnly) {
      NoLogging
    } else {
      if (_log eq null)
        _log = akka.event.Logging(context.system.eventStream, this)
      _log
    }
  }

}
