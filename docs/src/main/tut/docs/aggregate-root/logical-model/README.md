---
layout: docs
title: Aggregate Root - Logical Model
permalink: /docs/aggregate-root/logical-model
---

## Aggregate Root - Logical Model

The logical model of the Aggregate Root is purely behavioral. 
The Aggregate Root exposes a set of **business operations** (commands) related to a **business entity/case**. 
When a **command** is received, it is **validated** against the **current state** of the AR and either it is **accepted** or **rejected**. If a command is accepted, an **event** (or a sequence of events) is stored in the AR journal and the state of the AR is updated. The state of the AR is its internal property, that can never be accessed directly from the outside of the AR. Thus, the structure of the AR (the structure of the AR state) is not a property of the AR logical model. 

The **logical model** of the AR can only be defined as a **set of behavior scenarios / specifications**, each expressed in the following form:

### Aggregate Root - Behavior Specification

<table style="margin-left:40px; font-size: 18px">
<tr>
<td style="border: none">
<h4 style="margin-top:10px">Given</h4>
&nbsp; &nbsp; &nbsp; &nbsp; a sequence of commands CG-1...CG-n accepted
<h4>When</h4>

&nbsp; &nbsp; &nbsp; &nbsp; a command CR received   

<h4>Then</h4>

&nbsp; &nbsp; &nbsp; &nbsp; an event E (or a sequence of events E-1...E-n) stored in the AR journal

</td></tr>

<tr><td style="border: none">&nbsp; &nbsp; &nbsp; &nbsp; <b>or</b></td></tr>
<tr><td style="border: none">&nbsp; &nbsp; &nbsp; &nbsp; the command CR rejected due to a reason R</td></tr>
<tr><td style="border: none">&nbsp;</td></tr>
</table>

Not surprisingly, the AR logical model defined as a set of AR behavior specifications is a perfectly valid, complete specification of the unit test that the AR implementation should be accompanied by. 

Following, is a single behavior specification for the [Reservation]() AR, implemented as a unit test using Given-When-Then test fixture provided by the Akka-DDD:

```scala
"Reservation office" should {
    "confirm reservation" in {
      given(
        CreateReservation(reservationId, "client1"),
        ReserveProduct(reservationId, product, quantity = 1)
      )
      .when(
        ConfirmReservation(reservationId)
      )
      .expectEvent {
        ReservationConfirmed(reservationId, "client1", product.price)
      }
    }
}
```

See: [Testing Aggregate Root](testing) 

### Aggregate Root ID

Aggregate Root ID is the identifier of the associated business entity/case. All commands are expected to inherit from the [Command]() trait to be able to expose the Aggregate Root ID (by implementing the [aggregateId() method).