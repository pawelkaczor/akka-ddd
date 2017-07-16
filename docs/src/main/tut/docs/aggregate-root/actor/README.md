---
layout: docs
title: Aggregate Root Actor
permalink: /docs/aggregate-root/actor
---

## Aggregate Root Actor

[Aggregate Root Actor]() is responsible for running the Aggregate Root state machine (the AR behavior). It is a [Persistent Actor]().
When a [Command Messages]() comes in, the actor invokes the current [Command Handler]() (the Command Handler associated with the current state) and if the command is accepted, appends the [Event Message]()(s) to the actor's journal. Once the event message(s) is persisted, the AR actor invokes the current [Event Handler]() (the Event Handler associated with the current state) and the AR transitions to the next state.

Aggregate Root Actors are not directly accessible by the clients. They are like clerks working in an office. The only way to perform an operation on a particular Aggregate Root (a business entity / case) is to submit a form (command) to the respective [Office]().

### Aggregate Root Actor Factory

Aggregate Root Actor Factory should provide a recipe ([Props]()) for the Aggregate Root Actor of specified class. The supervising Office will use that recipe to create the actor. Please see an example of the AR Actor Factory below:

```scala
  implicit object ReservationARFactory extends AggregateRootActorFactory[Reservation] {
    def props(pc: PassivationConfig) = Props(new Reservation(DefaultConfig(pc)))
  }
```
See also: [Aggregate Root Configuration]()

## <a name="Office"></a>Office

The office should publish its identifier to be used across the system by the service consumers and client applications.
The office identifier should allow obtaining the identifier of the office journal and the identifiers of the journals of the individual business entities (cases).

Akka-DDD provides the [RemoteOfficeId] class to be used for that purpose.

The identifier of the sample [Reservation office]() is shown below:

```scala
RemoteOfficeId(
    id           = "Reservation",
    department   = "Sales",
    messageClass = classOf[sales.Command]
)
```
  
The messageClass property defines the base class of all message classes that the office is ready to handle (this information helps to auto-configure the command dispatching that is performed by the write-front application).

### Office creation

```scala
val reservationOffice = OfficeFactory.office[Reservation]
```

Assuming the `newicom.dddd.cluster` package object is available in the scope (the package object is imported), the actual office creation is delegated to the cluster-aware / sharding-capable [OfficeFactory]() object that is injected automatically as an implicit argument of the `office` method. The office factory requires an `AR Actor Factory` and a shard allocation strategy to be implicitly provided for the given AR Actor class. The `Office` object that is eventually created contains the office identifier and the address (in the form of an ActorPath) of the office representative Actor.


## Graceful Passivation

To reduce memory consumption, idle Aggregate Root Actors should be passivated (removed from the memory) after a specified inactivity timeout. To do it [safely]() AR Actor is configured to receive [ReceiveTimeout]() after specified inactivity period. Being notified, AR Actor asks the Office to dismiss him gracefully. The pattern/protocol is called Graceful Passivation and is supported by the Akka-DDD (see [GracefulPassivation]() trait). 

See also: [Passivation Configuration]()

## Collaboration with other Actors

The example of collaboration implementation can be found in the [DummyAggregateRoot]() Actor:
   
```scala
class DummyAggregateRoot(cfg: DummyConfig)
    extends AggregateRoot[DummyEvent, Dummy, DummyAggregateRoot]
    with AggregateRootLogger[DummyEvent]
    with ConfigClass[DummyConfig] {

  val config: DummyConfig = cfg.copy(valueGeneration = valueGeneration)

  lazy val valueGeneratorActor: ActorRef = context.actorOf(ValueGeneratorActor.props(cfg.valueGenerator))

  private def valueGeneration: Collaboration = {
    implicit val timeout = 10.millis
    (valueGeneratorActor !< GenerateRandom) {
      case ValueGeneratorActor.ValueGenerated(value) =>
        state.rejectNegative(value) orElse
          ValueGenerated(id, value, confirmationToken = uuidObj) match {
          case Reject(_) => valueGeneration
          case r         => r
        }
    }
  }
}
```

ValueGeneratorActor is some other actor responsible for generating a random value in response to the `GenerateRandom` message. The Dummy Aggregate Root Actor needs to implement the interaction with the ValueGeneratorActor as a `Collaboration` type and expose it via the [Aggregate Root Configuration]() interface. `Collaboration` is a special subtype of the `Reaction` type. See [Aggregate Root Implementation - Command Handling]() for more information about the `Reaction`.
