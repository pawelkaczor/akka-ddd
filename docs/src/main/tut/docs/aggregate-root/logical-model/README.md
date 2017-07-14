---
layout: docs
title: Aggregate Root - Logical Model
permalink: /docs/aggregate-root/logical-model
---

## Aggregate Root - Logical Model

The logical model of the Aggregate Root is purely behavioral. 
The Aggregate Root exposes a set of **business operations** (commands) related to a **business entity**. 
When a **command** is received, it is **validated** against the **current state** of the AR and either it is **accepted** or **rejected**. If a command is accepted, an **event** (or a sequence of events) is stored in the AR journal and the state of the AR is updated. The state of the AR is its internal property, that can never be accessed directly from the outside of the AR. Thus, the structure of the AR (the structure of the AR state) is not a property of the AR logical model. 

The **logical model** of the AR can only be defined as a **set of behavior scenarios**, each expressed in the following form:

### Aggregate Root - Behavior Scenario

#### Given 

&nbsp; &nbsp; &nbsp; &nbsp; a sequence of commands CG-1...CG-n accepted

#### When

&nbsp; &nbsp; &nbsp; &nbsp; a command CR received   

#### Then

&nbsp; &nbsp; &nbsp; &nbsp; an event E (or a sequence of events E-1...E-n) stored in the AR journal

&nbsp; &nbsp; &nbsp; &nbsp; **or** the command CR rejected due to a reason R

It turns out that the AR logical model defined as a set of AR behavior scenarios is a perfectly valid, complete specification of the Unit Test that the AR implementation should be accompanied by. 

See: [Testing Aggregate Root](testing) 