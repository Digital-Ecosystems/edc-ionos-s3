# Events

EDC provides an eventing system that permits to developers to write extensions that could react to events that are 
emitted from the core of the EDC and also emit custom events.

## Subscribe to events
The entry point for event listening is the `EventRouter` component, on which an `EventSubscriber` can be registered.

Actually, there are two ways to register an `EventSubscriber`:
- **async**: every event will be sent to the subscriber in an asynchronous way. Features:
  - fast, as the main thread won't be blocked during dispatchment. 
  - not-reliable, as an eventual subscriber dispatch failure won't get handled.
  - to be used for notifications and for send-and-forget event dispatchment.
- **sync**: every event will be sent to the subscriber in a synchronous way. Features:
  - slow, as the subscriber will block the main thread until the event is dispatched
  - reliable, an eventual exception will be thrown to the caller, and it could make a transactional context fail
  - to be used for event persistence and to satisfy the "at-least-one" rule.

Extension example:
```java
public class ExampleEventSubscriptionExtension implements ServiceExtension {
    @Inject
    private EventRouter eventRouter;

    @Override
    public void initialize(ServiceExtensionContext context) {
        eventRouter.register(new ExampleEventSubscriber()); // asynchronous dispatch
        eventRouter.registerSync(new ExampleEventSubscriber()); // synchronous dispatch
    }
}
```

Then the `EventSubscriber` subscription will receive all the events emitted from the EDC and react to them:
```java
public class ExampleEventSubscriber implements EventSubscriber {
    
    public void on(Event event) {
        // react to event    
    }
    
}
```

To filter events, the payload classes of the events can be used. There are 5 "Intermediate superclasses" (AssetEventPayload, 
ContractDefinitionEventPayload, ContractNegotiationEventPayload, PolicyDefinitionEventPayload, TransferProcessEventPayload) of 
which one payload class inherits in each case. These intermediate superclasses inherit again from the class EventPayload. Thus, 
in addition to filtering on a specific event, such as TransferProcessCompleted, it is possible to react to a group of events 
that generally have to do with Assets, ContractDefinition, ContractNegotiation, PolicyDefinition and TransferProcess. You can 
also filter for the EventClass directly, like seen in the example for the event AssetCreated.

```java
public class ExampleEventSubscriber implements EventSubscriber {
    
    public void on(Event event) {
        if (event instanceof AssetCreated) {
            // react only to AssetCreated events
        }
        
        if (event.getPayload() instanceof TransferProcessEventPayload) {
            // react on Events related to TransferProcessEvents
        }
    }
    
}
```

## Emit custom events
It's also possible to create and publish custom events on top of the EDC eventing system.
To define the event, extend the `Event` class, if you need to attach data to an event you have to extend the `Event.Payload` class,
and pass the class as `Event` class parameter.
> Rule of thumb: events should be named at past tense, as they describe something that's already happened
```java
public class SomethingHappened extends Event {

    private SomethingHappened() {
    }

    public static class Builder extends Event.Builder<SomethingHappened, Payload, Builder> {

        public static Builder newInstance() {
            return new Builder();
        }

        private Builder() {
            super(new SomethingHappened(), new Payload());
        }

        public Builder description(String description) {
            event.payload.description = description;
            return this;
        }

        protected void validate() {
            Objects.requireNonNull(event.payload.description);
            // this validation helps to catch up missing properties in the test phase,
            // but isn't supposed to fail in a production environment, so it's not mandatory.
        }
    }
    
    public static class Payload extends EventPayload {
        private String description;

        public String getDescription() {
            return assetId;
        }
    }
}
```
All the data regarding an event should be contained in the `Payload` class.

As you may notice, we use the builder pattern to construct objects, as stated in
the [Architecture Principles document](architecture/coding-principles.md).
The extended builder will inherit all the builder method from the superclass.
The `validate` method is the place where validations on the payload can be added.

Once the event is created, it can be published it through the `EventRouter` component:
```java
public class ExampleBusinessLogic {
    public void doSomething() {
        // some business logic that does something
        var event = SomethingHappened.Builder.newInstance()
                .description("something interesting happened")
                .at(clock.millis())
                .build();
        
        eventRouter.publish(event);
    }    
}
```
Please note that the `at` field is a timestamp that every event has, and it's mandatory 
(please use the `Clock` service to get the current timestamp).

## Serialization / Deserialization

By default, events must be serializable, because of this, every class that extends `Event` will be serializable to json by default 
(through the `TypeManager` service). 
The json will contain an additional field called `type` that describes the name of the event class. For example, a serialized `SomethingHappened`
event will look like:
```json
{
  "type": "SomethingHappened",
  "at": 1654764642188,
  "payload": {
    "description": "something interesting happened"  
  }
}
```

To make such an event deserializable by the `TypeManager`, is necessary to register the type:
```java
typeManager.registerTypes(new NamedType(SomethingHappened.class, SomethingHappened.class.getSimpleName()));
```

doing so, the event can be deserialized using the `Event` superclass as type:
```
var deserialized = typeManager.readValue(json, Event.class);
// deserialized will have the `SomethingHappened` type at runtime
```
(please take a look at the [`EventTest`](../../spi/common/core-spi/src/test/java/org/eclipse/edc/spi/event/EventTest.java) class for a serialization/deserialization example)
