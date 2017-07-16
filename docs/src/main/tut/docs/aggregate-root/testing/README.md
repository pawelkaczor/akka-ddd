---
layout: docs
title: Aggregate Root - Testing
permalink: /docs/aggregate-root/testing
---
## Testing Aggregate Root's behavior

Akka-DDD provides Given-When-Then test fixture to be used for testing Aggregate Root's behavior. The [Office]() infrastructure, that is necessary to run the test, is created automatically before the test is executed. The [OfficeSpec]() class that the test class should extend from, expects Aggregate Root Actor Factory to be available in the implicit scope. Please see example below:

```scala
object ReservationSpec {
  implicit def factory: AggregateRootActorFactory[ReservationAggregateRoot] =
    new AggregateRootActorFactory[ReservationAggregateRoot] {
      def props(pc: PassivationConfig): Props = Props(new ReservationAggregateRoot(DefaultConfig(pc)))
    }
}

class ReservationSpec extends OfficeSpec[ReservationAggregateRoot] {

  def reservationOffice: Office = officeUnderTest
  def reservationId: EntityId = aggregateId

  "Reservation office" should {
    "create reservation" in {
      when(
        CreateReservation(reservationId, "client1")
      )
      .expectEvent(
        ReservationCreated(reservationId, "client1")
      )
    }
  }
}
```
By default, each behavior specification is executed with a different AggregateRoot Id, and so it is executed by a different instance of the Aggregate Root Actor.
You may choose to use the same Actor for running all behavior specifications by setting the `shareAggregateRoot` constructor parameter of the `OfficeSpec` to `true`. 

### Examples 

A sample specification of the initial behavior (no Given section):

```scala
"create Dummy" in {
  when {
    CreateDummy(dummyId, "dummy name", "dummy description", 100)
  }
  .expect { c =>
    DummyCreated(c.id, c.name, c.description, c.value)
  }
}
```

A sample specification of the behavior of an existing Aggregate Root.

```scala
"update Dummy's name" in {
  given {
    CreateDummy(dummyId, "dummy name", "dummy description", 100)
  }
  .when {
    ChangeName(dummyId, "some other dummy name")
  }
  .expect { c =>
    NameChanged(c.id, c.name)
  }
}
```

A sample specification of a behavior that is expected to reject the command.

```scala
"reject negative value" in {
  when {
    CreateDummy(dummyId, "dummy name", "dummy description", value = -1)
  }
  .expectException[DomainException]("negative value not allowed")
}
```

### Testing with Command Generators

You may choose to implement [ScalaCheck Generators]() for the commands. For example, given the following generator for the [CreateDummy]() command:

```scala
  implicit def create: Gen[CreateDummy] = for {
    name <- Gen.alphaStr
    description <- Gen.alphaStr
    value <- Gen.choose(1, 1000)
  } yield {
    CreateDummy(dummyId, name, description, value)
  }
```
you can write the behavior specification more concisely:

```scala
"create Dummy" in {
  when {
    a [CreateDummy]
  }
  .expect { c =>
    DummyCreated(c.id, c.name, c.description, c.value)
  }
}
```
Notice, that the command created inside the When clause (in the example above) is accessible from inside the Then/expect clause.
 
In the following sample behavior specification, a result event of the initial command, is accessed from inside the When clause. `Gen.suchThat` can be used to adjust a command generator inside the test body.

```scala
"update Dummy's name" in {
  given {
    a [CreateDummy]
  }
  .when { implicit hist =>
    a [ChangeName] suchThat (_.name != past[DummyCreated].name)
  }
  .expect { c =>
    NameChanged(c.id, c.name)
  }
}
```

It is possible to declare multiple commands in the When section. `expectEvents` should be used in the `Then` section to assert multiple events.

```scala
"update Dummy's value twice" in {
  given {
    a [CreateDummy]
  }
  .when (
    Seq(ChangeValue(dummyId, 1), ChangeValue(dummyId, 2))
  )
  .expectEvents (
    ValueChanged(dummyId, 1, 1), ValueChanged(dummyId, 2, 2)
  )
}
```

The following sample behavior specification show how to declare multiple generated commands in the Given section.

```scala
"handle subsequent Update command" in {
  given(
    a_list_of [CreateDummy, ChangeName]
  )
  .when {
    a [ChangeName]
  }
  .expect { c =>
    NameChanged(c.id, c.name)
  }
}
```

Another sample behavior specification showing that result events of the initial commands are accessible from inside the Then/expect clause.

```scala
"confirm generated value" in {
  given(
    a_list_of [CreateDummy, GenerateValue]
  )
  .when { implicit hist =>
    ConfirmGeneratedValue(dummyId, past[ValueGenerated].confirmationToken)
  }
  .expect { implicit c =>
    ValueChanged(c.id, past[ValueGenerated].value, dummyVersion = 1)
  }
}
```

The following sample behavior specification uses `Gen.map` to modify the generated command.

```scala
"reject negative value" in {
  when {
    a [CreateDummy] map (_ copy(value = -1))
    // alternatively:
    //arbitraryOf[CreateDummy](_ copy(value = -1))
  }
  .expectException[DomainException]("negative value not allowed")
}
```