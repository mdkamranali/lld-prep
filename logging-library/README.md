# Logging Library — Low Level Design

Design a logging library (à la Log4j / SLF4J). A caller writes `logger.info("...")` and the message is tagged with its level, then delivered to every destination registered for that level — console, file, and so on.

## Patterns Used

| Pattern | Where |
| --- | --- |
| **Chain of Responsibility** | `LoggerHandler` links form a chain (INFO → WARN → ERROR). Each link either handles the level it owns or forwards the message to `next`, so the caller never needs to know which handler will pick it up. |
| **Observer** | `LogPublisher` keeps a list of `LogObserver`s per level and notifies all of them. Adding a new destination never touches the handlers. |
| **Singleton** | `Logger` is the single entry point (`Logger.getInstance()`), so the handler chain and the destination registry are built exactly once. |

## Domain Model

```
Logger  (singleton, the public API)
 ├── chain     : LoggerHandler          // first link
 │      ├── logLevel : LogLevel         // the level this link owns
 │      ├── prefix   : String           // "Info" / "Warn" / "Error"
 │      └── next     : LoggerHandler?   // null => end of chain
 └── publisher : LogPublisher
        └── observers : Map<LogLevel, List<LogObserver>>
                 └── ConsoleLogger / FileLogger
```

`LogManager` is the assembler: `buildLoggerChain()` wires the links together and `buildLogPublisher()` registers the destinations. Keeping construction out of `Logger` means the wiring can change without touching the logging path.

## End-to-End Flow

1. `Main.main()` grabs the singleton via `Logger.getInstance()`, which builds the chain and the publisher on first use.
2. `logger.info("This is info")` calls `logMessage(LogLevel.INFO, message)`, the one funnel every level method goes through.
3. `chain.log(level, message, publisher)` enters at the first link. Each `LoggerHandler` compares its own `logLevel` with the incoming level — on a match it formats `msg = prefix + ": " + message` and publishes; otherwise it hands the message to `next`.
4. `publisher.notifyObservers(level, msg)` looks up the destinations for that level and calls `log(msg)` on each. A level with no destinations is a no-op.
5. `ConsoleLogger` / `FileLogger` write the line out. `ERROR` is registered with both, so an error lands in two places from a single call.

## Compile & Run

```bash
cd src
javac Main.java && java Main
# or, on Java 11+:
java Main.java
```

## Sample Output

```
Logging into console: Info: This is info
Logging into console: Warn: This is warn
Logging into console: Error: This is error
Logging into file: Error: This is error
```

## Simplifications (intentional)

- Levels are matched exactly, not as thresholds. Real libraries treat a level as a floor (asking for `INFO` also emits `WARN` and `ERROR`); that would mean ordering the enum by severity and comparing with `>=` instead of `==`.
- `FileLogger` prints to stdout rather than doing real file I/O, which keeps the example free of streams and `IOException` handling. The seam is there — only `FileLogger#log` would change.
- `LogLevel` carries no severity value. An earlier version gave each level an `int`, but two levels sharing a number silently routed one level's messages to the other's handler; comparing enum constants makes that impossible.
- No timestamps, thread names, or formatting configuration, and the whole thing is single-threaded. `LogPublisher`'s map and lists would need concurrent variants for real use.
