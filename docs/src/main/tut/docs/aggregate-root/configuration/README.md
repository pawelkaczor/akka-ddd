---
layout: docs
title: Aggregate Root - Configuration
permalink: /docs/aggregate-root/configuration
---

## Aggregate Root - Configuration

When creating a class of the Aggregate Root Actor or the root trait of the Aggregate Root behavior, it is necessary to declare
the Aggregate Root configuration interface. If you do not intend to extend the default Aggregate Root configuration interface, just use the [Config]() trait as show in the examples below:

Root trait of the Reservation AR behavior:
```scala
sealed trait Reservation extends Behavior[Event, Reservation, Config] 
```
 
Reservation AR Actor:
```scala
class ReservationAggregateRoot(val config: Config) extends AggregateRoot[Event, Reservation, ReservationAggregateRoot]
  with ConfigClass[Config]
```

The default configuration interface exposes two properties: `PassivationConfig` and `RespondingPolicy`.

### Passivation Configuration

The inactivity timeout, defined by the [Graceful Passivation]() protocol, can be configured independently for each Aggregate Root Actor class. It is the responsibility of the Office to create [PassivationConfig]() object with default value for inactivity timeout. The value can be adjusted by the [Aggregate Root Actor Factory]().


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


The default configuration class (`DefaultConfig`) defines the `replyWithEvents` flag, that you should set to false if the `SparseReply` responding policy should be used instead of the default one (`ReplyWithEvents`). 

See also: [Aggregate Root Actor Factory]()

### Logging

To enable extensive logging of the Aggregate Root Actor, use [AggregateRootLogger]() trait. It should be mixed into the the Aggregate Root Actor class.

Received commands, executed reactions, and the current state will be included in the log, as shown in the example below:

Lottery AR test scenario:

```scala
"Lottery should" should {
    "run" in {
      given {
        a_list_of [CreateLottery, AddParticipant, AddParticipant]
      }
      .when {
        a [Run]
      }
      .expectEventMatching {
        case e: WinnerSelected => e
      }
}
```

Generated log messages:
 
```text
13:17:23.775UTC DEBUG akka://Tests/user/LotteryAggregateRoot_26aaff - Actor created b02772909
13:17:24.090UTC DEBUG LotteryAggregateRoot-b02772909 - Command received: CreateLottery(b02772909)
13:17:24.150UTC DEBUG LotteryAggregateRoot-b02772909 - Command accepted. LotteryCreated(b02772909)
13:17:24.176UTC DEBUG LotteryAggregateRoot-b02772909 - State: EmptyLottery
13:17:24.191UTC DEBUG LotteryAggregateRoot-b02772909 - Command received: AddParticipant(b02772909,Paul)
13:17:24.194UTC DEBUG LotteryAggregateRoot-b02772909 - Command accepted. ParticipantAdded(Paul,b02772909)
13:17:24.198UTC DEBUG LotteryAggregateRoot-b02772909 - State: NonEmptyLottery(List(Paul))
13:17:24.199UTC DEBUG LotteryAggregateRoot-b02772909 - Command received: AddParticipant(b02772909,John)
13:17:24.203UTC DEBUG LotteryAggregateRoot-b02772909 - Command accepted. ParticipantAdded(John,b02772909)
13:17:24.205UTC DEBUG LotteryAggregateRoot-b02772909 - State: NonEmptyLottery(List(John, Paul))
13:17:24.255UTC DEBUG akka://Tests/user/LotteryAggregateRoot_4402a5 - Actor created b02772909
13:17:24.264UTC DEBUG LotteryAggregateRoot-b02772909 - Command received: Run(b02772909)
13:17:24.267UTC DEBUG LotteryAggregateRoot-b02772909 - Command accepted. WinnerSelected(John,2017-07-16T15:17:24.265+02:00,b02772909)
13:17:24.269UTC DEBUG LotteryAggregateRoot-b02772909 - State: FinishedLottery(John,b02772909)
```