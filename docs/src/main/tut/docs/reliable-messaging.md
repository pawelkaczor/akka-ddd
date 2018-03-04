---
layout: docs
title: Reliable messaging
position: 4
---

## Reliable delivery

By reliable delivery I mean effectively-once delivery, which takes place when: 

- message is delivered **at least once**,
- message is processed (by the destination actor) **exactly-once**,
- messages are processed (by the destination actor) in the order they were dispatched for delivery by the sender.

Effectively-once delivery (a.k.a. The Business Handshake Pattern) can easily be accomplished if the sender keeps track of the "in delivery" messages and the receiver keeps track of the processed messages. The implementation of the pattern becomes straightforward if both the sender and the receiver are event-sourced actors. 

## Reliable messaging between event-sourced actors

To support the `At-Least-Once` delivery semantics (which is the ground for the reliable delivery) the sender should mix in the [AtLeastOnceDelivery](https://github.com/akka/akka/blob/master/akka-persistence/src/main/scala/akka/persistence/AtLeastOnceDelivery.scala) trait.

The `At-Least-Once` delivery implies that the original message send **order is not always retained** and the destination may receive **duplicate messages** due to possible resends. 

### Deduplication

As long as the actions, that the receiver takes to process the messages, are **idempotent**, the duplicate messages are not a problem. 
Otherwise the receiver should obtain the identifier of the received message and attach it to the event message (as a `CausationID` meta-attribute) that gets written to the receiver journal. The receiver should recognize an incoming message as a duplicate by checking if its ID is one of those IDs collected in the past. Obviously, the detected duplicate message should not be processed by the receiver. Still, the acknowledgment (delivery receipt) should be sent to the sender.

Akka-DDD provides the [Deduplication](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/messaging/Deduplication.scala) trait that implements the logic of the deduplication as described above. The trait is mixed in by the [AggregateRootBase](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/aggregate/AggregateRootBase.scala) and the [SagaBase](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/process/SagaBase.scala) traits. 

***
Akka-DDD guarantees that a message is not processed twice by an Aggregate Root or a Process Manager.

*** 

### Processing messages in proper order

Messages should be processed by the receiver in order they were dispatched for delivery by the sender. 

The sender should maintain the `lastSentMsgIdPerReceiver` map (of type: Map[EntityId, String]) as part of its [DeliveryInProgressState](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-messaging/src/main/scala/pl/newicom/dddd/delivery/DeliveryState.scala#L61). Using this map, when sending a message to a receiver, the sender should obtain the ID of the last message that was sent to the receiver, and attach it to the message being sent as the `MustFollow` meta-attribute. Assuming, the receiver knows the IDs of the messages received/processed in the past (see: Deduplication), it should recognize an incoming message as an out-of-order, if no message was received/processed in the past whose ID was equal to the value of the `MustFollow` meta-attribute of the incoming message. 

***
Akka-DDD guarantees that the Process Manager processes the events in the order they were written to the aggregated business process journal.

*** 

#### Process Manager ⟶ Aggregate Root

Currently, the `MustFollow` meta-attribute is not attached automatically by the Process Managers to the outgoing messages (commands). Thus, when designing a business process logic, in order to avoid out-of-order commands on the receiver side, care must be taken not to define state transitions with an action to send multiple commands to the same Aggregate Root. Instead, intermediary states should be introduced to the process model.
