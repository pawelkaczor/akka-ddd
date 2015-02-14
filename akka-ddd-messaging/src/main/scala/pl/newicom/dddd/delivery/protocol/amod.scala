package pl.newicom.dddd.delivery.protocol

import scala.util.{Success, Try}

/**
 * At-Most-Once Delivery protocol
 */
/***/
object Received                                        extends Receipt
case class Processed(result: Try[Any] = Success("OK")) extends Receipt
