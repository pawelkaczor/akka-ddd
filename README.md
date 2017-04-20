akka-ddd [![Build Status](https://travis-ci.org/pawelkaczor/akka-ddd.svg?branch=master)](https://travis-ci.org/pawelkaczor/akka-ddd) [![Version](https://img.shields.io/maven-central/v/pl.newicom.dddd/akka-ddd-core_2.11.svg?label=version)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Apl.newicom.dddd)
========

[![Join the chat at https://gitter.im/pawelkaczor/akka-ddd](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/pawelkaczor/akka-ddd?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Reusable artifacts for building applications on top of the Akka platform following [CQRS/DDDD](http://abdullin.com/post/dddd-cqrs-and-other-enterprise-development-buzz-words)-based approach. 

Used by: [DDD Leaven Akka Ver 2](https://github.com/pawelkaczor/ddd-leaven-akka-v2)

#### Modules overview

#### akka-ddd-protocol

Defines protocol to be used by the write-front application.
 
#### akka-ddd-messaging

Contains classes usable on both write and read side of the system. 

#### akka-ddd-core

Contains core artifacts to be used on the write side of the system:

- [AggregateRoot](akka-ddd-core/src/main/scala/pl/newicom/dddd/aggregate/AggregateRoot.scala) trait - 
abstract persistent, event sourced actor responsible for processing commands received from the office. 
Implementation of AggregateRoot trait represents concrete business entity (i.e Reservation, Product, etc) 
See example AR: [Reservation](https://github.com/pawelkaczor/ddd-leaven-akka-v2/blob/master/sales/write-back/src/main/scala/ecommerce/sales/Reservation.scala)
   
- [Office](akka-ddd-core/src/main/scala/pl/newicom/dddd/office/OfficeFactory.scala) - 
an actor that is used by the client to talk to Aggregate Roots of a particular class. 
See [Don't call me, call my office](http://pkaczor.blogspot.com/2014/04/reactive-ddd-with-akka-lesson-2.html#office) for explanation. 
There are two office implementations: ["simple"](akka-ddd-test/src/main/scala/pl/newicom/dddd/office/SimpleOffice.scala) 
(used for testing) and ["global" / distributed](akka-ddd-core/src/main/scala/pl/newicom/dddd/cluster/ShardingSupport.scala) 
(implemented using [Akka Sharding](http://doc.akka.io/docs/akka/current/scala/cluster-sharding.html))

- [Receptor](akka-ddd-core/src/main/scala/pl/newicom/dddd/process/Receptor.scala) - 
allows one office to react on events occurred in another office. Receptor is capable of filtering input events received from configured event stream, transforming them into arbitrary output messages and routing output messages to configured fixed or dynamic (derived from the message) destination. To make configuration of these capabilities straightforward ReceptorBuilder provides simple DSL. [Here](https://github.com/pawelkaczor/ddd-leaven-akka-v2/blob/20160731/shipping/write-back/src/main/scala/ecommerce/shipping/PaymentReceptor.scala) you can find an example configuration of typical receptor that receives event from one office and sends command to another office. Actual logic of reading events from event stream is pluggable - [EventStreamSubscriber](akka-ddd-messaging/src/main/scala/pl/newicom/dddd/messaging/event/EventStreamSubscriber.scala) must be mixed into Receptor class. Ready to use [EventstoreSubscriber](eventstore-akka-persistence/src/main/scala/pl/newicom/eventstore/EventstoreSubscriber.scala) is provided by eventstore-akka-persistence module. 

- [Saga / Process Manager](akka-ddd-core/src/main/scala/pl/newicom/dddd/process/Saga.scala) - implementation of [Saga / Process Manager](https://msdn.microsoft.com/en-us/library/jj591569.aspx) pattern. See: [Saga - big picture](https://github.com/pawelkaczor/akka-ddd/wiki/Saga). See example Process Manager: [OrderProcessManager](https://github.com/pawelkaczor/ddd-leaven-akka-v2/blob/master/headquarters/write-back/src/main/scala/ecommerce/headquarters/processes/OrderProcessManager.scala)

#### eventstore-akka-persistence

Incorporates [Akka Persistence journal and snapshot-store](https://github.com/EventStore/EventStore.Akka.Persistence) backed by [Event Store](http://geteventstore.com). Registers [JSON Serializer](eventstore-akka-persistence/src/main/scala/pl/newicom/eventstore/plugin/EventStoreSerializer.scala) as [Akka custom serializer](http://doc.akka.io/docs/akka/snapshot/scala/persistence.html#Custom_serialization) for ```akka.persistence.PersistentRepr``` (wrapper class that is used by Akka Persistence to store event in the journal). Json format is natural choice for Event Store as it enables creating user projections using javascript directly in Event Store. Provides also [EventstoreSubscriber](eventstore-akka-persistence/src/main/scala/pl/newicom/eventstore/EventstoreSubscriber.scala) that should be mixed into the [Receptor](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/process/Receptor.scala) (available in akka-ddd-core).     

#### akka-ddd-scheduling

Provides durable scheduler (backed by Event Store!) that is typically used by Saga to schedule timeout/deadline messages. See: [Durable Scheduler - big picture](https://github.com/pawelkaczor/akka-ddd/wiki/Durable-Scheduler).

#### view-update

Generic artifacts for building view update services that consume events from [Event Store](http://geteventstore.com/) and update a configured view store (i.e. Sql database). See: [View Update Service - big picture](https://github.com/pawelkaczor/akka-ddd/wiki/View-Update-Service)

#### view-update-sql

Sql (defult is Postgresql) specific implementation of view-update artifacts.

#### akka-ddd-test

Allows easy creation of test of Aggregate Root implementations. Supports both "simple" and "global" offices. See [DummyOfficeSpec](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-test/src/test/scala/pl/newicom/dddd/test/dummy/DummyOfficeSpec.scala) and [DummyOfficeWithGenSpec](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-test/src/test/scala/pl/newicom/dddd/test/dummy/DummyOfficeWithGenSpec.scala).

#### akka-ddd-write-front

Artifacts for building http server with use of [Akka Http](http://doc.akka.io/docs/akka-http/current/scala.html) and [Akka Cluster Client](http://doc.akka.io/docs/akka/current/scala/cluster-client.html) responsible for handling commands sent as json messages. Provides infrastructure for demarshalling commands and forwarding them to write-backend application.
