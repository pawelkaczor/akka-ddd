akka-ddd
========

Reusable artifacts for building applications on top of Akka platform following a [CQRS/DDDD](http://abdullin.com/post/dddd-cqrs-and-other-enterprise-development-buzz-words)-based approach.

Core functionality originally developed as part of [DDD Leaven Akka](https://github.com/pawelkaczor/ddd-leaven-akka) project.

Used by: [DDD Leaven Akka Ver 2](https://github.com/pawelkaczor/ddd-leaven-akka-v2)

#### Modules overview

##### akka-ddd-messaging
Contains base types for commands and events and their envelopes ( command/event messages ).
This module should be used on both write and read side of the application. 

##### akka-ddd-core
Contains core artifacts used on write side of the application:
- [AggregateRoot](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/aggregate/AggregateRoot.scala) trait - abstract persistent, event sourced actor responsible for processing commands received from the office. Implementation of AggregateRoot trait represents concrete business entity (i.e Reservation, Product, etc) See example AR: [DummyAggregateRoot](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-test/src/test/scala/pl/newicom/dddd/test/dummy/DummyAggregateRoot.scala)   
- [Office](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/office/Office.scala) - an actor that is used by the client to talk to Aggregate Roots of particular class. See [Don't call me, call my office](http://pkaczor.blogspot.com/2014/04/reactive-ddd-with-akka-lesson-2.html) for explanation. There are two office implementations: ["local"](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-test/src/main/scala/pl/newicom/dddd/office/LocalOffice.scala) (used for testing) and ["global"](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/cluster/ShardingSupport.scala) (uses [Akka Sharding](http://doc.akka.io/docs/akka/snapshot/contrib/cluster-sharding.html)
- [Saga](https://github.com/pawelkaczor/akka-ddd/tree/master/akka-ddd-core/src/main/scala/pl/newicom/dddd/process) - implementation of [Saga / Process Manager](https://msdn.microsoft.com/en-us/library/jj591569.aspx) pattern. 

##### akka-ddd-test
Allows easy creation of test of Aggregate Root implementations. Supports both "local" and "global" offices. See [DummyOfficeSpec](https://github.com/pawelkaczor/akka-ddd/blob/master/akka-ddd-test/src/test/scala/pl/newicom/dddd/test/dummy/DummyOfficeSpec.scala).

##### akka-ddd-write-front
Artifacts for building http server with use of [Akka Http](http://doc.akka.io/docs/akka-stream-and-http-experimental/1.0-M2/scala/http/index.html) and [Akka Cluster Client](http://doc.akka.io/docs/akka/snapshot/contrib/cluster-client.html) responsible for handling commands sent as json messages. Provides infrastructure for demarshalling commands and forwarding them to write-backend application.

##### view-update (Eventstore integration)
Generic artifacts for building view update services that consume events from event store ([Event Store](http://geteventstore.com/) is used) and update a configured view store (i.e. Sql database). 

##### view-update-sql 
Sql (defult is Postgresql) specific implementation of view-update artifacts.

##### eventstore-akka-persistence
Customized EventStore serializer (extends serializer provided by [EventStore journal](https://github.com/pawelkaczor/EventStore.Akka.Persistence) aware of event definition from akka-ddd-messaging. 
