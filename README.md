akka-ddd [![Build Status](https://travis-ci.org/pawelkaczor/akka-ddd.svg?branch=master)](https://travis-ci.org/pawelkaczor/akka-ddd) [![Scala CI](https://github.com/pawelkaczor/akka-ddd/actions/workflows/scala.yml/badge.svg)](https://github.com/pawelkaczor/akka-ddd/actions/workflows/scala.yml) [![Version](https://img.shields.io/maven-central/v/pl.newicom.dddd/akka-ddd-core_2.12.svg?label=version)](http://search.maven.org/#search%7Cga%7C1%7Cg%3Apl.newicom.dddd)
========

[![Join the chat at https://gitter.im/pawelkaczor/akka-ddd](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/pawelkaczor/akka-ddd?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Akka-DDD is a framework for building distributed services following DDD/CQRS/ES architecture on top of the Akka platform. Thanks to the pluggable architecture of the Akka-Persistence, Akka-DDD is not tied to any particular event journal provider.

The services are built as actor systems. Different services can be distributed in the same cluster (Akka cluster) or can be deployed to independent clusters.

Akka-DDD offers concise APIs for implementing business logic of the following actor types:

- **Aggregate Root**
- **Process Manager**
- **Receptor**

All of these are **event-sourced** actors that support **reliable** (effectively-once delivery) communication. Process Managers and Receptors are operating within non-blocking **back-pressured** event processing pipeline.

Akka-DDD provides an extensible implementation of the View Update Service that is responsible for running running **Projections** on the read-side of the system. Implementation of SQL View Update Service is available out of the box.

Akka-DDD has been tested with the [EventStore](https://eventstore.org/) journal implementation. A [demo project](https://github.com/pawelkaczor/ddd-leaven-akka-v2) of an e-commerce system implemented using Akka-DDD is available on GitHub.

### Documentation

Project [homepage](http://newicom.pl/akka-ddd/).

[Documentation](http://newicom.pl/akka-ddd/docs/getting-started.html).

Articles in [Reactive DDD with Akka](http://pkaczor.blogspot.com/search/label/Reactive-DDD) series.

Demo [project](https://github.com/pawelkaczor/ddd-leaven-akka-v2).
