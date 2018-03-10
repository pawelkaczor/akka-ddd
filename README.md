akka-ddd [![Build Status](https://travis-ci.org/pawelkaczor/akka-ddd.svg?branch=master)](https://travis-ci.org/pawelkaczor/akka-ddd) [![Version](https://img.shields.io/maven-central/v/pl.newicom.dddd/akka-ddd-core_2.12.svg?label=version)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Apl.newicom.dddd)
========

[![Join the chat at https://gitter.im/pawelkaczor/akka-ddd](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/pawelkaczor/akka-ddd?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Akka-DDD is a framework for building distributed services following DDD/CQRS/ES architecture on top of the Akka platform. Thanks to the pluggable architecture of the Akka-Persistence, Akka-DDD is not tied to any particular event journal provider.

The services are built as actor systems. Different services can be distributed in the same cluster (Akka cluster) or can be deployed to independent clusters.

Akka-DDD offers concise APIs for implementing business logic of the following actor types:

- Aggregate Root
- Process Manager
- Receptor

All of these are event-sourced actors that support reliable (effectively-once delivery) communication. 

Akka-DDD has been tested with the [EventStore](https://eventstore.org/) journal implementation. A [demo project](https://github.com/pawelkaczor/ddd-leaven-akka-v2) of an e-commerce system implemented using Akka-DDD is available on GitHub.

#### Modules overview

#### akka-ddd-core

Contains core artifacts to be used on the write side of the system:

- [AggregateRoot](akka-ddd-core/src/main/scala/pl/newicom/dddd/aggregate/AggregateRoot.scala) trait - 
abstract persistent, event sourced actor responsible for processing commands received from the office. 
Implementation of AggregateRoot trait represents concrete business entity (i.e Reservation, Product, etc) 

For more information visit the [documentation](http://newicom.pl/akka-ddd/docs/aggregate-root.html).
   
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

Incorporates [Akka Persistence journal and snapshot-store](https://github.com/EventStore/EventStore.Akka.Persistence) backed by [Event Store](http://eventstore.org). Registers [JSON Serializer](eventstore-akka-persistence/src/main/scala/pl/newicom/eventstore/plugin/EventStoreSerializer.scala) as [Akka custom serializer](http://doc.akka.io/docs/akka/snapshot/scala/persistence.html#Custom_serialization) for ```akka.persistence.PersistentRepr``` (wrapper class that is used by Akka Persistence to store event in the journal). Provides also [EventstoreSubscriber](eventstore-akka-persistence/src/main/scala/pl/newicom/eventstore/EventstoreSubscriber.scala) that should be mixed into the [Receptor](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/process/Receptor.scala) (available in akka-ddd-core).     

#### akka-ddd-scheduling

Provides durable scheduler (backed by the Event Store!) that is typically used by a Saga to schedule timeout/deadline messages. See: [Durable Scheduler - big picture](https://github.com/pawelkaczor/akka-ddd/wiki/Durable-Scheduler).

#### view-update

Generic artifacts for building View Update Service - a service that consumes events from an Event Store and updates a View Store (i.e. SQL database). See: [View Update Service - big picture](https://github.com/pawelkaczor/akka-ddd/wiki/View-Update-Service)

#### view-update-sql

SQL-specific implementation of view-update artifacts.

#### akka-ddd-test

Allows easy creation of Aggregate Root specifications (tests). See [Testing Aggregate Root’s behavior](http://newicom.pl/akka-ddd/docs/aggregate-root/testing).

#### akka-ddd-write-front

Provides building blocks for the write-front application. 
 
 - **HttpCommandHandler** - a route (building block of the [Akka Http](http://doc.akka.io/docs/akka-http/current/scala.html) endpoint) responsible for unmarshalling commands received in json format as HTTP POST requests. Once the command is unmarshalled, the handler passes it further to the **CommandDispatcher**. Eventually, once the command is processed on the backend and the response is received from the office (asynchronously), the handler converts it to an appropriate HTTP response that needs to be returned to the client.

 - **CommandDispatcher** - takes care of forwarding the incoming commands to the appropriate offices operating on backend. The forwarding is performed using the [Akka Cluster Client](http://doc.akka.io/docs/akka/current/scala/cluster-client.html).
