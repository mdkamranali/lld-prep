import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Logger logger = Logger.getInstance();

        logger.info("This is info");
        logger.warn("This is warn");
        logger.error("This is error");
    }
}

enum LogLevel {
    INFO, WARN, ERROR
}

/** A place a log message can be written to, such as the console or a file. */
interface LogObserver {
    void log(String message);
}

class ConsoleLogger implements LogObserver {
    public void log(String message) {
        System.out.println("Logging into console: " + message);
    }
}

class FileLogger implements LogObserver {
    public void log(String message) {
        System.out.println("Logging into file: " + message);
    }
}

/** Keeps a list of observers for each level and sends messages to them. */
class LogPublisher {
    private final Map<LogLevel, List<LogObserver>> observers = new HashMap<>();

    public void addObserver(LogLevel level, LogObserver observer) {
        if (!observers.containsKey(level)) {
            observers.put(level, new ArrayList<>());
        }
        observers.get(level).add(observer);
    }

    public void notifyObservers(LogLevel level, String message) {
        List<LogObserver> observersForLevel = observers.get(level);
        if (observersForLevel == null) {
            return;
        }
        for (LogObserver observer : observersForLevel) {
            observer.log(message);
        }
    }
}

/** One link in the chain: handles its own level, otherwise passes the message along. */
class LoggerHandler {
    private final LogLevel logLevel;
    private final String prefix;
    private LoggerHandler next;

    LoggerHandler(LogLevel logLevel, String prefix) {
        this.logLevel = logLevel;
        this.prefix = prefix;
    }

    public void setNext(LoggerHandler next) {
        this.next = next;
    }

    public void log(LogLevel level, String message, LogPublisher publisher) {
        if (this.logLevel == level) {
            String msg = prefix + ": " + message;
            publisher.notifyObservers(level, msg);
        } else if (next != null) {
            next.log(level, message, publisher);
        }
    }
}

class LogManager {
    static LoggerHandler buildLoggerChain() {
        LoggerHandler info = new LoggerHandler(LogLevel.INFO, "Info");
        LoggerHandler warn = new LoggerHandler(LogLevel.WARN, "Warn");
        LoggerHandler error = new LoggerHandler(LogLevel.ERROR, "Error");

        info.setNext(warn);
        warn.setNext(error);

        return info;
    }

    static LogPublisher buildLogPublisher() {
        LogPublisher publisher = new LogPublisher();
        publisher.addObserver(LogLevel.INFO, new ConsoleLogger());
        publisher.addObserver(LogLevel.WARN, new ConsoleLogger());
        publisher.addObserver(LogLevel.ERROR, new ConsoleLogger());
        publisher.addObserver(LogLevel.ERROR, new FileLogger());

        return publisher;
    }
}

class Logger {
    private static final Logger INSTANCE = new Logger();

    private final LoggerHandler chain = LogManager.buildLoggerChain();
    private final LogPublisher publisher = LogManager.buildLogPublisher();

    private Logger() {}

    public static Logger getInstance() {
        return INSTANCE;
    }

    private void logMessage(LogLevel level, String message) {
        chain.log(level, message, publisher);
    }

    public void info(String message) {
        logMessage(LogLevel.INFO, message);
    }

    public void warn(String message) {
        logMessage(LogLevel.WARN, message);
    }

    public void error(String message) {
        logMessage(LogLevel.ERROR, message);
    }
}
