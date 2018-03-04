---
layout: docs
title: Process Manager - Implementation
permalink: /docs/process-manager/impl
---

## Ordering Process - implementation  

```scala
  startWhen {

    case _: ReservationConfirmed => New

  } andThen {

    case New => {

      case ReservationConfirmed(reservationId, customerId, totalAmount) =>
        WaitingForPayment {
          ⟶(CreateInvoice(sagaId, reservationId, customerId, totalAmount, now()))

          ⟵(PaymentExpired(sagaId, reservationId.value)) in 3.minutes
        }

    }

    case WaitingForPayment => {

      case PaymentExpired(invoiceId, orderId) =>
        ⟶(CancelInvoice(invoiceId, orderId))

      case OrderBilled(_, orderId, _, _) =>
        DeliveryInProgress {
          ⟶(CloseReservation(orderId))

          ⟶(CreateShipment(shipmentIdGen(), orderId))
        }

      case OrderBillingFailed(_, orderId) =>
        Failed {
          ⟶(CancelReservation(orderId))
        }
    }
  }

```

Source: [OrderProcessManager.scala](https://github.com/pawelkaczor/ddd-leaven-akka-v2/blob/master/headquarters/write-back/src/main/scala/ecommerce/headquarters/processes/OrderProcessManager.scala)

An order is represented as a process that is triggered by the `ReservationConfirmed` event published by the `Reservation Office`. As soon as the order is created, the `CreateInvoice` command is issued to the `Invoicing Office` and the status of the order is changed to `WaitingForPayment`. If the payment succeeds (the `OrderBilled` event is received from the `Invoicing` office within 3 minutes) the `CreateShipment` command is issued to the `Shipping Office` and the status of the order is changed to `DeliveryInProgress`. But, if the scheduled timeout message `PaymentExpired` is received while the order is still not billed, the `CancelInvoice`commands is issued to the `Invoicing Office` and eventually the process ends with a `Failed` status. 

I hope you agree, that the logic of the Ordering Process is easy to grasp by looking at the code above. We simply declare a set of state transitions with associated triggering events and resulting commands. Please note that ```⟵ (PaymentExpired(...)) in 3.minutes``` gets resolved to the following command: (```⟶ ScheduleEvent(PaymentExpired(...), now + 3.minutes)```) that will be issued to the specialized `Scheduling Office`.