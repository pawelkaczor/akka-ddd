package pl.newicom.dddd.aggregate.error

import scala.concurrent.duration.FiniteDuration

@SerialVersionUID(1L)
class CollaborationFailed(msg: String) extends CommandRejected(msg)

case class NoResponseReceived(timeout: FiniteDuration)
  extends CollaborationFailed(s"No response received within $timeout.")

case class UnexpectedResponseReceived(response: Any)
  extends CollaborationFailed(s"Unexpected response received: $response.")
