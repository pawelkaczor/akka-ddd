---
layout: docs
title: Aggregate Root
permalink: /docs/aggregate-root/
---

## <a name="Logical"></a>Logical model

The logical model of the Aggregate Root is purely behavioral. The Aggregate Root exposes a set of business operations (commands) related to a business entity. When a command is received, it is validated against the current state of the AR and either is accepted or rejected. If a command is accepted, an event (or a sequence of events) is stored in the AR journal and the state of the AR is changed. The state of the AR is its internal property, that can never be accessed directly from the outside of the AR. Thus, the structure of the AR (the structure of the AR state) is not a property of the AR logical model. 

The logical model of the AR can only be defined as a set of behavior scenarios, each expressed in the following form:

#### Behavior scenario

##### Given 

* a sequence of commands CG-1...CG-n accepted

##### When

* a command CR received   

##### Then

* either: 
  - an event E (or a sequence of events E-1...E-n) stored in the AR journal
* or
  - the command CR rejected due to a reason R

It turns out that the AR logical model defined as a set of AR behavior scenarios is a perfectly valid, complete specification of the Unit Test that the AR implementation should be accompanied by. 
See: [Testing Aggregate Roots]() 

## <a name="Implementation"></a>Implementation

The Akka-DDD framework allows to model the behavior of the AR as a state machine using the [Algebraic Data Type](). Each type must extend from the [AggregateActions]() trait and implement the `actions` method as shown in the example below.

```scala
def actions =
  handleCommand {
      case ChangeValue(id, newValue) =>
        rejectNegative(newValue) orElse ValueChanged(id, newValue)
    
      case Reset(id) =>
        ValueChanged(id, 0)
  }
  .handleEvent {
      case ValueChanged(_, newValue) =>
        copy(value = newValue)
  }
```
The command processing logic consists of two parts: **command handling (reaction)** and **event handling (state transition)**. The `actions` method is a factory of the Command Handler and Event Handler functions. It creates an instance of [Actions]() containing given Command Handler and Event Handler functions.

### Command Handling (reaction)

The actual command handling logic (the Command Handler) is a partial function that accepts a `Command` and returns a `Reaction`. The `Reaction`, that indicates the acceptance of a command, can be constructed from an event or a sequence of events. The `Reaction`, that indicates the [rejection]() of a command, can be created by providing a rejection reason. 

#### Command Acceptance

The Command Handler indicates that the command is accepted by returning an Event.

```scala
case AddParticipant(id, name) => ParticipantAdded(name, id)
```


Sometimes a single event is not enough. You can build a sequence of events in many ways, for example by transforming an existing sequence of an arbitrary type.

```scala
case RemoveAllParticipants(id) =>
  this.participants  
    .map { name => ParticipantRemoved(name, id) }

// Notice that `participants` is a property of the current state.
```

You can also use a special '&' operator to build a sequence of events from scratch.

```scala
case Reset(id, name) =>
  NameChanged(id, name) & ValueChanged(id, 0, version + 1)

// Notice that `version` is a property of the current state.
```

#### Command Rejection 

There are two ways to reject a command. The first way is to omit the command handler implementation. In this case, the command will be rejected automatically with a rejection reason of type [CommandHandlerNotDefined]().

To indicate explicitly that a command must be rejected (for example due to a validation error), the command handler should return a rejection reason of type [DomainException]() or its subtype. 

The `reject` method should be used to create a rejection: 

```scala
case anyCommand  =>
  reject (LotteryHasAlreadyAWinner(s"Lottery has already a winner and the winner is $winner"))

```

It is also possible to pass a message to the `reject` method. In this case a rejection of type [DomainException]() will be created automatically:

```scala
case Run(_)  =>
  reject("Lottery has no participants")

```

To keep the implementation of the command handler concise, `rejectIf` method can be used as shown below: 

```scala
case CreateDummy(id, name, description, value) =>
  rejectIf(value < 0, "negative value not allowed") orElse
    DummyCreated(id, name, description, value)

```

Again you can either pass a message or a custom rejection reason to the `rejectIf` method. 


#### Collaboration with external actors

Sometimes the command handling logic is more complex and requires the AR Actor to collaborate with other Actors. In such cases the `Reaction` needs to be implemented as a method of the AR Actor and exposed as a property of type `Reaction` of the [Aggregate Root configuration]() object. The command handler will simply return the `Reaction` that is available in the AR configuration. 

The example of collaboration implementation can be found in the DummyAggregateRoot Actor:

```
DummyAggregateRoot code
```    

Finally, the implementation of the command handler:

```scala
case GenerateValue(_) =>
  ctx.config.valueGeneration
```


### Event handling (state transition)


```scala
case ParticipantRemoved(name, id) =>
  val newParticipants = participants.filter(_ != name)
  // NOTE: if last participant is removed, transition back to EmptyLottery
  if (newParticipants.isEmpty)
    EmptyLottery
  else
    copy(participants = newParticipants)

```

### Behavior composition


## <a name="AggregateRootActor"></a>Aggregate Root Actor

[Aggregate Root Actor]() is responsible for running the Aggregate Root state machine (the AR behavior). It is a [Persistent Actor]().
When a [Command Messages]() comes in, the actor invokes the current [Command Handler]() (the Command Handler associated with the current state) and if the command is accepted, appends the [Event Message]()(s) to the actor's journal. Once the event message(s) is persisted, the AR actor invokes the current [Event Handler]() (the Event Handler associated with the current state) and the AR transitions to the next state.

Aggregate Root Actors are not directly accessible by the clients. They are like clerks working in an office. The only way to perform an operation on a particular Aggregate Root (a business entity / case) is to submit a form (command) to the respective Office.

## <a name="Office"></a>Office

TODO

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
OfficeFactory.office[Reservation]
```

Assuming the `newicom.dddd.cluster` package object is available in the scope (the package object is imported), the actual office creation is delegated to the cluster-aware / sharding-capable OfficeFactory object that is injected automatically as an implicit argument of the office method. The office factory requires an `AR Actor Factory` and a shard allocation strategy to be implicitly provided for the given AR Actor class. For the `Reservation` these objects are defined in the [SalesBackendConfiguration]() trait that the [SalesBackendApp]() mixes-in. The `Office` object that is eventually created contains the office identifier and the address (in the form of an ActorPath) of the office representative Actor.


## <a name="Configuration"></a>Configuration

When creating a class of the Aggregate Root Actor or the root trait of the Aggregate Root behavior, it is necessary to declare
the AR configuration interface. If you do not intend to extend the default AR configuration interface, just use the `Config` trait as show in the examples below:

Root trait of the Reservation AR behavior:
```scala
sealed trait ReservationActions extends AggregateActions[Event, ReservationActions, Config] 
```
 
Reservation AR Actor:
```scala
class Reservation(val config: Config) extends AggregateRoot[Event, ReservationActions, Reservation] with ConfigClass[Config]
```

The default configuration interface exposes two properties: `PassivationConfig` and `RespondingPolicy`.

```scala
trait Config {
  def pc: PassivationConfig
  def respondingPolicy: RespondingPolicy
}
```
The `DefaultConfig` defines the `replyWithEvents` flag, that you should set to false if the `SparseReply` responding policy should be used instead of the default one (`ReplyWithEvents`). 

The AR Actor Factory is responsible for creating and **configuring** the AR Actor:

```scala
implicit object ReservationARFactory extends AggregateRootActorFactory[Reservation] {
    def props(pc: PassivationConfig) = Props(new Reservation(DefaultConfig(pc, replyWithEvents = false)))
  }
```

### Responding Policy

[RespondingPolicy]() trait defines the `successMapper` function. The function is responsible for creating a response message that the command sender receives in case the command is accepted.

```scala
trait RespondingPolicy {
  type SuccessMapper = (Seq[OfficeEventMessage]) => Any
  def successMapper: SuccessMapper
}
```

You can define a custom Responding Policy by providing your own implementation of the `RespondingPolicy` trait or you can use one of the built-in policies:

* [SparseReply]() - a generic message is returned
* [ReplyWithEvents]() - the sequence of [OfficeEventMessage]() is returned


### Passivation 
