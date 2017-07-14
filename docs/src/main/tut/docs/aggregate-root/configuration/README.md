---
layout: docs
title: Aggregate Root - Configuration
permalink: /docs/aggregate-root/configuration
---

## Aggregate Root - Configuration

When creating a class of the Aggregate Root Actor or the root trait of the Aggregate Root behavior, it is necessary to declare
the Aggregate Root configuration interface. If you do not intend to extend the default Aggregate Root configuration interface, just use the `Config` trait as show in the examples below:

Root trait of the Reservation AR behavior:
```scala
sealed trait ReservationActions extends AggregateActions[Event, ReservationActions, Config] 
```
 
Reservation AR Actor:
```scala
class Reservation(val config: Config) extends AggregateRoot[Event, ReservationActions, Reservation]
  with ConfigClass[Config]
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
    def props(pc: PassivationConfig) = Props(
      new Reservation(DefaultConfig(pc, replyWithEvents = false))
    )
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

TODO