---
layout: docs
title: Aggregate Root - Testing
permalink: /docs/aggregate-root/testing
---
## Testing Aggregate Root

Examples:

when/expect

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

given/when/expect

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

when/expectException

```scala
"reject negative value" in {
  when {
    CreateDummy(dummyId, "dummy name", "dummy description", value = -1)
  }
  .expectException[DomainException]("negative value not allowed")
}
```

### Testing with Command Generators

Commands are generated (see generators above). The command under test (created inside When clause) is accessible from inside Then/expect clause. No need to define val members for name, description, etc. No need to define concrete values for name, description, etc.

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

Events triggered by Given command(s) are accessible from inside When clause. `Gen.suchThat` can be used to configure command generator inside test body.

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

Multiple commands in When section are supported. `expectEvents` should be used in `Then` section to assert multiple events were raised.

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

No problem with two or more commands inside Given clause.

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

Events triggered by Given command(s) are accessible from inside Then/expect clause.

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

Gen.map can be used to modify generated command.

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