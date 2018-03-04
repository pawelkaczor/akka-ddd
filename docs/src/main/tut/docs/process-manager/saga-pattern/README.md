---
layout: docs
title: Process Manager - Saga Pattern
permalink: /docs/process-manager/saga-pattern
---

## The Saga pattern

As the business process participants are distributed and communicate asynchronously (just like the human actors in the real world!) the only way to deal with a failure is to incorporate it into the business process logic. If a failure happens (a command rejected by the office, a command not processed at all (office stopped), an event not received within the configured timeout), the counteraction, called compensation, must be executed. For example, the creation of an invoice is compensated by its cancellation (see the Ordering Process above). Following this rule, we break the long running conversation (the business process) into multiple smaller actions and counteractions that can be coordinated in the distributed environment without the global / distributed transaction. 

This pattern for reaching the distributed consensus without a distributed transaction is called the **Saga** pattern and was first introduced by the Hector Garcia-Molina in the 1987. 


### Event Orchestration vs. Event Choreography
A Saga pattern can be implemented with or without the central component (coordinator) (see: [Orchestration vs. Choreography](http://stackoverflow.com/a/29808740)). The [implementation of the Ordering Process]() follows the Orchestration pattern - the Ordering Process is managed by an actor, that is external to all process participants. 
