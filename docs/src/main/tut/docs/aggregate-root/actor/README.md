---
layout: docs
title: Aggregate Root Actor
permalink: /docs/aggregate-root/actor
---

## Aggregate Root Actor

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
