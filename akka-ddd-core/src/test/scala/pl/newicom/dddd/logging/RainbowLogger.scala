package pl.newicom.dddd.logging

import java.util.Date

import akka.event.LoggingAdapter
import pl.project13.scala.rainbow._

class RainbowLogger(source: String) extends LoggingAdapter {

  override def isErrorEnabled: Boolean = true
  override def isWarningEnabled: Boolean = true
  override def isInfoEnabled: Boolean = true
  override def isDebugEnabled: Boolean = true

  override protected def notifyInfo(message: String): Unit = {
    println(fullMessage(message).blue)
  }

  override protected def notifyError(message: String): Unit = {
    println(fullMessage(message).red)
  }

  override protected def notifyError(cause: Throwable, message: String): Unit = {
    println(fullMessage(message + " cased by " + cause.toString).blue)
    cause.printStackTrace()
  }

  override protected def notifyWarning(message: String): Unit = {
    println(fullMessage(message).magenta)
  }

  override protected def notifyDebug(message: String): Unit = {
    println(fullMessage(message).yellow)
  }

  def fullMessage(message: String) = messageInfo + " " + message

  def messageInfo = List(timestamp, source).mkString(" ")

  def timestamp = (new Date).toString
}
