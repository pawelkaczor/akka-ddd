---
layout: docs
title: Business Processes - Introduction
permalink: /docs/process-manager/intro
---

## Business processes and SOA

To deliver a business value, an enterprise needs to perform its activities in a coordinated manner. Regardless of whether it is a production line or a decision chain, activities needs to be performed in a specific order accordingly to the rules defined by the business. The business process thus defines precisely what activities, in which order and under which conditions need to be performed, so that the desired business goal gets achieved. 

The coordination of the activities implies the information exchange between the collaborators. In the past, business processes were driven by the paper documents flying back and forth between process actors. Nowadays, when more and more activities are performed by computers and machines, the business process execution is realized as a message flow between services. Unfortunately though, for a variety of reasons, the implementation of the **Service-Oriented Architecture (SOA)**, very often ended up with a **Big Ball of Mud** as well as scalability and reliability issues (just to name a few) in the runtime. 

The recent move towards the **SOA 2.0** (aka Event-driven SOA / "SOA done Right" / Microservices) enables delivering more light-weight, reliable / fault-tolerant and scalable SOA implementations. When it comes to modeling and executing business processes, the key realization is that since **business processes are event-driven**, the same events that get written to the office journals could be used to trigger the start or the continuation of business processes. If so, any business process can be implemented as an **event-sourced actor** (called **Process Manager**) assuming it gets subscribed to a single stream of events (coming from an arbitrary number of offices) it is interested in. Once an event is received, the Process Manager executes an action, usually by sending a command to an office, and updates its state by writing the event to its journal. In this way, the Process Manager coordinates the work of the offices within the process. What is important is that **the logic of a business process is expressed in terms of incoming events, state transitions and outgoing commands**. This seems to be a quite powerful domain specific language for describing business processes. Let's take a look at the definition of an sample Ordering Process, that is written using the DSL offered by the Akka-DDD:
